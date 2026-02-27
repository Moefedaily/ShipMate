package com.shipmate.mapper.booking;

import java.util.List;

import org.springframework.stereotype.Component;

import com.shipmate.dto.response.booking.BookingResponse;
import com.shipmate.dto.response.shipment.ShipmentResponse;
import com.shipmate.mapper.shipment.ShipmentAssembler;
import com.shipmate.model.booking.Booking;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BookingAssembler {

    private final BookingMapper bookingMapper;
    private final ShipmentAssembler shipmentAssembler;

    public BookingResponse toResponse(Booking booking) {

        BookingResponse response = bookingMapper.toResponse(booking);

        List<ShipmentResponse> shipmentResponses = booking.getShipments()
                .stream()
                .map(shipmentAssembler::toResponse)
                .toList();

        response.setShipments(shipmentResponses);

        return response;
    }
}
