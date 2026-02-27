package com.shipmate.model.payment;

public enum PaymentStatus {
    REQUIRED,
    PROCESSING,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    CANCELLED,
    REFUNDED
}
