package ru.nikidzawa.datingapp.api.internal.controllers.payments;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.http.HttpResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nikidzawa.datingapp.api.internal.controllers.payments.helpers.Entities.Payment;
import ru.nikidzawa.datingapp.api.internal.controllers.payments.helpers.PaymentHelper;
import ru.nikidzawa.datingapp.api.internal.exceptions.NotFoundException;
import ru.nikidzawa.datingapp.api.internal.exceptions.PaymentException;
import ru.nikidzawa.datingapp.store.entities.event.EventEntity;
import ru.nikidzawa.datingapp.store.entities.payment.PaymentEntity;
import ru.nikidzawa.datingapp.store.entities.payment.PaymentResponse;
import ru.nikidzawa.datingapp.store.entities.payment.PaymentStatus;
import ru.nikidzawa.datingapp.store.repositories.EventRepository;
import ru.nikidzawa.datingapp.store.repositories.PaymentRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;
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

    @PostMapping("receivePay")
    public ResponseEntity<?> receivePay (@RequestBody Payment payResult) {
        try {
            String status = payResult.getStatus();
            switch (status) {
                case "waiting_for_capture" -> {
                    System.out.println("Подтверждение платежа");
                    HttpResponse httpResponse = paymentHelper.successPay(payResult.getId(), payResult.getMetadata().getLocalPaymentId());
                    int code = httpResponse.getStatusLine().getStatusCode();
                    if (!(code >= 200 && code < 300)) {
                        throw new PaymentException("Ошибка при подвтерждении платежа");
                    }
                }
                case "payment.succeeded" -> {
                    System.out.println("Оплата прошла");
                }
                case "payment.canceled" -> {
                    System.out.println("Оплата отменена");
                }
            }
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            throw new PaymentException("Ошибка оплаты");
        }
    }

    @PostMapping("startPay")
    public ResponseEntity<?> startPay(@RequestBody List<PaymentResponse> paymentResponses) {
        long finalCost = paymentResponses.stream()
                .mapToLong(paymentResponse -> paymentResponse.getCount() * paymentResponse.getCost())
                .sum();

        List<EventEntity> eventEntities = paymentResponses.stream()
                .map(paymentResponse -> {
                    Long eventId = paymentResponse.getId();
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
