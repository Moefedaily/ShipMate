package com.shipmate.dto.response.conversation;

import com.shipmate.dto.response.photo.PhotoResponse;
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
    PhotoResponse otherUserAvatar
) {}
