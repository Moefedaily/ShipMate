package com.shipmate.unit.service.booking;

import com.shipmate.dto.request.booking.CreateBookingRequest;
import com.shipmate.dto.response.booking.BookingResponse;
import com.shipmate.exception.BookingConstraintErrorCode;
import com.shipmate.exception.BookingConstraintException;
import com.shipmate.listener.booking.BookingStatusChangedEvent;
import com.shipmate.listener.payment.PaymentRequiredEvent;
import com.shipmate.mapper.booking.BookingAssembler;
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
import com.shipmate.service.shipment.ShipmentService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private ShipmentRepository shipmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private DriverProfileRepository driverProfileRepository;
    @Mock private ShipmentService shipmentService;
    @Mock private BookingAssembler bookingAssembler;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BookingService bookingService;

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
                .firstName("Driver")
                .build();

        booking = Booking.builder()
                .id(bookingId)
                .driver(driver)
                .status(BookingStatus.PENDING)
                .totalPrice(BigDecimal.valueOf(100))
                .platformCommission(BigDecimal.valueOf(10))
                .driverEarnings(BigDecimal.valueOf(90))
                .shipments(new ArrayList<>())
                .build();
    }

    // ===================== CONFIRM =====================

    @Test
    void confirm_shouldSucceed_whenStatusIsPending_andPublishEvents() {

        // booking must contain shipments with sender, because confirm() publishes PaymentRequiredEvent(senderId)
        Shipment s1 = Shipment.builder()
                .id(UUID.randomUUID())
                .status(ShipmentStatus.ASSIGNED)
                .sender(User.builder().id(UUID.randomUUID()).build())
                .build();

        Shipment s2 = Shipment.builder()
                .id(UUID.randomUUID())
                .status(ShipmentStatus.ASSIGNED)
                .sender(User.builder().id(UUID.randomUUID()).build())
                .build();

        booking.setShipments(List.of(s1, s2));

        when(bookingRepository.findWithShipmentsById(bookingId))
                .thenReturn(Optional.of(booking));

        Booking result = bookingService.confirm(bookingId, driverId);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        // Verify status change event published
        verify(eventPublisher).publishEvent(any(BookingStatusChangedEvent.class));

        // Verify PaymentRequiredEvent published for each shipment
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeast(3)).publishEvent(captor.capture());

        long paymentRequiredCount = captor.getAllValues().stream()
                .filter(e -> e instanceof PaymentRequiredEvent)
                .count();

        assertThat(paymentRequiredCount).isEqualTo(2);
    }

    @Test
    void confirm_shouldFail_whenStatusIsNotPending() {

        booking.setStatus(BookingStatus.CANCELLED);

        when(bookingRepository.findWithShipmentsById(bookingId))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.confirm(bookingId, driverId))
                .isInstanceOf(BookingConstraintException.class);
        // Don't assert old message, since service uses BookingConstraintException.locked()
    }

    // ===================== START =====================

    @Test
    void start_shouldSucceed_whenConfirmed_andDelegateFirstShipmentToInTransit() {

        booking.setStatus(BookingStatus.CONFIRMED);

        Shipment firstAssigned = Shipment.builder()
                .id(UUID.randomUUID())
                .status(ShipmentStatus.ASSIGNED)
                .sender(User.builder().id(UUID.randomUUID()).build())
                .build();

        booking.setShipments(List.of(firstAssigned));

        when(bookingRepository.findWithShipmentsById(bookingId))
                .thenReturn(Optional.of(booking));

        // shipmentService.markInTransit returns ShipmentResponse in real life, but method ignores return
        doReturn(null).when(shipmentService).markInTransit(firstAssigned.getId(), driverId);

        Booking result = bookingService.start(bookingId, driverId);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.IN_PROGRESS);
        verify(shipmentService).markInTransit(firstAssigned.getId(), driverId);
        verify(eventPublisher).publishEvent(any(BookingStatusChangedEvent.class));
    }

    @Test
    void start_shouldFail_whenNotConfirmed() {

        booking.setStatus(BookingStatus.PENDING);

        when(bookingRepository.findWithShipmentsById(bookingId))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.start(bookingId, driverId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only confirmed bookings can be started");
    }

    // ===================== CANCEL =====================

    @Test
    void cancel_shouldSucceed_whenStatusIsPending() {

        when(bookingRepository.findWithShipmentsById(bookingId))
                .thenReturn(Optional.of(booking));

        Booking result = bookingService.cancel(bookingId, driverId);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(eventPublisher).publishEvent(any(BookingStatusChangedEvent.class));
    }

    @Test
    void cancel_shouldSucceed_whenStatusIsConfirmed() {

        booking.setStatus(BookingStatus.CONFIRMED);

        when(bookingRepository.findWithShipmentsById(bookingId))
                .thenReturn(Optional.of(booking));

        Booking result = bookingService.cancel(bookingId, driverId);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(eventPublisher).publishEvent(any(BookingStatusChangedEvent.class));
    }

    @Test
    void cancel_shouldFail_whenStatusIsCompleted() {

        booking.setStatus(BookingStatus.COMPLETED);

        when(bookingRepository.findWithShipmentsById(bookingId))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(bookingId, driverId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Booking cannot be cancelled");
    }

    // ===================== COMPLETE =====================

    @Test
    void complete_shouldSucceed_whenStatusIsInProgress() {

        booking.setStatus(BookingStatus.IN_PROGRESS);

        when(bookingRepository.findWithShipmentsById(bookingId))
                .thenReturn(Optional.of(booking));

        Booking result = bookingService.complete(bookingId, driverId);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        verify(eventPublisher).publishEvent(any(BookingStatusChangedEvent.class));
    }

    @Test
    void complete_shouldFail_whenStatusIsNotInProgress() {

        booking.setStatus(BookingStatus.CONFIRMED);

        when(bookingRepository.findWithShipmentsById(bookingId))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.complete(bookingId, driverId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only in-progress bookings can be completed");
    }

    // ===================== OWNERSHIP =====================

    @Test
    void action_shouldFail_whenDriverIsNotOwner() {

        UUID otherDriverId = UUID.randomUUID();

        when(bookingRepository.findWithShipmentsById(bookingId))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.confirm(bookingId, otherDriverId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not allowed");
    }

    // ===================== READ (DRIVER) =====================

    @Test
    void getMyBookings_shouldReturnOnlyDriverBookings() {

        when(userRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(bookingRepository.findByDriver(driver)).thenReturn(List.of(booking));

        List<Booking> result = bookingService.getMyBookings(driverId);

        assertThat(result).hasSize(1).containsExactly(booking);
    }

    @Test
    void getMyActiveBooking_shouldReturnActive_whenExists() {

        when(userRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(bookingRepository.findFirstByDriverAndStatusInOrderByCreatedAtDesc(
                eq(driver),
                anyList()
        )).thenReturn(Optional.of(booking));

        Booking result = bookingService.getMyActiveBooking(driverId);

        assertThat(result).isSameAs(booking);
    }

    @Test
    void getMyBooking_shouldSucceed_whenOwner() {

        when(bookingRepository.findWithShipmentsById(bookingId))
                .thenReturn(Optional.of(booking));

        BookingResponse response = new BookingResponse();
        response.setShipments(new ArrayList<>());

        when(bookingAssembler.toResponse(booking)).thenReturn(response);

        // driver profile optional; if null, driver info not added
        when(driverProfileRepository.findByUser_Id(driverId))
                .thenReturn(Optional.empty());

        BookingResponse result = bookingService.getMyBooking(bookingId, driverId);

        assertThat(result).isSameAs(response);
        verify(bookingAssembler).toResponse(booking);
    }

    @Test
    void getMyBooking_shouldFail_whenNotOwner() {

        UUID otherDriverId = UUID.randomUUID();

        when(bookingRepository.findWithShipmentsById(bookingId))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.getMyBooking(bookingId, otherDriverId))
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

        when(bookingRepository.findAll()).thenReturn(List.of(booking, secondBooking));

        List<Booking> result = bookingService.getAllBookings();

        assertThat(result).hasSize(2).containsExactlyInAnyOrder(booking, secondBooking);
    }

    @Test
    void getBookingById_shouldFail_whenNotFound() {

        when(bookingRepository.findWithShipmentsById(bookingId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBookingById(bookingId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Booking not found");
    }

    // ===================== CREATE BOOKING (CONSTRAINTS) =====================

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

        when(shipmentRepository.findAllById(any())).thenReturn(List.of(incoming));
        when(userRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(driverProfileRepository.findByUser_Id(driverId)).thenReturn(Optional.of(profile));

        // ensure resolveOrCreatePendingBooking returns existing pending booking
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
                .pickupLatitude(BigDecimal.valueOf(0.05)) // far
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

        when(shipmentRepository.findAllById(any())).thenReturn(List.of(incoming));
        when(userRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(driverProfileRepository.findByUser_Id(driverId)).thenReturn(Optional.of(profile));
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

        when(shipmentRepository.findAllById(any())).thenReturn(List.of(incoming));
        when(userRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(driverProfileRepository.findByUser_Id(driverId)).thenReturn(Optional.of(profile));
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

        when(shipmentRepository.findAllById(any())).thenReturn(List.of(incoming));
        when(userRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(driverProfileRepository.findByUser_Id(driverId)).thenReturn(Optional.of(profile));
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