package ru.nikidzawa.datingapp.api.internal.controllers.payments;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.http.HttpResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nikidzawa.datingapp.api.internal.controllers.payments.helpers.PaymentHelper;
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
import ru.nikidzawa.datingapp.store.entities.payment.PaymentStatus;
import ru.nikidzawa.datingapp.store.repositories.EventRepository;
import ru.nikidzawa.datingapp.store.repositories.PaymentRepository;
import ru.nikidzawa.datingapp.store.repositories.TokenRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping("api/payment/")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentController {

    EventRepository eventRepository;

    PaymentRepository paymentRepository;

    PaymentHelper paymentHelper;

    MailSender mailSender;

    QrCodeGenerator qrCodeGenerator;

    TokenRepository tokenRepository;

    @PostMapping("receivePay")
    public ResponseEntity<?> receivePay (@RequestBody PaymentResponse paymentResponse) {
        try {
            new Thread(() -> {
                String status = paymentResponse.getEvent();
                Payment payment = paymentResponse.getObject();
                switch (status) {
                    case "payment.waiting_for_capture" -> {
                        HttpResponse httpResponse = paymentHelper.successPay(payment.getId(), payment.getMetadata().getLocalPaymentId());
                        int code = httpResponse.getStatusLine().getStatusCode();
                        if (!(code >= 200 && code < 300)) {
                            throw new PaymentException("Ошибка при подвтерждении платежа");
                        }
                    }
                    case "payment.succeeded" -> {
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

                            new Thread(() -> {
                                Token tokenEntity = tokenRepository.saveAndFlush(
                                        Token.builder()
                                                .token(token)
                                                .build()
                                );
                                event.getTokens().add(tokenEntity);
                                eventRepository.saveAndFlush(event);
                            }).start();

                            String qrCodePath = qrCodeGenerator.generate(path, eventId, token);
                            mailSender.sendMessage(mail, qrCodePath);
                        });
                    }
                    case "payment.canceled" -> throw new PaymentException("Отказ от оплаты");
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

        List<EventEntity> eventEntities = paymentResponse.stream()
                .map(paymentOrder -> {
                    Long eventId = paymentOrder.getId();
                    Optional<EventEntity> eventEntity = eventRepository.findById(eventId);
                    if (eventEntity.isPresent()) {
                        return eventEntity.get();
                    } else {
                        throw new NotFoundException("Событие с id = " + eventId + " не найдено. Оплата отменена");
                    }
                })
                .collect(Collectors.toList());

        PaymentEntity paymentEntity = PaymentEntity.builder()
                .events(eventEntities)
                .cost(finalCost)
                .paymentStatus(PaymentStatus.WAIT_FOR_PAY)
                .mail(mail)
                .build();

        PaymentEntity payment = paymentRepository.saveAndFlush(paymentEntity);
        String localPaymentId = payment.getId().toString();

        try {
            HttpResponse response = paymentHelper.sendHttpPay(finalCost, localPaymentId);
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
                paymentRepository.delete(payment);
                return ResponseEntity.status(statusCode).build();
            }
        } catch (Exception ex) {
            paymentRepository.delete(payment);
            return ResponseEntity.status(500).body("Internal server error");
        }
    }
}
