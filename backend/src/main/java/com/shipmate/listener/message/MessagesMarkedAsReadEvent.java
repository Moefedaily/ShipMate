package com.shipmate.listener.message;

import java.util.UUID;

public record MessagesMarkedAsReadEvent(
        UUID shipmentId,
        UUID userId
) {}
