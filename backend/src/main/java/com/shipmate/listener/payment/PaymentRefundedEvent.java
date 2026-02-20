package com.shipmate.listener.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRefundedEvent(
        UUID shipmentId,
        UUID senderId,
        BigDecimal amount
) {}
