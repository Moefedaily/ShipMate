package com.shipmate.controller.shipment;

import com.shipmate.dto.request.shipment.AdminUpdateShipmentStatusRequest;
import com.shipmate.dto.response.shipment.ShipmentResponse;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.service.shipment.ShipmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/shipments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Shipment Management", description = "Admin APIs for managing all shipments")
public class AdminShipmentController {

    private final ShipmentService shipmentService;


    @Operation(
        summary = "Get shipment by ID",
        description = "Retrieves detailed information about a specific shipment by its ID. Admin only."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved shipment"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
        @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ShipmentResponse> getShipment(@PathVariable UUID id) {
        return ResponseEntity.ok(shipmentService.adminGetShipment(id));
    }

    @GetMapping
    @Operation(summary = "List shipments", description = "List shipments with optional status filter + pagination")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Shipments listed successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    public ResponseEntity<Page<ShipmentResponse>> getShipments(
            @RequestParam(required = false) ShipmentStatus status,
            Pageable pageable
    ) {
        return ResponseEntity.ok(shipmentService.adminListShipments(status, pageable));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update shipment status", description = "Admin updates a shipment status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Shipment status updated"),
        @ApiResponse(responseCode = "400", description = "Invalid status transition"),
        @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    public ResponseEntity<ShipmentResponse> updateStatus(
            @PathVariable UUID id,
            @RequestBody AdminUpdateShipmentStatusRequest request
    ) {
        return ResponseEntity.ok(
                shipmentService.adminUpdateStatus(id, request.getStatus(), request.getAdminNotes())
        );
    }

}
