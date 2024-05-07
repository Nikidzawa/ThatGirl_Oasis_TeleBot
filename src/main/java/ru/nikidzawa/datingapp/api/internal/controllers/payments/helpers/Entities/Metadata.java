package ru.nikidzawa.datingapp.api.internal.controllers.payments.helpers.Entities;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Metadata {
    private String localPaymentId;
}
