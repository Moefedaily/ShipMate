package com.shipmate.service.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.shipmate.dto.request.pricing.PricingRequest;

@Service
public class PricingService {

    public BigDecimal computeBasePrice(PricingRequest req) {

        validate(req);

        BigDecimal distance = req.distanceKm().max(BigDecimal.ZERO);
        BigDecimal weight = req.weightKg().max(BigDecimal.ZERO);

        BigDecimal price =
            PricingPolicy.BASE_FEE
                .add(distance.multiply(PricingPolicy.PRICE_PER_KM));

        BigDecimal extraWeight = weight.subtract(PricingPolicy.INCLUDED_WEIGHT_KG);
        if (extraWeight.compareTo(BigDecimal.ZERO) > 0) {
            price = price.add(
                extraWeight.multiply(PricingPolicy.PRICE_PER_EXTRA_KG)
            );
        }

        price = price.max(PricingPolicy.MIN_PRICE);

        return price.setScale(2, RoundingMode.HALF_UP);
    }

    private void validate(PricingRequest req) {
        if (req.distanceKm() == null || req.weightKg() == null) {
            throw new IllegalArgumentException(
                "distanceKm and weightKg are required for pricing"
            );
        }
    }
}
