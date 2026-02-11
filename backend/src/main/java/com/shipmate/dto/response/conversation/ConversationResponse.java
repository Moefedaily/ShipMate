package com.shipmate.dto.response.conversation;

import com.shipmate.model.booking.BookingStatus;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
    
    UUID bookingId,
    BookingStatus bookingStatus,

    String lastMessagePreview,
    Instant lastMessageAt,

    long unreadCount
) {}
