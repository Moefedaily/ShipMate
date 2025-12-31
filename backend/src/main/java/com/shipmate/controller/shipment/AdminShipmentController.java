package com.shipmate.controller.shipment;

import com.shipmate.dto.response.shipment.ShipmentResponse;
import com.shipmate.mapper.ShipmentMapper;
import com.shipmate.service.shipment.ShipmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

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
    private final ShipmentMapper shipmentMapper;

    // ===================== GET ALL SHIPMENTS =====================

    @Operation(
        summary = "Get all shipments",
        description = "Retrieves a list of all shipments in the system. Admin only."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved all shipments"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @GetMapping
    public ResponseEntity<List<ShipmentResponse>> getAllShipments() {
        return ResponseEntity.ok(
                shipmentService.getAllShipments()
                        .stream()
                        .map(shipmentMapper::toResponse)
                        .toList()
        );
    }

    // ===================== GET SHIPMENT BY ID =====================

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
        return ResponseEntity.ok(
                shipmentMapper.toResponse(
                        shipmentService.getShipmentById(id)
                )
        );
    }
}
