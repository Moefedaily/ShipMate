package com.shipmate.unit.service.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shipmate.dto.request.pricing.PricingEstimateRequest;
import com.shipmate.dto.request.pricing.PricingRequest;
import com.shipmate.dto.response.pricing.PricingEstimateResponse;
import com.shipmate.service.pricing.PricingEstimateService;
import com.shipmate.service.pricing.PricingService;

@ExtendWith(MockitoExtension.class)
class PricingEstimateServiceTest {

    @Mock
    private PricingService pricingService;

    @InjectMocks
    private PricingEstimateService pricingEstimateService;

    @Test
    void estimate_shouldReturnDistanceAndEstimatedBasePrice() {
        PricingEstimateRequest request = new PricingEstimateRequest(
                new BigDecimal("48.8566"),
                new BigDecimal("2.3522"),
                new BigDecimal("45.7640"),
                new BigDecimal("4.8357"),
                new BigDecimal("8.25")
        );

        when(pricingService.computeBasePrice(any(PricingRequest.class))).thenReturn(new BigDecimal("64.50"));

        PricingEstimateResponse response = pricingEstimateService.estimate(request);

        assertThat(response.estimatedBasePrice()).isEqualByComparingTo("64.50");
        assertThat(response.distanceKm()).isGreaterThan(BigDecimal.ZERO);

        ArgumentCaptor<PricingRequest> captor = ArgumentCaptor.forClass(PricingRequest.class);
        verify(pricingService).computeBasePrice(captor.capture());
        assertThat(captor.getValue().weightKg()).isEqualByComparingTo("8.25");
        assertThat(captor.getValue().distanceKm()).isEqualByComparingTo(response.distanceKm());
    }
}
