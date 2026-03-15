package com.shipmate.controller.vehicle;

import com.shipmate.dto.request.vehicle.CreateVehicleRequest;
import com.shipmate.dto.response.vehicle.VehicleResponse;
import com.shipmate.service.vehicle.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Vehicle", description = "Vehicle management APIs")
public class VehicleController {

    private final VehicleService vehicleService;

    // --- DRIVER ENDPOINTS ---

    @Operation(summary = "Add a new vehicle")
    @PostMapping("/api/vehicles")
    @PreAuthorize("@driverSecurity.hasDriverProfile(authentication)")
    public ResponseEntity<VehicleResponse> addVehicle(
            @AuthenticationPrincipal(expression = "username") String userId,
            @Valid @RequestBody CreateVehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vehicleService.addVehicle(UUID.fromString(userId), request));
    }

    @Operation(summary = "Get my vehicles")
    @GetMapping("/api/vehicles/mine")
    @PreAuthorize("@driverSecurity.hasDriverProfile(authentication)")
    public ResponseEntity<List<VehicleResponse>> getMyVehicles(
            @AuthenticationPrincipal(expression = "username") String userId) {
        return ResponseEntity.ok(vehicleService.getMyVehicles(UUID.fromString(userId)));
    }

    @Operation(summary = "Activate a specific vehicle")
    @PutMapping("/api/vehicles/{id}/activate")
    @PreAuthorize("@driverSecurity.isApprovedDriver(authentication)")
    public ResponseEntity<VehicleResponse> activateVehicle(
            @AuthenticationPrincipal(expression = "username") String userId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(vehicleService.activateVehicle(UUID.fromString(userId), id));
    }
}
