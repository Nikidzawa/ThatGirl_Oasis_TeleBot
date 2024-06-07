package ru.nikidzawa.datingapp.api.internal.controllers.payments.helpers;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Getter
@Component
public class ShopData {
    private final String encodingData;

    public ShopData () {
        String shopId = "364431";
        String apiKey = "live_YsC1w_ZPG0cA986zbIhXT-koERIK71-zz1FdH9rFs_M";
        encodingData = Base64.getEncoder().encodeToString((shopId + ":" + apiKey).getBytes());
    }
}
