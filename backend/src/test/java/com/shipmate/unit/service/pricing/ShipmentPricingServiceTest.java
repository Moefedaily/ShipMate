package com.shipmate.unit.service.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.shipmate.dto.request.pricing.PricingRequest;
import com.shipmate.dto.request.pricing.ShipmentPricingPreviewRequest;
import com.shipmate.dto.response.pricing.ShipmentPricingPreviewResponse;
import com.shipmate.service.pricing.PricingService;
import com.shipmate.service.pricing.ShipmentPricingService;

@ExtendWith(MockitoExtension.class)
class ShipmentPricingServiceTest {

    @Mock
    private PricingService pricingService;

    @InjectMocks
    private ShipmentPricingService shipmentPricingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(shipmentPricingService, "maxDeclaredValue", BigDecimal.valueOf(3000));
        ReflectionTestUtils.setField(shipmentPricingService, "tier1Limit", BigDecimal.valueOf(1000));
        ReflectionTestUtils.setField(shipmentPricingService, "tier1Rate", new BigDecimal("0.02"));
        ReflectionTestUtils.setField(shipmentPricingService, "tier2Limit", BigDecimal.valueOf(3000));
        ReflectionTestUtils.setField(shipmentPricingService, "tier2Rate", new BigDecimal("0.03"));
        ReflectionTestUtils.setField(shipmentPricingService, "deductibleRate", new BigDecimal("0.10"));
    }

    @Test
    void previewShipmentPricing_shouldReturnBasePriceOnly_whenInsuranceNotSelected() {
        ShipmentPricingPreviewRequest request = validRequest();
        request.setInsuranceSelected(false);

        when(pricingService.computeBasePrice(any(PricingRequest.class))).thenReturn(new BigDecimal("125.456"));

        ShipmentPricingPreviewResponse response = shipmentPricingService.previewShipmentPricing(request);

        assertThat(response.getBasePrice()).isEqualByComparingTo("125.46");
        assertThat(response.getInsuranceFee()).isEqualByComparingTo("0.00");
        assertThat(response.getTotalPrice()).isEqualByComparingTo("125.46");
        assertThat(response.getInsuranceRateApplied()).isEqualByComparingTo("0");
        assertThat(response.getDeductibleRate()).isEqualByComparingTo("0.10");
        assertThat(response.getDeclaredValue()).isNull();
    }

    @Test
    void previewShipmentPricing_shouldApplyTierOneInsuranceRate() {
        ShipmentPricingPreviewRequest request = validRequest();
        request.setInsuranceSelected(true);
        request.setDeclaredValue(new BigDecimal("999.999"));

        when(pricingService.computeBasePrice(any(PricingRequest.class))).thenReturn(new BigDecimal("100.00"));

        ShipmentPricingPreviewResponse response = shipmentPricingService.previewShipmentPricing(request);

        assertThat(response.getDeclaredValue()).isEqualByComparingTo("1000.00");
        assertThat(response.getInsuranceRateApplied()).isEqualByComparingTo("0.02");
        assertThat(response.getInsuranceFee()).isEqualByComparingTo("20.00");
        assertThat(response.getTotalPrice()).isEqualByComparingTo("120.00");
    }

    @Test
    void previewShipmentPricing_shouldApplyTierTwoInsuranceRate_whenDeclaredValueExceedsTierOneLimit() {
        ShipmentPricingPreviewRequest request = validRequest();
        request.setInsuranceSelected(true);
        request.setDeclaredValue(new BigDecimal("1500.00"));

        when(pricingService.computeBasePrice(any(PricingRequest.class))).thenReturn(new BigDecimal("80.00"));

        ShipmentPricingPreviewResponse response = shipmentPricingService.previewShipmentPricing(request);

        assertThat(response.getInsuranceRateApplied()).isEqualByComparingTo("0.03");
        assertThat(response.getInsuranceFee()).isEqualByComparingTo("45.00");
        assertThat(response.getTotalPrice()).isEqualByComparingTo("125.00");
    }

    @Test
    void previewShipmentPricing_shouldPassCalculatedDistanceAndWeightToPricingService() {
        ShipmentPricingPreviewRequest request = validRequest();

        when(pricingService.computeBasePrice(any(PricingRequest.class))).thenReturn(new BigDecimal("25.00"));

        shipmentPricingService.previewShipmentPricing(request);

        ArgumentCaptor<PricingRequest> captor = ArgumentCaptor.forClass(PricingRequest.class);
        verify(pricingService).computeBasePrice(captor.capture());

        PricingRequest pricingRequest = captor.getValue();
        assertThat(pricingRequest.weightKg()).isEqualByComparingTo("12.50");
        assertThat(pricingRequest.distanceKm()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void previewShipmentPricing_shouldRejectMissingCoordinates() {
        ShipmentPricingPreviewRequest request = validRequest();
        request.setPickupLatitude(null);

        assertThatThrownBy(() -> shipmentPricingService.previewShipmentPricing(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Pickup and delivery coordinates are required");
    }

    @Test
    void previewShipmentPricing_shouldRejectNonPositiveWeight() {
        ShipmentPricingPreviewRequest request = validRequest();
        request.setPackageWeight(BigDecimal.ZERO);

        assertThatThrownBy(() -> shipmentPricingService.previewShipmentPricing(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Package weight must be > 0");
    }

    @Test
    void previewShipmentPricing_shouldRejectNonPositivePackageValue() {
        ShipmentPricingPreviewRequest request = validRequest();
        request.setPackageValue(BigDecimal.ZERO);

        assertThatThrownBy(() -> shipmentPricingService.previewShipmentPricing(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Package value must be > 0");
    }

    @Test
    void previewShipmentPricing_shouldRejectMissingDeclaredValue_whenInsuranceSelected() {
        ShipmentPricingPreviewRequest request = validRequest();
        request.setInsuranceSelected(true);
        request.setDeclaredValue(null);
        when(pricingService.computeBasePrice(any(PricingRequest.class))).thenReturn(new BigDecimal("10.00"));

        assertThatThrownBy(() -> shipmentPricingService.previewShipmentPricing(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Declared value is required when insurance is selected");
    }

    @Test
    void previewShipmentPricing_shouldRejectDeclaredValueAbovePackageValue() {
        ShipmentPricingPreviewRequest request = validRequest();
        request.setInsuranceSelected(true);
        request.setDeclaredValue(new BigDecimal("600.00"));
        request.setPackageValue(new BigDecimal("500.00"));
        when(pricingService.computeBasePrice(any(PricingRequest.class))).thenReturn(new BigDecimal("10.00"));

        assertThatThrownBy(() -> shipmentPricingService.previewShipmentPricing(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Declared value cannot exceed package value");
    }

    @Test
    void previewShipmentPricing_shouldRejectDeclaredValueAboveMaxInsuranceLimit() {
        ShipmentPricingPreviewRequest request = validRequest();
        request.setInsuranceSelected(true);
        request.setDeclaredValue(new BigDecimal("3000.01"));
        request.setPackageValue(new BigDecimal("3500.00"));
        when(pricingService.computeBasePrice(any(PricingRequest.class))).thenReturn(new BigDecimal("10.00"));

        assertThatThrownBy(() -> shipmentPricingService.previewShipmentPricing(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Declared value exceeds maximum insurance limit");
    }

    private ShipmentPricingPreviewRequest validRequest() {
        ShipmentPricingPreviewRequest request = new ShipmentPricingPreviewRequest();
        request.setPickupLatitude(new BigDecimal("48.8566"));
        request.setPickupLongitude(new BigDecimal("2.3522"));
        request.setDeliveryLatitude(new BigDecimal("45.7640"));
        request.setDeliveryLongitude(new BigDecimal("4.8357"));
        request.setPackageWeight(new BigDecimal("12.50"));
        request.setPackageValue(new BigDecimal("5000.00"));
        return request;
    }
}
