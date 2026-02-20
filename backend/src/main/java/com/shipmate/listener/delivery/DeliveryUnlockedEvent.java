package com.shipmate.listener.delivery;

import java.util.UUID;

public record DeliveryUnlockedEvent(
        UUID shipmentId,
        UUID senderId,
        UUID driverId
) {}
