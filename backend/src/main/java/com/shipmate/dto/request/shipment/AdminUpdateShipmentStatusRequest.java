package com.shipmate.dto.request.shipment;

import com.shipmate.model.shipment.ShipmentStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUpdateShipmentStatusRequest {
    private ShipmentStatus status;
    private String adminNotes;
}