package com.shipmate.dto.response.message;

import com.shipmate.model.message.MessageType;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        Long id,
        UUID bookingId,
        MessageType messageType,
        String messageContent,
        UUID senderId,
        UUID receiverId,
        boolean read,
        Instant sentAt
) {}
