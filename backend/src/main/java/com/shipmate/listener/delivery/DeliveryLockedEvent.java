package com.shipmate.listener.delivery;

import java.util.UUID;

public record DeliveryLockedEvent(
        UUID shipmentId,
        UUID senderId,
        UUID driverId
) {}
