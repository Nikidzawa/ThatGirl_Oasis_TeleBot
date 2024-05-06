package ru.nikidzawa.datingapp.API.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nikidzawa.datingapp.API.exceptions.NotFoundException;
import ru.nikidzawa.datingapp.API.exceptions.PaymentException;
import ru.nikidzawa.datingapp.API.payment.helpers.Entities.sendPay.SendPay;
import ru.nikidzawa.datingapp.API.payment.helpers.PaymentHelper;
import ru.nikidzawa.datingapp.API.payment.helpers.Entities.receivePay.ReceivePay;
import ru.nikidzawa.datingapp.store.entities.event.EventEntity;
import ru.nikidzawa.datingapp.store.entities.payment.PaymentEntity;
import ru.nikidzawa.datingapp.store.entities.payment.PaymentResponse;
import ru.nikidzawa.datingapp.store.entities.payment.PaymentStatus;
import ru.nikidzawa.datingapp.store.repositories.EventRepository;
import ru.nikidzawa.datingapp.store.repositories.PaymentRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Base64;
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
    public ResponseEntity<?> receivePay (@RequestBody ReceivePay payResult) {
        try {
            String status = payResult.getEvent();
            switch (status) {
                case "waiting_for_capture" -> {
                    HttpResponse httpResponse = paymentHelper.successPay();
                    int code = httpResponse.getStatusLine().getStatusCode();
                    if (!(code >= 200 && code < 300)) {
                        throw new PaymentException("Ошибка при подвтерждении платежа");
                    }
                }
                case "payment.succeeded" -> {
                }
                case "payment.canceled" -> {

                }
            }
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            throw new PaymentException("Ошибка оплаты");
        }
    }

    @PostMapping("startPay/{mail}")
    public ResponseEntity<?> startPay(@PathVariable String mail,
                                      @RequestBody List<PaymentResponse> paymentResponses) {
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
                .mail(mail)
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
                return ResponseEntity.status(statusCode).build();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body("Internal server error");
        }
    }
}
