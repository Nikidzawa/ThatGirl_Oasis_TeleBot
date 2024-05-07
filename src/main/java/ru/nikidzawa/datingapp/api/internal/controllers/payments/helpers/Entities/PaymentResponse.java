package ru.nikidzawa.datingapp.api.internal.controllers.payments.helpers.Entities;

import lombok.Getter;

@Getter
public class PaymentResponse {
    private String type;
    private String event;
    private Payment object;
}
