package com.shipmate.service.pricing;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.shipmate.dto.request.pricing.PricingEstimateRequest;
import com.shipmate.dto.request.pricing.PricingRequest;
import com.shipmate.dto.response.pricing.PricingEstimateResponse;
import com.shipmate.util.GeoUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PricingEstimateService {

    private final PricingService pricingService;

    public PricingEstimateResponse estimate(PricingEstimateRequest req) {

        BigDecimal distanceKm = GeoUtils.haversineKm(
            req.pickupLatitude(),
            req.pickupLongitude(),
            req.deliveryLatitude(),
            req.deliveryLongitude()
        );

        BigDecimal price = pricingService.computeBasePrice(
            new PricingRequest(distanceKm, req.packageWeight())
        );

        return PricingEstimateResponse.builder()
            .distanceKm(distanceKm)
            .estimatedBasePrice(price)
            .build();
    }
}
