package com.shipmate.controller.matching;


import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import com.shipmate.dto.response.matching.MatchResultResponse;
import com.shipmate.service.matching.ShipmentMatchingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/matching")
@PreAuthorize("@driverSecurity.isDriver(authentication)")
@RequiredArgsConstructor
public class ShipmentMatchingController {

    private final ShipmentMatchingService shipmentMatchingService;

    /**
     * Driver-facing endpoint to find compatible shipments near a driver.
     *
     * Notes:
     * - If lat/lng are not provided, service falls back to driver's last known location (DriverProfile).
     * - Only considers CREATED shipments and excludes already booked shipments (service layer rules).
     */
    
    @Operation(summary = "Find compatible shipments near a driver")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @Tag(name = "Matching")
    @GetMapping("/shipments")
    public List<MatchResultResponse> matchShipments(
            @AuthenticationPrincipal(expression = "username") String userId,

            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,

            @RequestParam(required = false) UUID bookingId,

            @RequestParam(defaultValue = "25") double radiusKm,
            @RequestParam(defaultValue = "50") int maxResults,

            Pageable pageable
    ) {
        return shipmentMatchingService.matchShipments(
                UUID.fromString(userId),
                lat,
                lng,
                bookingId,
                radiusKm,
                maxResults,
                pageable
        );
    }
}
