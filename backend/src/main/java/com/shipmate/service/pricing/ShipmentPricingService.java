package com.shipmate.service.pricing;

import com.shipmate.dto.request.pricing.PricingRequest;
import com.shipmate.dto.request.pricing.ShipmentPricingPreviewRequest;
import com.shipmate.dto.response.pricing.ShipmentPricingPreviewResponse;
import com.shipmate.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ShipmentPricingService {

    private final PricingService pricingService;

    @Value("${app.insurance.max-declared-value:3000}")
    private BigDecimal maxDeclaredValue;

    @Value("${app.insurance.tier1.limit:1000}")
    private BigDecimal tier1Limit;

    @Value("${app.insurance.tier1.rate:0.02}")
    private BigDecimal tier1Rate;

    @Value("${app.insurance.tier2.limit:3000}")
    private BigDecimal tier2Limit;

    @Value("${app.insurance.tier2.rate:0.03}")
    private BigDecimal tier2Rate;

    @Value("${app.insurance.deductible-rate:0.10}")
    private BigDecimal deductibleRate;

    public ShipmentPricingPreviewResponse previewShipmentPricing(ShipmentPricingPreviewRequest request) {

        validateBasics(request);

        BigDecimal distanceKm = GeoUtils.haversineKm(
                request.getPickupLatitude(),
                request.getPickupLongitude(),
                request.getDeliveryLatitude(),
                request.getDeliveryLongitude()
        );

        BigDecimal basePrice = pricingService.computeBasePrice(
                new PricingRequest(distanceKm, request.getPackageWeight())
        ).setScale(2, RoundingMode.HALF_UP);

        BigDecimal insuranceFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal insuranceRateApplied = BigDecimal.ZERO;

        BigDecimal declaredValue = null;

        if (request.isInsuranceSelected()) {
            declaredValue = normalizeDeclaredValue(request);
            insuranceRateApplied = resolveTierRate(declaredValue);

            insuranceFee = declaredValue
                    .multiply(insuranceRateApplied)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal total = basePrice.add(insuranceFee).setScale(2, RoundingMode.HALF_UP);

        return ShipmentPricingPreviewResponse.builder()
                .basePrice(basePrice)
                .insuranceFee(insuranceFee)
                .totalPrice(total)
                .insuranceRateApplied(insuranceRateApplied)
                .deductibleRate(deductibleRate)
                .declaredValue(declaredValue)
                .build();
    }

    private void validateBasics(ShipmentPricingPreviewRequest request) {
        if (request.getPickupLatitude() == null || request.getPickupLongitude() == null ||
            request.getDeliveryLatitude() == null || request.getDeliveryLongitude() == null) {
            throw new IllegalArgumentException("Pickup and delivery coordinates are required");
        }

        if (request.getPackageWeight() == null || request.getPackageWeight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Package weight must be > 0");
        }

        if (request.getPackageValue() == null || request.getPackageValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Package value must be > 0");
        }
    }

    private BigDecimal normalizeDeclaredValue(ShipmentPricingPreviewRequest request) {

        if (request.getDeclaredValue() == null || request.getDeclaredValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Declared value is required when insurance is selected");
        }

        BigDecimal declaredValue = request.getDeclaredValue().setScale(2, RoundingMode.HALF_UP);

        if (declaredValue.compareTo(request.getPackageValue()) > 0) {
            throw new IllegalArgumentException("Declared value cannot exceed package value");
        }

        if (declaredValue.compareTo(maxDeclaredValue) > 0) {
            throw new IllegalArgumentException("Declared value exceeds maximum insurance limit");
        }

        return declaredValue;
    }

    private BigDecimal resolveTierRate(BigDecimal declaredValue) {
        if (declaredValue.compareTo(tier1Limit) <= 0) {
            return tier1Rate;
        }
        if (declaredValue.compareTo(tier2Limit) <= 0) {
            return tier2Rate;
        }
        return tier2Rate;
    }
}