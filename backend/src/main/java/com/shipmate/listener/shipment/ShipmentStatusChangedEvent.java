package com.shipmate.listener.shipment;

import com.shipmate.model.shipment.ShipmentStatus;

import java.util.UUID;

public record ShipmentStatusChangedEvent(
        UUID shipmentId,
        ShipmentStatus status
) {}
