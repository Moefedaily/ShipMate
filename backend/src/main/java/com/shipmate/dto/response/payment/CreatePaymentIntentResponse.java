package com.shipmate.dto.response.payment;

import com.shipmate.model.payment.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CreatePaymentIntentResponse {

    private String clientSecret;

    private PaymentStatus paymentStatus;

    private BigDecimal amountTotal;

    private String currency;
}
