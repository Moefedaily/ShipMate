package com.shipmate.dto.ws.delivery;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class DeliveryCodeWsDto {

    private UUID shipmentId;
    private String code;
}
