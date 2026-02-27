package com.shipmate.dto.response.delivery;

import java.time.Instant;
import java.util.UUID;

public record DeliveryCodeStatusResponse(
        UUID shipmentId,
        String code,
        Instant expiresAt
) {}
