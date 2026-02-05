package com.shipmate.dto.response.pricing;

import java.math.BigDecimal;

import lombok.Builder;

@Builder
public record PricingEstimateResponse(
    BigDecimal distanceKm,
    BigDecimal estimatedBasePrice
) {}
