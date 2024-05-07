package ru.nikidzawa.datingapp.api.internal.controllers.payments.helpers;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Getter
@Component
public class ShopData {
    private final String encodingData;

    public ShopData () {
        String shopId = "377347";
        String apiKey = "test_SxUjBzknf1nAyjUVLL8nODeg6c0G7LhKVsCxnYfYCa8";
        encodingData = Base64.getEncoder().encodeToString((shopId + ":" + apiKey).getBytes());
    }
}
