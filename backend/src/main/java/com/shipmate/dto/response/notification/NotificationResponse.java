package com.shipmate.dto.response.notification;

import com.shipmate.model.notification.NotificationType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private UUID id;
    private String title;
    private String message;
    private NotificationType notificationType;
    private boolean read;
    private Instant createdAt;
    private Instant expiresAt;
}
