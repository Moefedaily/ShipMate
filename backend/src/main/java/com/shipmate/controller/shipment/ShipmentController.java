package com.shipmate.controller.shipment;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.shipmate.dto.request.confirmDelivery.ConfirmDeliveryRequest;
import com.shipmate.dto.request.shipment.CreateShipmentRequest;
import com.shipmate.dto.request.shipment.UpdateShipmentRequest;
import com.shipmate.dto.response.delivery.DeliveryCodeStatusResponse;
import com.shipmate.dto.response.shipment.ShipmentResponse;
import com.shipmate.service.delivery.DeliveryCodeService;
import com.shipmate.service.shipment.ShipmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
@Tag(name = "Shipments", description = "Shipment management APIs")
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final DeliveryCodeService deliveryCodeService;

    // ===================== CREATE SHIPMENT =====================
    @Operation(
        summary = "Create a shipment",
        description = "Creates a new shipment for the authenticated user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Shipment created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<ShipmentResponse> createShipment(
            @AuthenticationPrincipal(expression = "username") String userId,
            @Valid @RequestBody CreateShipmentRequest request) {

        return ResponseEntity.ok(
                shipmentService.create(UUID.fromString(userId), request)
        );
    }

    // ===================== GET MY SHIPMENTS =====================
    @Operation(
        summary = "Get my shipments",
        description = "Retrieves all shipments created by the authenticated user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Shipments retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/me")
    public ResponseEntity<Page<ShipmentResponse>> getMyShipments(
            @AuthenticationPrincipal(expression = "username") String userId,
            Pageable pageable) {

        return ResponseEntity.ok(
                shipmentService.getMyShipments(UUID.fromString(userId), pageable)
        );
    }

    // ===================== GET MY SHIPMENT BY ID =====================
    @Operation(
        summary = "Get my shipment by ID",
        description = "Retrieves a specific shipment owned by the authenticated user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Shipment retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Shipment not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{shipmentId}")
    public ResponseEntity<ShipmentResponse> getMyShipment(
            @PathVariable UUID shipmentId,
            @AuthenticationPrincipal(expression = "username") String userId) {

        return ResponseEntity.ok(
                shipmentService.getMyShipment(shipmentId, UUID.fromString(userId))
        );
    }

    // ===================== UPDATE SHIPMENT =====================
    @Operation(
        summary = "Update my shipment",
        description = "Updates a shipment owned by the authenticated user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Shipment updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input or shipment state"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/{shipmentId}")
    public ResponseEntity<ShipmentResponse> updateShipment(
            @PathVariable UUID shipmentId,
            @AuthenticationPrincipal(expression = "username") String userId,
            @Valid @RequestBody UpdateShipmentRequest request) {

        return ResponseEntity.ok(
                shipmentService.update(shipmentId, UUID.fromString(userId), request)
        );
    }

    // ===================== DELETE SHIPMENT =====================
    @Operation(
        summary = "Delete my shipment",
        description = "Deletes a shipment owned by the authenticated user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Shipment deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid shipment state"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/{shipmentId}")
    public ResponseEntity<Void> deleteShipment(
            @PathVariable UUID shipmentId,
            @AuthenticationPrincipal(expression = "username") String userId) {

        shipmentService.delete(shipmentId, UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

    // ===================== ADD SHIPMENT PHOTOS =====================
    @Operation(
        summary = "Add shipment photos",
        description = "Uploads one or more photos for a shipment owned by the authenticated user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Photos uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid shipment state or input"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Not shipment owner")
    })
    @PostMapping(
        value = "/{shipmentId}/photos",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ShipmentResponse> addShipmentPhotos(
            @PathVariable UUID shipmentId,
            @AuthenticationPrincipal(expression = "username") String userId,
            @RequestPart("files") List<MultipartFile> files) {

        return ResponseEntity.ok(
            shipmentService.addPhotos(
                shipmentId,
                UUID.fromString(userId),
                files
            )
        );
    }

    // ===================== MARK SHIPMENT IN TRANSIT =====================
    @Operation(
        summary = "Mark shipment as in transit",
        description = "Driver marks a shipment as in transit."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Shipment marked as in transit"),
        @ApiResponse(responseCode = "400", description = "Invalid shipment state"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Not assigned driver")
    })
    @PostMapping("/{shipmentId}/in-transit")
    public ResponseEntity<ShipmentResponse> markInTransit(
            @PathVariable UUID shipmentId,
            @AuthenticationPrincipal(expression = "username") String userId) {

        return ResponseEntity.ok(
                shipmentService.markInTransit(
                        shipmentId,
                        UUID.fromString(userId)
                )
        );
    }

    // ===================== CONFIRM DELIVERY =====================
    @Operation(
        summary = "Confirm delivery",
        description = "Driver confirms delivery of a shipment."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Delivery confirmed"),
        @ApiResponse(responseCode = "400", description = "Invalid shipment state"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Not assigned driver")
    })
    @PostMapping("/{shipmentId}/confirm-delivery")
    public ResponseEntity<ShipmentResponse> confirmDelivery(
            @PathVariable UUID shipmentId,
            @AuthenticationPrincipal(expression = "username") String userId,
            @RequestBody ConfirmDeliveryRequest request) {

        return ResponseEntity.ok(
                shipmentService.confirmDelivery(
                        shipmentId,
                        UUID.fromString(userId),
                        request.getCode()
                )
        );
    }

    // ===================== RESET DELIVERY CODE =====================
    @Operation(
        summary = "Reset delivery code",
        description = "Resets the delivery code for a shipment."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Delivery code reset successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid shipment state"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Not allowed")
    })
    @PostMapping("/{shipmentId}/delivery-code/reset")
    public ResponseEntity<Void> resetDeliveryCode(
            @PathVariable UUID shipmentId,
            @AuthenticationPrincipal(expression = "username") String userId) {

        shipmentService.resetDeliveryCode(
                shipmentId,
                UUID.fromString(userId)
        );

        return ResponseEntity.noContent().build();
    }

    // ===================== CANCEL SHIPMENT =====================
    @Operation(
        summary = "Cancel shipment",
        description = "Cancels a shipment (driver or sender)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Shipment cancelled successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid shipment state"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Not allowed")
    })
    @PostMapping("/{shipmentId}/cancel")
    public ResponseEntity<ShipmentResponse> cancelShipment(
            @PathVariable UUID shipmentId,
            @AuthenticationPrincipal(expression = "username") String userId) {

        return ResponseEntity.ok(
                shipmentService.cancelShipment(
                        shipmentId,
                        UUID.fromString(userId)
                )
        );
    }

    @Operation(
        summary = "Get active delivery code",
        description = "Returns the active delivery code for the shipment if available."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Delivery code returned"),
        @ApiResponse(responseCode = "204", description = "No active delivery code"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Not allowed")
    })
    @GetMapping("/{shipmentId}/delivery-code")
    public ResponseEntity<DeliveryCodeStatusResponse> getActiveCode(
            @PathVariable UUID shipmentId,
            @AuthenticationPrincipal(expression = "username") String userId) {

        DeliveryCodeStatusResponse response =
                deliveryCodeService.getActiveCode(
                        shipmentId,
                        UUID.fromString(userId)
                );

        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

}
