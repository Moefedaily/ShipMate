package com.shipmate.service.booking;

import com.shipmate.dto.request.booking.CreateBookingRequest;
import com.shipmate.dto.response.booking.BookingResponse;
import com.shipmate.mapper.BookingMapper;
import com.shipmate.model.booking.Booking;
import com.shipmate.model.booking.BookingStatus;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.User;
import com.shipmate.repository.booking.BookingRepository;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;

    // ===================== CREATE BOOKING =====================

    public BookingResponse createBooking(UUID driverId, CreateBookingRequest request) {

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

        List<Shipment> shipments = shipmentRepository.findAllById(request.getShipmentIds());

        if (shipments.isEmpty()) {
            throw new IllegalArgumentException("No shipments found");
        }

        // ===================== VALIDATION =====================

        validateShipmentsForBooking(shipments);

        // ===================== PRICE CALCULATION =====================

        BigDecimal totalPrice = shipments.stream()
                .map(Shipment::getBasePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal platformCommission = totalPrice.multiply(BigDecimal.valueOf(0.10));
        BigDecimal driverEarnings = totalPrice.subtract(platformCommission);

        // ===================== CREATE BOOKING =====================

        Booking booking = Booking.builder()
                .driver(driver)
                .status(BookingStatus.PENDING)
                .totalPrice(totalPrice)
                .platformCommission(platformCommission)
                .driverEarnings(driverEarnings)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        // ===================== ATTACH SHIPMENTS =====================

        shipments.forEach(shipment -> {
            shipment.setBooking(savedBooking);
            shipment.setStatus(ShipmentStatus.ASSIGNED);
        });

        return bookingMapper.toResponse(savedBooking);
    }

    // ===================== VALIDATION RULES =====================

    private void validateShipmentsForBooking(List<Shipment> shipments) {

        for (Shipment shipment : shipments) {

            if (shipment.getBooking() != null) {
                throw new IllegalStateException("Shipment already assigned to a booking");
            }

            if (shipment.getStatus() != ShipmentStatus.CREATED) {
                throw new IllegalStateException("Shipment is not available for booking");
            }
        }
    }
}
