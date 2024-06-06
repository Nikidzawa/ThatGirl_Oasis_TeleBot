package ru.nikidzawa.datingapp.api.internal.controllers.payments;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nikidzawa.datingapp.api.internal.controllers.events.EventsController;
import ru.nikidzawa.datingapp.api.internal.controllers.payments.helpers.ExternalHttpSender;
import ru.nikidzawa.datingapp.api.internal.controllers.payments.helpers.entities.Payment;
import ru.nikidzawa.datingapp.api.internal.controllers.payments.helpers.entities.PaymentResponse;
import ru.nikidzawa.datingapp.api.internal.exceptions.NotFoundException;
import ru.nikidzawa.datingapp.api.internal.exceptions.PaymentException;
import ru.nikidzawa.datingapp.configs.mail.MailSender;
import ru.nikidzawa.datingapp.configs.qrCode.QrCodeGenerator;
import ru.nikidzawa.datingapp.store.entities.event.EventEntity;
import ru.nikidzawa.datingapp.store.entities.event.Token;
import ru.nikidzawa.datingapp.store.entities.payment.PaymentEntity;
import ru.nikidzawa.datingapp.store.entities.payment.PaymentOrder;
import ru.nikidzawa.datingapp.store.repositories.EventRepository;
import ru.nikidzawa.datingapp.store.repositories.PaymentRepository;
import ru.nikidzawa.datingapp.store.repositories.TokenRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping("api/payment/")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Log4j2
public class PaymentController {

    EventRepository eventRepository;

    PaymentRepository paymentRepository;

    ExternalHttpSender externalHttpSender;

    MailSender mailSender;

    QrCodeGenerator qrCodeGenerator;

    TokenRepository tokenRepository;

    EventsController eventsController;

    @PostMapping("receivePay")
    public ResponseEntity<?> receivePay (@RequestBody PaymentResponse paymentResponse) {
        try {
            new Thread(() -> {
                String status = paymentResponse.getEvent();
                Payment payment = paymentResponse.getObject();
                switch (status) {
                    case "payment.waiting_for_capture" -> {
                        try {
                            PaymentEntity paymentEntity =
                                    paymentRepository.findById(Long.valueOf(payment.getMetadata().getLocalPaymentId()))
                                            .orElseThrow(() -> new PaymentException("Платёж не найден"));

                            List<EventEntity> eventEntities = paymentEntity.getEvents();
                            eventEntities.forEach(event -> {
                                String eventId = String.valueOf(event.getId());
                                String path = paymentEntity.getId() + "-" + eventId;
                                String mail = paymentEntity.getMail();

                                UUID uuid = UUID.randomUUID();
                                String token = uuid.toString();

                                Token tokenEntity = tokenRepository.saveAndFlush(
                                        Token.builder()
                                                .token(token)
                                                .build()
                                );
                                eventsController.addTokenInEvent(tokenEntity, event);

                                String qrCodePath = qrCodeGenerator.generate(path, eventId, token);
                                try {
                                    mailSender.sendMessage(mail, qrCodePath, event);
                                    log.info("QR код отправлен на почту");
                                } finally {
                                    Path pathToDelete = Paths.get(qrCodePath);
                                    try {
                                        Files.delete(pathToDelete);
                                        log.info("QR код удален: {}", qrCodePath);
                                    } catch (IOException e) {
                                        log.error("Ошибка удаления QR: {}\n{}", qrCodePath, e);
                                    }
                                }
                            });
                            HttpResponse httpResponse = externalHttpSender.successPay(payment.getId(), payment.getMetadata().getOperationId());
                            int code = httpResponse.getStatusLine().getStatusCode();
                            if (!(code >= 200 && code < 300)) {
                                throw new PaymentException("Ошибка при подтверждении платежа");
                            }
                        } catch (Exception ex) {
                            externalHttpSender.cancelPay(payment.getId(), payment.getMetadata().getOperationId());
                            throw new RuntimeException(ex);
                        }
                    }
                    case "payment.succeeded" -> log.info("Оплата подтверждена");
                    case "payment.canceled" -> log.info("Оплата отменена");
                }
            }).start();
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            paymentRepository.deleteById(Long.parseLong(paymentResponse.getObject().getMetadata().getLocalPaymentId()));
            throw new PaymentException(ex.getMessage());
        }
    }

    @PostMapping("startPay/{mail}")
    public ResponseEntity<?> startPay(@RequestBody List<PaymentOrder> paymentResponse, @PathVariable String mail) {
        long finalCost = paymentResponse.stream()
                .mapToLong(paymentOrder -> paymentOrder.getCount() * paymentOrder.getCost())
                .sum();

        List<EventEntity> finalEventList = paymentResponse.stream()
                .flatMap(paymentOrder -> {
                    EventEntity eventEntity = eventRepository.findById(paymentOrder.getId())
                            .orElseThrow(() -> new NotFoundException("Событие с id = " + paymentOrder.getId() + " не найдено. Оплата отменена"));
                    return Collections.nCopies(paymentOrder.getCount(), eventEntity).stream();
                }).collect(Collectors.toList());


        PaymentEntity paymentEntity = PaymentEntity.builder()
                .events(finalEventList)
                .cost(finalCost)
                .mail(mail)
                .build();

        PaymentEntity payment = paymentRepository.saveAndFlush(paymentEntity);
        String localPaymentId = payment.getId().toString();
        String operationId = localPaymentId + "@" + UUID.randomUUID();
        try {
            HttpResponse response = externalHttpSender.sendHttpPay(finalCost, localPaymentId, operationId);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 200 && statusCode < 300) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                return ResponseEntity.ok(result.toString());
            } else {
                log.error("Payment error: {}", statusCode);
                paymentRepository.delete(payment);
                return ResponseEntity.status(statusCode).build();
            }
        } catch (Exception ex) {
            paymentRepository.delete(payment);
            log.error("Payment error: {}", ex.getMessage());
            return ResponseEntity.status(500).body("Internal server error");
        }
    }
}
