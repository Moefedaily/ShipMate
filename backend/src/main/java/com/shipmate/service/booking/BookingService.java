package com.shipmate.service.booking;

import com.shipmate.dto.request.booking.CreateBookingRequest;
import com.shipmate.model.booking.Booking;
import com.shipmate.model.booking.BookingStatus;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.User;
import com.shipmate.repository.booking.BookingRepository;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
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

    // ===================== CREATE BOOKING =====================

    public Booking createBooking(UUID driverId, CreateBookingRequest request) {

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

        return savedBooking;
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
    // ===================== CONFIRM BOOKING =====================
    public Booking  confirm(UUID bookingId, UUID driverId) {
        Booking booking = loadDriverBooking(bookingId, driverId);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Only pending bookings can be confirmed");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        return booking;
    }

    // ===================== START BOOKING =====================

    public Booking start(UUID bookingId, UUID driverId) {
        Booking booking = loadDriverBooking(bookingId, driverId);

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed bookings can be started");
        }

        booking.setStatus(BookingStatus.IN_PROGRESS);
        return booking;
    }

    // ===================== COMPLETE BOOKING =====================

    public Booking complete(UUID bookingId, UUID driverId) {
        Booking booking = loadDriverBooking(bookingId, driverId);

        if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only in-progress bookings can be completed");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        return booking;
    }

    // ===================== CANCEL BOOKING =====================

    public Booking cancel(UUID bookingId, UUID driverId) {
        Booking booking = loadDriverBooking(bookingId, driverId);

        if (booking.getStatus() == BookingStatus.COMPLETED ||
            booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking cannot be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return booking;
    }

    // ===================== INTERNAL HELPER =====================

    private Booking loadDriverBooking(UUID bookingId, UUID driverId) {

        Booking booking = bookingRepository.findWithShipmentsById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!booking.getDriver().getId().equals(driverId)) {
            throw new AccessDeniedException("You are not allowed to access this booking");
        }

        return booking;
    }

    @Transactional(readOnly = true)
    public List<Booking> getMyBookings(UUID driverId) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

        return bookingRepository.findByDriver(driver);
    }

    @Transactional(readOnly = true)
    public Booking getMyBooking(UUID bookingId, UUID driverId) {
        Booking booking = bookingRepository.findWithShipmentsById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!booking.getDriver().getId().equals(driverId)) {
            throw new AccessDeniedException("You are not allowed to access this booking");
        }

        return booking;
    }


    @Transactional(readOnly = true)
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Booking getBookingById(UUID bookingId) {
        return bookingRepository.findWithShipmentsById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
    }


}
