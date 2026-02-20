package com.shipmate.listener.notification;

import com.shipmate.model.notification.NotificationType;
import com.shipmate.model.notification.ReferenceType;

import java.util.UUID;

public record NotificationRequestedEvent(
        UUID recipientUserId,
        String title,
        String message,
        NotificationType type,
        UUID referenceId,
        ReferenceType referenceType
) {}
