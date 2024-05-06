package ru.nikidzawa.datingapp.API.payment.helpers.Entities.receivePay;

import lombok.Getter;

@Getter
public class ReceivePay {
    String type;
    String event;
    Object object;
}
