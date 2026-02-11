package com.shipmate.dto.ws.message;

import com.shipmate.model.message.MessageType;

import java.time.Instant;
import java.util.UUID;

public record MessageWsDto(
        Long id,
        UUID bookingId,
        MessageType type,
        String content,
        Instant sentAt
) {}
