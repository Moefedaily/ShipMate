package com.shipmate.dto.ws.booking;

import com.shipmate.model.booking.BookingStatus;

import java.util.UUID;

public record BookingStatusUpdateWsDto(
        UUID bookingId,
        BookingStatus status
) {}
