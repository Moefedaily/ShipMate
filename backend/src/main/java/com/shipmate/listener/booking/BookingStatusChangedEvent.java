package com.shipmate.listener.booking;


import com.shipmate.model.booking.BookingStatus;

import java.util.UUID;

public record BookingStatusChangedEvent(
        UUID bookingId,
        BookingStatus status,
        UUID actorId
) {}
