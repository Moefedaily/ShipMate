package com.shipmate.dto.response.pricing;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ShipmentPricingPreviewResponse {

    private BigDecimal basePrice;
    private BigDecimal insuranceFee;
    private BigDecimal totalPrice;

    private BigDecimal insuranceRateApplied;
    private BigDecimal deductibleRate;
    private BigDecimal declaredValue;
}