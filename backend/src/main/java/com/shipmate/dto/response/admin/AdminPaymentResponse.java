package com.shipmate.dto.response.admin;

import com.shipmate.model.payment.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AdminPaymentResponse {

    private UUID id;

    private UUID shipmentId;

    private UUID senderId;

    private BigDecimal amountTotal;

    private String currency;

    private PaymentStatus paymentStatus;

    private String stripePaymentIntentId;

    private Instant createdAt;
}