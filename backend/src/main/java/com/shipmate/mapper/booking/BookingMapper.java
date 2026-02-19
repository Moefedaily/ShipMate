package com.shipmate.mapper.booking;

import com.shipmate.dto.response.booking.BookingResponse;
import com.shipmate.model.booking.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(source = "driver.id", target = "driverId")
    @Mapping(target = "shipments", ignore = true)
    BookingResponse toResponse(Booking booking);
}
