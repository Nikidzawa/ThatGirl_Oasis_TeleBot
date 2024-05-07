package ru.nikidzawa.datingapp.api.internal.controllers.payments.helpers.Entities;

import lombok.Getter;

@Getter
public class Payment {
    private String id;
    private String status;
    private boolean paid;
    private Amount amount;
    private AuthDetails authDetails;
    private String created_at;
    private String description;
    private String expires_at;
    private Metadata metadata;
    private PaymentMethod payment_method;
    private Recipient recipient;
    private boolean refundable;
    private boolean test;
    private IncomeAmount income_amount;

    @Getter
    public static class Amount {
        private String value;
        private String currency;
    }

    @Getter
    public static class AuthDetails {
        String rrn;
        String auth_code;
        ThreeDSecure three_d_secure;

        @Getter
        public static class ThreeDSecure {
            boolean applied;
        }
    }

    @Getter
    public static class PaymentMethod {
        private String type;
        private String id;
        private boolean saved;
        private Card card;
        private String title;

        @Getter
        public static class Card {
            private String first6;
            private String last4;
            private String expiry_month;
            private String expiry_year;
            private String card_type;
            private CardProduct card_product;
            private String issuer_country;
            private String issuer_name;

            @Getter
            public static class CardProduct {
                private String code;
                private String name;
            }
        }
    }

    @Getter
    public static class Recipient {
        private String account_id;
        private String gateway_id;
    }

    @Getter
    public static class IncomeAmount {
        private String value;
        private String currency;
    }
}
