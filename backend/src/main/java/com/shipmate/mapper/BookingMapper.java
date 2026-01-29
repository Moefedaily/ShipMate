package com.shipmate.mapper;

import com.shipmate.dto.response.booking.BookingResponse;
import com.shipmate.model.booking.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring", uses = ShipmentMapper.class)
public interface BookingMapper {
    
    @Mapping(source = "driver.id", target = "driverId")
    @Mapping(source = "shipments", target = "shipments")
    BookingResponse toResponse(Booking booking);
    
} 