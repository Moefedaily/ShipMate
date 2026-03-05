package com.shipmate.dto.response.admin;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ShipmentSummary {

    private UUID id;

    private String pickupAddress;

    private String deliveryAddress;
}