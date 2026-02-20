package com.shipmate.dto.response.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shipmate.model.notification.NotificationType;
import com.shipmate.model.notification.ReferenceType;

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
    @JsonProperty("isRead")
    private boolean read;
    private UUID referenceId;
    private ReferenceType referenceType;
    private Instant createdAt;
    private Instant expiresAt;
}
