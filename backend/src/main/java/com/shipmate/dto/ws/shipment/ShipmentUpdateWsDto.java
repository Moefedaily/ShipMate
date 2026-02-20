package com.shipmate.dto.ws.shipment;

import java.util.UUID;

import com.shipmate.model.shipment.ShipmentStatus;

public record ShipmentUpdateWsDto(
        UUID shipmentId,
        ShipmentStatus status,
        boolean deliveryLocked
) {}