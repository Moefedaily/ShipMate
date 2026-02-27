package com.shipmate.dto.request.pricing;

import java.math.BigDecimal;


public record PricingRequest(

    BigDecimal distanceKm,

    BigDecimal weightKg
) {}
