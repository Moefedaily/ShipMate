package com.shipmate.dto.ws.notification;

import com.shipmate.model.notification.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationWsDto(
        UUID id,
        String title,
        String message,
        NotificationType type,
        Instant createdAt
) {}
