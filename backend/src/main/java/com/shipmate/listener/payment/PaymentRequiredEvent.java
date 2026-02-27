package com.shipmate.listener.payment;

import java.util.UUID;

public record PaymentRequiredEvent(
        UUID shipmentId,
        UUID senderId
) {}
