package com.shipmate.mapper;

import com.shipmate.dto.response.booking.BookingResponse;
import com.shipmate.model.booking.Booking;
import com.shipmate.model.shipment.Shipment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(source = "driver.id", target = "driverId")
    @Mapping(source = "shipments", target = "shipmentIds")
    BookingResponse toResponse(Booking booking);

    // ===================== HELPERS =====================

    default List<UUID> mapShipmentsToIds(List<Shipment> shipments) {
        return shipments == null
                ? List.of()
                : shipments.stream()
                           .map(Shipment::getId)
                           .toList();
    }
}
