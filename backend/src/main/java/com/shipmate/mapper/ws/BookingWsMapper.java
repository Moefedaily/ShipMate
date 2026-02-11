package com.shipmate.mapper.ws;

import com.shipmate.dto.ws.booking.BookingStatusUpdateWsDto;
import com.shipmate.model.booking.Booking;

public class BookingWsMapper {

    public static BookingStatusUpdateWsDto toWsDto(Booking booking) {
        return new BookingStatusUpdateWsDto(
                booking.getId(),
                booking.getStatus()
        );
    }
}
