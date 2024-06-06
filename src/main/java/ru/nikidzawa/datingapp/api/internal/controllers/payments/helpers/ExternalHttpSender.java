package ru.nikidzawa.datingapp.api.internal.controllers.payments.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.stereotype.Component;
import ru.nikidzawa.datingapp.api.internal.controllers.payments.helpers.entities.Metadata;
import ru.nikidzawa.datingapp.api.internal.controllers.payments.helpers.entities.SendPay;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ExternalHttpSender {

    ShopData shopData;

    @SneakyThrows
    public HttpResponse successPay (String paymentId, String operationId) {
        String url = "https://api.yookassa.ru/v3/payments/" + paymentId + "/" + "capture";
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost request = new HttpPost(url);
        request.addHeader("content-type", "application/json");
        request.addHeader("Idempotence-Key", operationId);
        request.addHeader("Authorization", "Basic " + shopData.getEncodingData());
        return httpClient.execute(request);
    }

    @SneakyThrows
    public void cancelPay (String paymentId, String operationId) {
        String url = "https://api.yookassa.ru/v3/payments/" + paymentId + "/" + "cancel";
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost request = new HttpPost(url);
        request.addHeader("content-type", "application/json");
        request.addHeader("Idempotence-Key", operationId);
        request.addHeader("Authorization", "Basic " + shopData.getEncodingData());
        httpClient.execute(request);
    }

    public HttpResponse sendHttpPay(long finalCost, String localPaymentId, String operationId) throws IOException {
        String apiUrl = "https://api.yookassa.ru/v3/payments";
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost request = new HttpPost(apiUrl);
        SendPay sendPay = new SendPay();
        sendPay.setAmount(
                SendPay.Amount.builder()
                        .value(String.valueOf(finalCost))
                        .currency("RUB")
                        .build());
        sendPay.setPayment_method_data(
                SendPay.PaymentMethodData.builder()
                        .type("bank_card")
                        .build());
        sendPay.setConfirmation(
                SendPay.Confirmation.builder()
                        .type("redirect")
                        .return_url("https://thatgirloasis.ru")
                        .build()
        );
        sendPay.setMetadata(Metadata.builder()
                .localPaymentId(localPaymentId)
                        .operationId(operationId)
                .build());
        sendPay.setDescription("Заказ №" + localPaymentId);

        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(sendPay);

        request.addHeader("content-type", "application/json");
        request.addHeader("Idempotence-Key", operationId);
        request.addHeader("Authorization", "Basic " + shopData.getEncodingData());

        StringEntity params = new StringEntity(json, ContentType.APPLICATION_JSON);
        request.setEntity(params);

        return httpClient.execute(request);
    }
}
