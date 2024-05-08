package ru.nikidzawa.datingapp.api.internal.controllers.payments.helpers.entities;

import lombok.Getter;

@Getter
public class PaymentResponse {
    private String type;
    private String event;
    private Payment object;
}
