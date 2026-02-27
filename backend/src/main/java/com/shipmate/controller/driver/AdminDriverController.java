package com.shipmate.controller.driver;

import java.util.List;
import java.util.UUID;

import com.shipmate.dto.response.driver.DriverProfileResponse;
import com.shipmate.service.driver.DriverProfileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/drivers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Driver Management", description = "Admin APIs for managing driver applications and status")
public class AdminDriverController {

    private final DriverProfileService driverProfileService;


    @Operation(
        summary = "Get pending driver applications",
        description = "Retrieves all driver applications with pending status awaiting admin review."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @GetMapping("/pending")
    public ResponseEntity<List<DriverProfileResponse>> getPendingDrivers() {
        return ResponseEntity.ok(driverProfileService.getPendingDrivers());
    }


    @Operation(
        summary = "Approve driver application",
        description = "Approves a driver application, allowing the driver to start accepting shipments."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Driver approved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
        @ApiResponse(responseCode = "404", description = "Driver profile not found")
    })
    @PostMapping("/{id}/approve")
    public ResponseEntity<DriverProfileResponse> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(driverProfileService.approve(id));
    }


    @Operation(
        summary = "Reject driver application",
        description = "Rejects a driver application, preventing the applicant from becoming a driver."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Driver rejected successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
        @ApiResponse(responseCode = "404", description = "Driver profile not found")
    })
    @PostMapping("/{id}/reject")
    public ResponseEntity<DriverProfileResponse> reject(@PathVariable UUID id) {
        return ResponseEntity.ok(driverProfileService.reject(id));
    }


    @Operation(
        summary = "Suspend driver account",
        description = "Suspends an active driver account, preventing them from accepting new shipments."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Driver suspended successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
        @ApiResponse(responseCode = "404", description = "Driver profile not found")
    })
    @PostMapping("/{id}/suspend")
    public ResponseEntity<DriverProfileResponse> suspend(@PathVariable UUID id) {
        return ResponseEntity.ok(driverProfileService.suspend(id));
    }

    @Operation(
        summary = "Get drivers with strikes",
        description = "Retrieve all drivers with strike count information."
    )
    @GetMapping("/strikes")
    public ResponseEntity<List<DriverProfileResponse>> getDriversWithStrikes() {
        return ResponseEntity.ok(driverProfileService.getDriversWithStrikes());
    }

    @Operation(
        summary = "Reset driver strikes",
        description = "Resets strike count and restores driver if suspended."
    )
    @PostMapping("/{id}/reset-strikes")
    public ResponseEntity<DriverProfileResponse> resetStrikes(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(driverProfileService.resetStrikes(id));
    }
    
}