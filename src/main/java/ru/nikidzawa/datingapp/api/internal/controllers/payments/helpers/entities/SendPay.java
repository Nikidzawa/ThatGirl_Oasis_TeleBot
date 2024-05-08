package ru.nikidzawa.datingapp.api.internal.controllers.payments.helpers.entities;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendPay {
    @Getter
    @Setter
    @Builder
    public static class Amount {
        private String value;
        private String currency;
    }
    @Getter
    @Setter
    @Builder
    public static class PaymentMethodData {
        private String type;
    }
    @Getter
    @Setter
    @Builder
    public static class Confirmation {
        private String type;
        private String return_url;
    }

    private Amount amount;
    private PaymentMethodData payment_method_data;
    private Confirmation confirmation;
    private String description;
    private Metadata metadata;
}
