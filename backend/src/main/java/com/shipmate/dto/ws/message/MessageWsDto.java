package com.shipmate.dto.ws.message;

import com.shipmate.model.message.MessageType;

import java.time.Instant;
import java.util.UUID;

public record MessageWsDto(
        UUID id,
        UUID shipmentId,
        MessageType type,
        String content,
        Instant sentAt
) {}
