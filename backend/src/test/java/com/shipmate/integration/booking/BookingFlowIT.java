package com.shipmate.integration.booking;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.dto.request.booking.CreateBookingRequest;
import com.shipmate.model.booking.Booking;
import com.shipmate.model.booking.BookingStatus;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.booking.BookingService;

class BookingFlowIT extends AbstractIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Test
    void shouldFollowCompleteBookingLifecycle() {
        User driver = createDriver();
        Shipment shipment = createAvailableShipment();

        // CREATE
        Booking booking = bookingService.createBooking(
                driver.getId(),
                new CreateBookingRequest(List.of(shipment.getId()))
        );

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);

        // CONFIRM
        booking = bookingService.confirm(booking.getId(), driver.getId());
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        // START
        booking = bookingService.start(booking.getId(), driver.getId());
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.IN_PROGRESS);

        // COMPLETE
        booking = bookingService.complete(booking.getId(), driver.getId());
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
    }

    // --------------------------------------------------
    // Invalid transitions
    // --------------------------------------------------
    @Test
    void shouldRejectInvalidBookingTransitions() {
        User driver = createDriver();
        Shipment shipment = createAvailableShipment();

        Booking booking = bookingService.createBooking(
                driver.getId(),
                new CreateBookingRequest(List.of(shipment.getId()))
        );

        // Cannot complete directly from PENDING
        assertThatThrownBy(() ->
                bookingService.complete(booking.getId(), driver.getId()))
                .isInstanceOf(IllegalStateException.class);

        // Cannot start before confirm
        assertThatThrownBy(() ->
                bookingService.start(booking.getId(), driver.getId()))
                .isInstanceOf(IllegalStateException.class);
    }

    // --------------------------------------------------
    // Ownership enforcement
    // --------------------------------------------------
    @Test
    void shouldRejectActionsFromNonOwner() {
        User owner = createDriver();
        User intruder = createDriver();
        Shipment shipment = createAvailableShipment();

        Booking booking = bookingService.createBooking(
                owner.getId(),
                new CreateBookingRequest(List.of(shipment.getId()))
        );

        assertThatThrownBy(() ->
                bookingService.confirm(booking.getId(), intruder.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not allowed");
    }

    // --------------------------------------------------
    // Helpers
    // --------------------------------------------------
    private User createDriver() {
        return userRepository.saveAndFlush(
                User.builder()
                        .email("driver-" + UUID.randomUUID() + "@shipmate.com")
                        .password(passwordEncoder.encode("Password123!"))
                        .firstName("Driver")
                        .lastName("Test")
                        .role(Role.USER)
                        .userType(UserType.DRIVER)
                        .verified(true)
                        .active(true)
                        .build()
        );
    }

    private Shipment createAvailableShipment() {
        User sender = userRepository.saveAndFlush(
                User.builder()
                        .email("sender-" + UUID.randomUUID() + "@shipmate.com")
                        .password(passwordEncoder.encode("Password123!"))
                        .firstName("Sender")
                        .lastName("Test")
                        .role(Role.USER)
                        .userType(UserType.SENDER)
                        .verified(true)
                        .active(true)
                        .build()
        );

        Shipment shipment = Shipment.builder()
                .sender(sender)
                .pickupAddress("Paris")
                .pickupLatitude(BigDecimal.valueOf(48.8566))
                .pickupLongitude(BigDecimal.valueOf(2.3522))
                .deliveryAddress("Lyon")
                .deliveryLatitude(BigDecimal.valueOf(45.7640))
                .deliveryLongitude(BigDecimal.valueOf(4.8357))
                .packageWeight(BigDecimal.valueOf(2))
                .packageValue(BigDecimal.valueOf(100))
                .requestedPickupDate(LocalDate.now())
                .requestedDeliveryDate(LocalDate.now().plusDays(1))
                .status(ShipmentStatus.CREATED)
                .basePrice(BigDecimal.valueOf(20))
                .extraInsuranceFee(BigDecimal.ZERO)
                .build();

        return shipmentRepository.saveAndFlush(shipment);
    }
}
