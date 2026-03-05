package com.shipmate.controller.admin;

import com.shipmate.dto.response.shipment.ShipmentResponse;
import com.shipmate.dto.response.insurance.InsuranceClaimResponse;
import com.shipmate.dto.response.payment.PaymentResponse;

import com.shipmate.service.admin.AdminUserInspectionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin User Inspection", description = "Inspect user activity")
public class AdminUserInspectionController {

    private final AdminUserInspectionService inspectionService;

    @GetMapping("/{userId}/shipments")
    @Operation(summary = "Get user shipments", description = "Get a paginated list of shipments associated with a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User shipments retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Page<ShipmentResponse>> getUserShipments(
            @PathVariable UUID userId,
            Pageable pageable
    ) {

        Page<ShipmentResponse> response =
                inspectionService.getUserShipments(userId, pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/claims")
    @Operation(summary = "Get user claims", description = "Get a paginated list of claims associated with a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User claims retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Page<InsuranceClaimResponse>> getUserClaims(
            @PathVariable UUID userId,
            Pageable pageable
    ) {

        Page<InsuranceClaimResponse> response =
                inspectionService.getUserClaims(userId, pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/payments")
    @Operation(summary = "Get user payments", description = "Get a paginated list of payments associated with a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User payments retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Page<PaymentResponse>> getUserPayments(
            @PathVariable UUID userId,
            Pageable pageable
    ) {

        Page<PaymentResponse> response =
                inspectionService.getUserPayments(userId, pageable);

        return ResponseEntity.ok(response);
    }
}