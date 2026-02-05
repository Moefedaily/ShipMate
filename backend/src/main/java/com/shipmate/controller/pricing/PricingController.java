package com.shipmate.controller.pricing;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.shipmate.dto.request.pricing.PricingEstimateRequest;
import com.shipmate.dto.response.pricing.PricingEstimateResponse;
import com.shipmate.service.pricing.PricingEstimateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pricing")
@RequiredArgsConstructor
@Tag(name = "Pricing", description = "Pricing estimation APIs")
public class PricingController {

    private final PricingEstimateService pricingEstimateService;

    @Operation(
        summary = "Estimate shipment base price",
        description = "Returns a read-only price estimate computed server-side from distance and weight."
    )
    @PostMapping("/estimate")
    public ResponseEntity<PricingEstimateResponse> estimate(
        @Valid @RequestBody PricingEstimateRequest request
    ) {
        return ResponseEntity.ok(pricingEstimateService.estimate(request));
    }
}
