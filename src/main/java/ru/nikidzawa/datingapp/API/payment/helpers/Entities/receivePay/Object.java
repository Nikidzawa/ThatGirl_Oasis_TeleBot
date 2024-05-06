package ru.nikidzawa.datingapp.API.payment.helpers.Entities.receivePay;

import lombok.Getter;
import ru.nikidzawa.datingapp.API.payment.helpers.Entities.receivePay.ObjectFields.Amount;
import ru.nikidzawa.datingapp.API.payment.helpers.Entities.receivePay.ObjectFields.Metadata;

@Getter
public class Object {
    String id;
    String status;
    boolean paid;
    Amount amount;
    Metadata metadata;
}
