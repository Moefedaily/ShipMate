package com.shipmate.controller.delivery;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shipmate.service.delivery.DeliveryCodeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/shipments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDeliveryController {

    private final DeliveryCodeService deliveryCodeService;

    @PostMapping("/{shipmentId}/unlock")
    public ResponseEntity<Void> unlockDelivery(
            @PathVariable UUID shipmentId
    ) {

        deliveryCodeService.unlockByAdmin(shipmentId);

        return ResponseEntity.ok().build();
    }
}
