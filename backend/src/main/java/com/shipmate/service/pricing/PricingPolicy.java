package com.shipmate.service.pricing;

import java.math.BigDecimal;

public final class PricingPolicy {

    private PricingPolicy() {}

    public static final BigDecimal BASE_FEE =
            BigDecimal.valueOf(5.00);

    public static final BigDecimal PRICE_PER_KM =
            BigDecimal.valueOf(0.80);

    public static final BigDecimal INCLUDED_WEIGHT_KG =
            BigDecimal.valueOf(5);

    public static final BigDecimal PRICE_PER_EXTRA_KG =
            BigDecimal.valueOf(0.50);

    public static final BigDecimal MIN_PRICE =
            BigDecimal.valueOf(10.00);

}
