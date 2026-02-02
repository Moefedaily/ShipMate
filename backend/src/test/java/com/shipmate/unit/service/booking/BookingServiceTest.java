package com.shipmate.unit.service.booking;

import com.shipmate.dto.request.booking.CreateBookingRequest;
import com.shipmate.exception.BookingConstraintErrorCode;
import com.shipmate.exception.BookingConstraintException;
import com.shipmate.model.DriverProfile.DriverProfile;
import com.shipmate.model.booking.Booking;
import com.shipmate.model.booking.BookingStatus;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.User;
import com.shipmate.model.user.VehicleType;
import com.shipmate.repository.booking.BookingRepository;
import com.shipmate.repository.driver.DriverProfileRepository;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.booking.BookingService;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.security.access.AccessDeniedException;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingService bookingService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private DriverProfileRepository driverProfileRepository;


    private UUID bookingId;
    private UUID driverId;
    private User driver;
    private Booking booking;

    @BeforeEach
    void setup() {
        bookingId = UUID.randomUUID();
        driverId = UUID.randomUUID();

        driver = User.builder()
                .id(driverId)
                .build();

        booking = Booking.builder()
                .id(bookingId)
                .driver(driver)
                .status(BookingStatus.PENDING)
                .totalPrice(BigDecimal.valueOf(100))
                .platformCommission(BigDecimal.valueOf(10))
                .driverEarnings(BigDecimal.valueOf(90))
                .build();
    }

    // ===================== CONFIRM =====================

    @Test
    void confirm_shouldSucceed_whenStatusIsPending() {
        when(bookingRepository.findWithShipmentsById(bookingId))
                .thenReturn(Optional.of(booking));

        Booking result = bookingService.confirm(bookingId, driverId);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void confirm_shouldFail_whenStatusIsNotPending() {
        booking.setStatus(BookingStatus.CANCELLED);

        when(bookingRepository.findWithShipmentsById(bookingId))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() ->
                bookingService.confirm(bookingId, driverId))
                .isInstanceOf(BookingConstraintException.class)
                .hasMessageContaining("Booking is already confirmed or in progress");
    }

    // ===================== CANCEL =====================

    @Test
    void cancel_shouldSucceed_whenStatusIsPending() {
        when(bookingRepository.findWithShipmentsById(bookingId))
                .thenReturn(Optional.of(booking));

        Booking result = bookingService.cancel(bookingId, driverId);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancel_shouldSucceed_whenStatusIsConfirmed() {
        booking.setStatus(BookingStatus.CONFIRMED);

        when(bookingRepository.findWithShipmentsById(bookingId))
                .thenReturn(Optional.of(booking));

        Booking result = bookingService.cancel(bookingId, driverId);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancel_shouldFail_whenStatusIsCompleted() {
        booking.setStatus(BookingStatus.COMPLETED);

        when(bookingRepository.findWithShipmentsById(bookingId))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() ->
                bookingService.cancel(bookingId, driverId))
                .isInstanceOf(IllegalStateException.class);
    }

    // ===================== COMPLETE =====================

    @Test
    void complete_shouldSucceed_whenStatusIsInProgress() {
        booking.setStatus(BookingStatus.IN_PROGRESS);

        when(bookingRepository.findWithShipmentsById(bookingId))
                .thenReturn(Optional.of(booking));

        Booking result = bookingService.complete(bookingId, driverId);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.COMPLETED);
    }

    @Test
    void complete_shouldFail_whenStatusIsNotInProgress() {
        booking.setStatus(BookingStatus.CONFIRMED);

        when(bookingRepository.findWithShipmentsById(bookingId))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() ->
                bookingService.complete(bookingId, driverId))
                .isInstanceOf(IllegalStateException.class);
    }

    // ===================== OWNERSHIP =====================

    @Test
    void action_shouldFail_whenDriverIsNotOwner() {
        UUID otherDriverId = UUID.randomUUID();

        when(bookingRepository.findWithShipmentsById(bookingId))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() ->
                bookingService.confirm(bookingId, otherDriverId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not allowed");
    }

    // ===================== READ (DRIVER) =====================

    @Test
    void getMyBookings_shouldReturnOnlyDriverBookings() {
        when(userRepository.findById(driverId))
                .thenReturn(Optional.of(driver));

        when(bookingRepository.findByDriver(driver))
                .thenReturn(List.of(booking));

        List<Booking> result = bookingService.getMyBookings(driverId);

        assertThat(result)
                .hasSize(1)
                .containsExactly(booking);
    }

    @Test
    void getMyBooking_shouldFail_whenNotOwner() {
        UUID otherDriverId = UUID.randomUUID();

        when(bookingRepository.findWithShipmentsById(bookingId))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() ->
                bookingService.getMyBooking(bookingId, otherDriverId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not allowed");
    }

    // ===================== READ (ADMIN) =====================

    @Test
    void getAllBookings_shouldReturnAll() {
        Booking secondBooking = Booking.builder()
                .id(UUID.randomUUID())
                .driver(User.builder().id(UUID.randomUUID()).build())
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findAll())
                .thenReturn(List.of(booking, secondBooking));

        List<Booking> result = bookingService.getAllBookings();

        assertThat(result)
                .hasSize(2)
                .containsExactlyInAnyOrder(booking, secondBooking);
    }

    @Test
    void getBookingById_shouldFail_whenNotFound() {
        when(bookingRepository.findWithShipmentsById(bookingId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                bookingService.getBookingById(bookingId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Booking not found");
    }

    
    @Test
    void createBooking_shouldFail_whenShipmentLimitExceeded_forCar() {

        booking.setShipments(List.of(
                Shipment.builder()
                        .status(ShipmentStatus.CREATED)
                        .pickupLatitude(BigDecimal.ZERO)
                        .pickupLongitude(BigDecimal.ZERO)
                        .deliveryLatitude(BigDecimal.ONE)
                        .deliveryLongitude(BigDecimal.ONE)
                        .basePrice(BigDecimal.TEN)
                        .build(),
                Shipment.builder()
                        .status(ShipmentStatus.CREATED)
                        .pickupLatitude(BigDecimal.ZERO)
                        .pickupLongitude(BigDecimal.ZERO)
                        .deliveryLatitude(BigDecimal.ONE)
                        .deliveryLongitude(BigDecimal.ONE)
                        .basePrice(BigDecimal.TEN)
                        .build(),
                Shipment.builder()
                        .status(ShipmentStatus.CREATED)
                        .pickupLatitude(BigDecimal.ZERO)
                        .pickupLongitude(BigDecimal.ZERO)
                        .deliveryLatitude(BigDecimal.ONE)
                        .deliveryLongitude(BigDecimal.ONE)
                        .basePrice(BigDecimal.TEN)
                        .build()
        ));

        Shipment incoming = Shipment.builder()
                .status(ShipmentStatus.CREATED)
                .pickupLatitude(BigDecimal.ZERO)
                .pickupLongitude(BigDecimal.ZERO)
                .deliveryLatitude(BigDecimal.ONE)
                .deliveryLongitude(BigDecimal.ONE)
                .basePrice(BigDecimal.TEN)
                .build();

        DriverProfile profile = DriverProfile.builder()
                .vehicleType(VehicleType.CAR)
                .lastLatitude(BigDecimal.ZERO)
                .lastLongitude(BigDecimal.ZERO)
                .lastLocationUpdatedAt(Instant.now())
                .build();

        when(shipmentRepository.findAllById(any()))
                .thenReturn(List.of(incoming));

        when(userRepository.findById(driverId))
                .thenReturn(Optional.of(driver));

        when(driverProfileRepository.findByUser_Id(driverId))
                .thenReturn(Optional.of(profile));

        when(bookingRepository.findFirstByDriverAndStatusInOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() ->
                bookingService.createBooking(
                        driverId,
                        new CreateBookingRequest(List.of(UUID.randomUUID()))
                ))
            .isInstanceOf(BookingConstraintException.class)
            .extracting("code")
            .isEqualTo(BookingConstraintErrorCode.SHIPMENT_LIMIT_EXCEEDED);
    }

    @Test
    void createBooking_shouldFail_whenPickupRadiusExceeded() {

        booking.setShipments(List.of(
                Shipment.builder()
                        .status(ShipmentStatus.CREATED)
                        .pickupLatitude(BigDecimal.ZERO)
                        .pickupLongitude(BigDecimal.ZERO)
                        .deliveryLatitude(BigDecimal.ONE)
                        .deliveryLongitude(BigDecimal.ONE)
                        .basePrice(BigDecimal.TEN)
                        .build()
        ));

        Shipment incoming = Shipment.builder()
                .status(ShipmentStatus.CREATED)
                .pickupLatitude(BigDecimal.valueOf(0.05))   // far away
                .pickupLongitude(BigDecimal.valueOf(0.05))
                .deliveryLatitude(BigDecimal.ONE)
                .deliveryLongitude(BigDecimal.ONE)
                .basePrice(BigDecimal.TEN)
                .build();

        DriverProfile profile = DriverProfile.builder()
                .vehicleType(VehicleType.MOTORCYCLE)
                .lastLatitude(BigDecimal.ZERO)
                .lastLongitude(BigDecimal.ZERO)
                .lastLocationUpdatedAt(Instant.now())
                .build();

        when(shipmentRepository.findAllById(any()))
                .thenReturn(List.of(incoming));

        when(userRepository.findById(driverId))
                .thenReturn(Optional.of(driver));

        when(driverProfileRepository.findByUser_Id(driverId))
                .thenReturn(Optional.of(profile));

        when(bookingRepository.findFirstByDriverAndStatusInOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() ->
                bookingService.createBooking(
                        driverId,
                        new CreateBookingRequest(List.of(UUID.randomUUID()))
                ))
            .isInstanceOf(BookingConstraintException.class)
            .extracting("code")
            .isEqualTo(BookingConstraintErrorCode.TRIP_DISTANCE_CAP_EXCEEDED);
    }


    @Test
    void createBooking_shouldFail_whenTripDistanceCapExceeded() {

        booking.setShipments(List.of(
                Shipment.builder()
                        .status(ShipmentStatus.CREATED)
                        .pickupLatitude(BigDecimal.ZERO)
                        .pickupLongitude(BigDecimal.ZERO)
                        .deliveryLatitude(BigDecimal.valueOf(30))
                        .deliveryLongitude(BigDecimal.valueOf(30))
                        .basePrice(BigDecimal.TEN)
                        .build()
        ));

        Shipment incoming = Shipment.builder()
                .status(ShipmentStatus.CREATED)
                .pickupLatitude(BigDecimal.ZERO)
                .pickupLongitude(BigDecimal.ZERO)
                .deliveryLatitude(BigDecimal.valueOf(40))
                .deliveryLongitude(BigDecimal.valueOf(40))
                .basePrice(BigDecimal.TEN)
                .build();

        DriverProfile profile = DriverProfile.builder()
                .vehicleType(VehicleType.CAR)
                .lastLatitude(BigDecimal.ZERO)
                .lastLongitude(BigDecimal.ZERO)
                .lastLocationUpdatedAt(Instant.now())
                .build();

        when(shipmentRepository.findAllById(any()))
                .thenReturn(List.of(incoming));

        when(userRepository.findById(driverId))
                .thenReturn(Optional.of(driver));

        when(driverProfileRepository.findByUser_Id(driverId))
                .thenReturn(Optional.of(profile));

        when(bookingRepository.findFirstByDriverAndStatusInOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() ->
                bookingService.createBooking(
                        driverId,
                        new CreateBookingRequest(List.of(UUID.randomUUID()))
                ))
            .isInstanceOf(BookingConstraintException.class)
            .extracting("code")
            .isEqualTo(BookingConstraintErrorCode.TRIP_DISTANCE_CAP_EXCEEDED);
    }

    @Test
    void createBooking_shouldFail_whenBookingIsNotPending() {

        booking.setStatus(BookingStatus.CONFIRMED);

        Shipment incoming = Shipment.builder()
                .status(ShipmentStatus.CREATED)
                .pickupLatitude(BigDecimal.ZERO)
                .pickupLongitude(BigDecimal.ZERO)
                .deliveryLatitude(BigDecimal.ONE)
                .deliveryLongitude(BigDecimal.ONE)
                .basePrice(BigDecimal.TEN)
                .build();

        DriverProfile profile = DriverProfile.builder()
                .vehicleType(VehicleType.CAR)
                .lastLatitude(BigDecimal.ZERO)
                .lastLongitude(BigDecimal.ZERO)
                .lastLocationUpdatedAt(Instant.now())
                .build();

        when(shipmentRepository.findAllById(any()))
                .thenReturn(List.of(incoming));

        when(userRepository.findById(driverId))
                .thenReturn(Optional.of(driver));

        when(driverProfileRepository.findByUser_Id(driverId))
                .thenReturn(Optional.of(profile));

        when(bookingRepository.findFirstByDriverAndStatusInOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() ->
                bookingService.createBooking(
                        driverId,
                        new CreateBookingRequest(List.of(UUID.randomUUID()))
                ))
            .isInstanceOf(BookingConstraintException.class)
            .extracting("code")
            .isEqualTo(BookingConstraintErrorCode.BOOKING_LOCKED);
    }


}
