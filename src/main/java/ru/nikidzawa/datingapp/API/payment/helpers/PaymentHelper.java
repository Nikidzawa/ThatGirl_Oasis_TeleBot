package ru.nikidzawa.datingapp.API.payment.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.nikidzawa.datingapp.API.payment.helpers.Entities.sendPay.SendPay;

import java.io.IOException;
import java.util.Base64;

@Component
public class PaymentHelper {

    @Autowired
    ShopData shopData;

    @SneakyThrows
    public HttpResponse successPay (String paymentId, String localPaymentId) {
        String url = "https://api.yookassa.ru/v3/payments/" + paymentId + "/" + "capture";
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost request = new HttpPost(url);
        request.addHeader("content-type", "application/json");
        request.addHeader("Idempotence-Key", localPaymentId);
        request.addHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString((shopData.shopId + ":" + shopData.apiKey).getBytes()));
        HttpResponse response = httpClient.execute(request);
        return response;
    }

    public HttpResponse sendHttpPay(long finalCost, String localPaymentId) throws IOException {
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
                        .returnUrl("http://localhost:3000")
                        .build()
        );
        sendPay.setMetadata(SendPay.Metadata.builder()
                .localPaymentId(localPaymentId)
                .build());
        sendPay.setDescription("Заказ №" + localPaymentId);
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(sendPay);

        request.addHeader("content-type", "application/json");
        request.addHeader("Idempotence-Key", localPaymentId);
        request.addHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString((shopData.shopId + ":" + shopData.apiKey).getBytes()));

        StringEntity params = new StringEntity(json, ContentType.APPLICATION_JSON);
        request.setEntity(params);

        return httpClient.execute(request);
    }

}
