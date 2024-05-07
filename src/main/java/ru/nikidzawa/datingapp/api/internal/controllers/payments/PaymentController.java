package ru.nikidzawa.datingapp.api.internal.controllers.payments;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.http.HttpResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nikidzawa.datingapp.api.internal.controllers.payments.helpers.Entities.Payment;
import ru.nikidzawa.datingapp.api.internal.controllers.payments.helpers.Entities.PaymentResponse;
import ru.nikidzawa.datingapp.api.internal.controllers.payments.helpers.PaymentHelper;
import ru.nikidzawa.datingapp.api.internal.exceptions.NotFoundException;
import ru.nikidzawa.datingapp.api.internal.exceptions.PaymentException;
import ru.nikidzawa.datingapp.store.entities.event.EventEntity;
import ru.nikidzawa.datingapp.store.entities.payment.PaymentEntity;
import ru.nikidzawa.datingapp.store.entities.payment.PaymentOrder;
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
                        } else {
                            System.out.println("Подтверждение платежа прошло успешно");
                        }
                    }
                    case "payment.succeeded" -> System.out.println("Оплата прошла");
                    case "payment.canceled" -> System.out.println("Оплата отменена");
                }
            }).start();
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            throw new PaymentException("Ошибка оплаты");
        }
    }

    @PostMapping("startPay")
    public ResponseEntity<?> startPay(@RequestBody List<PaymentOrder> paymentResponse) {
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
