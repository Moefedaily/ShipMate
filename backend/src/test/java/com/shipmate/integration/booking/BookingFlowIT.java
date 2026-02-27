package com.shipmate.integration.booking;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.dto.request.booking.CreateBookingRequest;
import com.shipmate.model.DriverProfile.DriverProfile;
import com.shipmate.model.DriverProfile.DriverStatus;
import com.shipmate.model.booking.Booking;
import com.shipmate.model.booking.BookingStatus;
import com.shipmate.model.payment.Payment;
import com.shipmate.model.payment.PaymentStatus;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.model.user.VehicleType;
import com.shipmate.repository.driver.DriverProfileRepository;
import com.shipmate.repository.payment.PaymentRepository;
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

    @Autowired
    private DriverProfileRepository driverProfileRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void shouldFollowCompleteBookingLifecycle() {


        User driver = createDriver();
        Shipment shipment = createAvailableShipment();

        Booking booking = bookingService.createBooking(
                driver.getId(),
                new CreateBookingRequest(List.of(shipment.getId()))
        );

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);

        assertThat(booking.getTotalPrice())
                .isEqualByComparingTo(shipment.getBasePrice());

        BigDecimal expectedCommission =
                shipment.getBasePrice().multiply(BigDecimal.valueOf(0.10));

        assertThat(booking.getPlatformCommission())
                .isEqualByComparingTo(expectedCommission);

        assertThat(booking.getDriverEarnings())
                .isEqualByComparingTo(
                        shipment.getBasePrice().subtract(expectedCommission)
                );

        Shipment assignedShipment = shipmentRepository
                .findById(shipment.getId())
                .orElseThrow();

        assertThat(assignedShipment.getStatus())
                .isEqualTo(ShipmentStatus.ASSIGNED);

        booking = bookingService.confirm(booking.getId(), driver.getId());
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        Payment payment = Payment.builder()
                .shipment(assignedShipment)
                .sender(assignedShipment.getSender())
                .amountTotal(assignedShipment.getBasePrice())
                .currency("EUR")
                .stripePaymentIntentId("pi_test_" + UUID.randomUUID())
                .paymentStatus(PaymentStatus.AUTHORIZED)
                .build();

        paymentRepository.saveAndFlush(payment);

        booking = bookingService.start(booking.getId(), driver.getId());

        assertThat(booking.getStatus())
                .isEqualTo(BookingStatus.IN_PROGRESS);

        Shipment inTransitShipment = shipmentRepository
                .findById(shipment.getId())
                .orElseThrow();

        assertThat(inTransitShipment.getStatus())
                .isEqualTo(ShipmentStatus.IN_TRANSIT);

        booking = bookingService.complete(booking.getId(), driver.getId());

        assertThat(booking.getStatus())
                .isEqualTo(BookingStatus.COMPLETED);
    }

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

    private User createDriver() {

        User driver = userRepository.saveAndFlush(
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

        driverProfileRepository.saveAndFlush(
                DriverProfile.builder()
                        .user(driver)
                        .vehicleType(VehicleType.CAR)
                        .licenseNumber("LIC-" + UUID.randomUUID())
                        .vehicleDescription("Test car")
                        .maxWeightCapacity(BigDecimal.valueOf(500))
                        .status(DriverStatus.APPROVED)
                        .lastLatitude(BigDecimal.valueOf(48.8566))
                        .lastLongitude(BigDecimal.valueOf(2.3522))
                        .lastLocationUpdatedAt(Instant.now())
                        .build()
        );

        return driver;
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
        .deliveryLatitude(BigDecimal.valueOf(48.8666))
        .deliveryLongitude(BigDecimal.valueOf(2.3622))
        .packageWeight(BigDecimal.valueOf(2.50).setScale(2))
        .packageValue(BigDecimal.valueOf(100.00).setScale(2))
        .requestedPickupDate(LocalDate.now())
        .requestedDeliveryDate(LocalDate.now().plusDays(1))
        .status(ShipmentStatus.CREATED)
        .basePrice(BigDecimal.valueOf(20.00).setScale(2))
        .insuranceSelected(false)
        .insuranceFee(BigDecimal.ZERO.setScale(2))
        .declaredValue(null)
        .insuranceCoverageAmount(null)
        .insuranceDeductibleRate(null)
        .deliveryLocked(false)
        .build();
        return shipmentRepository.saveAndFlush(shipment);
    }
    
}
