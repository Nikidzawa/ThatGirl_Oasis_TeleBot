package ru.nikidzawa.datingapp.api.internal.controllers.payments.helpers.entities;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Metadata {
    private String localPaymentId;
    private String operationId;
}
