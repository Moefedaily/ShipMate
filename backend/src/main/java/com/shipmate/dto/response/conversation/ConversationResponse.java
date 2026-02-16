package com.shipmate.dto.response.conversation;

import com.shipmate.model.shipment.ShipmentStatus;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
    UUID shipmentId,
    ShipmentStatus shipmentStatus,
    String lastMessagePreview,
    Instant lastMessageAt,
    long unreadCount,
    String otherUserName,
    String otherUserAvatarUrl
) {}
