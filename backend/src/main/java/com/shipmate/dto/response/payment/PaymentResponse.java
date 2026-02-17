package com.shipmate.dto.response.payment;

import com.shipmate.model.payment.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class PaymentResponse {

    private UUID shipmentId;

    private PaymentStatus paymentStatus;

    private BigDecimal amountTotal;

    private String currency;

    private String failureReason;
}
