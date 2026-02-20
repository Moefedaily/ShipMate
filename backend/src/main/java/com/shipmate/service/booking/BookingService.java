package com.shipmate.service.booking;

import com.shipmate.dto.request.booking.CreateBookingRequest;
import com.shipmate.dto.response.booking.BookingResponse;
import com.shipmate.dto.response.driver.AssignedDriverResponse;
import com.shipmate.exception.BookingConstraintException;
import com.shipmate.exception.DriverLocationException;
import com.shipmate.model.DriverProfile.DriverProfile;
import com.shipmate.model.booking.Booking;
import com.shipmate.model.booking.BookingStatus;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.User;
import com.shipmate.repository.booking.BookingRepository;
import com.shipmate.repository.driver.DriverProfileRepository;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.shipment.ShipmentService;

import org.springframework.context.ApplicationEventPublisher;
import com.shipmate.listener.booking.BookingStatusChangedEvent;
import com.shipmate.listener.payment.PaymentRequiredEvent;
import com.shipmate.mapper.booking.BookingAssembler;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final ShipmentService shipmentService;
    private final BookingAssembler bookingAssembler;
    private final ApplicationEventPublisher eventPublisher;

    private static final Duration LOCATION_MAX_AGE = Duration.ofMinutes(60);
    private static final double MAX_DISTANCE_KM = 10.0;


    public Booking createBooking(UUID driverId, CreateBookingRequest request) {

        List<Shipment> shipments = shipmentRepository.findAllById(
                request.getShipmentIds()
        );

        if (shipments.isEmpty()) {
            throw new IllegalArgumentException("No shipments found");
        }

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

        DriverProfile profile = driverProfileRepository
                .findByUser_Id(driverId)
                .orElseThrow(() -> new IllegalStateException("Driver profile not found"));

        Booking booking = resolveOrCreatePendingBooking(driver);

        if (booking.getShipments() == null) {
            booking.setShipments(new ArrayList<>());
        }

        validateDriverLocation(driverId, shipments.get(0));
        validateShipmentsForBooking(shipments, booking);

        validateMaxShipments(profile, booking, shipments);
        validatePickupRadius(profile, booking, shipments);
        validateTripDistanceCap(profile, booking, shipments);

        int routeOrder = nextRouteOrder(booking);

        for (Shipment shipment : shipments) {
            shipment.setBooking(booking);
            shipment.setStatus(ShipmentStatus.ASSIGNED);
            shipment.setPickupOrder(routeOrder++);
            shipment.setDeliveryOrder(routeOrder++);
            booking.getShipments().add(shipment);
        }

        shipmentRepository.saveAll(shipments);

        recalculatePricing(booking);
        bookingRepository.save(booking);

        return bookingRepository
                .findWithShipmentsById(booking.getId())
                .orElseThrow();
    }


    public Booking confirm(UUID bookingId, UUID driverId) {
        Booking booking = loadDriverBooking(bookingId, driverId);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw BookingConstraintException.locked();
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        publishStatusChange(booking, driverId);
        for (Shipment shipment : booking.getShipments()) {

        eventPublisher.publishEvent(
            new PaymentRequiredEvent(
                    shipment.getId(),
                    shipment.getSender().getId()
            )
        );
        }

        return booking;
    }


    public Booking start(UUID bookingId, UUID driverId) {

        Booking booking = loadDriverBooking(bookingId, driverId);

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed bookings can be started");
        }

        // Move booking to IN_PROGRESS
        booking.setStatus(BookingStatus.IN_PROGRESS);

        // Find first shipment that is ASSIGNED
        Shipment firstShipment = booking.getShipments()
                .stream()
                .filter(s -> s.getStatus() == ShipmentStatus.ASSIGNED)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("No shipment available to start")
                );

        // Delegate shipment transition to ShipmentService
        shipmentService.markInTransit(firstShipment.getId(), driverId);

        publishStatusChange(booking, driverId);

        return booking;
    }

    public Booking complete(UUID bookingId, UUID driverId) {
        Booking booking = loadDriverBooking(bookingId, driverId);

        if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only in-progress bookings can be completed");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        publishStatusChange(booking, driverId);
        return booking;
    }


    public Booking cancel(UUID bookingId, UUID driverId) {
        Booking booking = loadDriverBooking(bookingId, driverId);

        if (booking.getStatus() == BookingStatus.COMPLETED ||
            booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking cannot be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        publishStatusChange(booking, driverId);
        return booking;
    }


    private Booking resolveOrCreatePendingBooking(User driver) {
        return bookingRepository
                .findFirstByDriverAndStatusInOrderByCreatedAtDesc(
                        driver,
                        List.of(BookingStatus.PENDING)
                )
                .orElseGet(() -> bookingRepository.save(
                        Booking.builder()
                                .driver(driver)
                                .status(BookingStatus.PENDING)
                                .totalPrice(BigDecimal.ZERO)
                                .platformCommission(BigDecimal.ZERO)
                                .driverEarnings(BigDecimal.ZERO)
                                .build()
                ));
    }

    private void recalculatePricing(Booking booking) {
        BigDecimal total = booking.getShipments().stream()
                .map(Shipment::getBasePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal commission = total.multiply(BigDecimal.valueOf(0.10));

        booking.setTotalPrice(total);
        booking.setPlatformCommission(commission);
        booking.setDriverEarnings(total.subtract(commission));
    }

    private void validateShipmentsForBooking(List<Shipment> shipments, Booking booking) {
        for (Shipment shipment : shipments) {
            if (shipment.getBooking() != null) {
                throw new IllegalStateException("Shipment already assigned");
            }

            if (shipment.getStatus() != ShipmentStatus.CREATED) {
                throw new IllegalStateException("Shipment is not available");
            }

            if (booking.getStatus() != BookingStatus.PENDING) {
                throw BookingConstraintException.locked();
            }
        }
    }

    private void validateDriverLocation(UUID driverId, Shipment shipment) {
        DriverProfile profile = driverProfileRepository
                .findByUser_Id(driverId)
                .orElseThrow(() -> new IllegalStateException("Driver profile not found"));

        if (profile.getLastLatitude() == null ||
            profile.getLastLongitude() == null ||
            profile.getLastLocationUpdatedAt() == null) {
            throw DriverLocationException.required();
        }

        Instant cutoff = Instant.now().minus(LOCATION_MAX_AGE);
        if (profile.getLastLocationUpdatedAt().isBefore(cutoff)) {
            throw DriverLocationException.outdated();
        }

        double distanceKm = haversine(
                profile.getLastLatitude().doubleValue(),
                profile.getLastLongitude().doubleValue(),
                shipment.getPickupLatitude().doubleValue(),
                shipment.getPickupLongitude().doubleValue()
        );

        if (distanceKm > MAX_DISTANCE_KM) {
            throw DriverLocationException.tooFar(distanceKm);
        }
    }

    private void validateMaxShipments(DriverProfile profile, Booking booking, List<Shipment> incoming) {
        int existing = booking.getShipments() != null
                ? booking.getShipments().size()
                : 0;

        int maxAllowed = switch (profile.getVehicleType()) {
            case BICYCLE -> 1;
            case MOTORCYCLE -> 2;
            case CAR -> 3;
            case VAN -> 6;
            case TRUCK -> 10;
        };

        if (existing + incoming.size() > maxAllowed) {
            throw BookingConstraintException.shipmentLimitExceeded(maxAllowed);
        }
    }

    private void validatePickupRadius(DriverProfile profile, Booking booking, List<Shipment> incoming) {
        if (booking.getShipments() == null || booking.getShipments().isEmpty()) {
            return;
        }

        Shipment anchor = booking.getShipments().get(0);

        double maxRadiusKm = switch (profile.getVehicleType()) {
            case BICYCLE -> 5.0;
            case MOTORCYCLE -> 8.0;
            case CAR -> 12.0;
            case VAN -> 18.0;
            case TRUCK -> 25.0;
        };

        for (Shipment shipment : incoming) {
            double distance = haversine(
                    anchor.getPickupLatitude().doubleValue(),
                    anchor.getPickupLongitude().doubleValue(),
                    shipment.getPickupLatitude().doubleValue(),
                    shipment.getPickupLongitude().doubleValue()
            );

            if (distance > maxRadiusKm) {
                throw BookingConstraintException.tripDistanceExceeded();
            }
        }
    }

    private void validateTripDistanceCap(DriverProfile profile, Booking booking, List<Shipment> incoming) {
        double maxKm = switch (profile.getVehicleType()) {
            case BICYCLE -> 20;
            case MOTORCYCLE -> 40;
            case CAR -> 120;
            case VAN -> 200;
            case TRUCK -> 500;
        };

        Shipment anchor = booking.getShipments().isEmpty()
                ? incoming.get(0)
                : booking.getShipments().get(0);

        double totalKm = 0;

        for (Shipment s : booking.getShipments()) {
            totalKm += haversine(
                anchor.getPickupLatitude().doubleValue(),
                anchor.getPickupLongitude().doubleValue(),
                s.getDeliveryLatitude().doubleValue(),
                s.getDeliveryLongitude().doubleValue()
            );
        }

        for (Shipment s : incoming) {
            totalKm += haversine(
                anchor.getPickupLatitude().doubleValue(),
                anchor.getPickupLongitude().doubleValue(),
                s.getDeliveryLatitude().doubleValue(),
                s.getDeliveryLongitude().doubleValue()
            );
        }

        if (totalKm > maxKm) {
            throw BookingConstraintException.tripDistanceExceeded();
        }
    }

    private int nextRouteOrder(Booking booking) {
        if (booking.getShipments() == null || booking.getShipments().isEmpty()) {
            return 1;
        }

        return booking.getShipments().stream()
            .<Integer>mapMulti((s, consumer) -> {
                consumer.accept(s.getPickupOrder());
                consumer.accept(s.getDeliveryOrder());
            })
            .filter(Objects::nonNull)
            .max(Integer::compareTo)
            .orElse(0) + 1;
    }

    private Booking loadDriverBooking(UUID bookingId, UUID driverId) {
        Booking booking = bookingRepository.findWithShipmentsById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!booking.getDriver().getId().equals(driverId)) {
            throw new AccessDeniedException("You are not allowed to access this booking");
        }

        return booking;
    }

    @Transactional(readOnly = true)
    public Booking getMyActiveBooking(UUID driverId) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

        return bookingRepository
                .findFirstByDriverAndStatusInOrderByCreatedAtDesc(
                        driver,
                        List.of(
                                BookingStatus.PENDING,
                                BookingStatus.CONFIRMED,
                                BookingStatus.IN_PROGRESS
                        )
                )
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Booking> getMyBookings(UUID driverId) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

        return bookingRepository.findByDriver(driver);
    }

    @Transactional(readOnly = true)
    public BookingResponse getMyBooking(UUID bookingId, UUID driverId) {
        Booking booking = bookingRepository.findWithShipmentsById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!booking.getDriver().getId().equals(driverId)) {
            throw new AccessDeniedException("You are not allowed to access this booking");
        }

        BookingResponse response = bookingAssembler.toResponse(booking);

        AssignedDriverResponse driver = buildAssignedDriver(booking.getDriver());

        response.getShipments().forEach(s -> s.setDriver(driver));

        return response;
    }

    private AssignedDriverResponse buildAssignedDriver(User driver) {
        DriverProfile profile = driverProfileRepository
                .findByUser_Id(driver.getId())
                .orElse(null);

        if (profile == null) return null;

        return AssignedDriverResponse.builder()
                .id(profile.getId())
                .firstName(driver.getFirstName())
                .avatarUrl(driver.getAvatarUrl())
                .vehicleType(profile.getVehicleType())
                .build();
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

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth radius in km

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) *
                Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
    private void publishStatusChange(Booking booking, UUID actorId) {
        eventPublisher.publishEvent(
                new BookingStatusChangedEvent(
                        booking.getId(),
                        booking.getStatus(),
                        actorId
                )
        );
    }

}