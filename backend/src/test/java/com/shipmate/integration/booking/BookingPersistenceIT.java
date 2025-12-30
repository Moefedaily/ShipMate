package com.shipmate.integration.booking;

import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.model.booking.Booking;
import com.shipmate.model.booking.BookingStatus;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.repository.booking.BookingRepository;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.repository.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class BookingPersistenceIT extends AbstractIntegrationTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ===================== BOOKING + SHIPMENTS =====================

    @Test
    void bookingAndShipments_shouldPersistRelationshipCorrectly() {
        // ===================== SETUP =====================

        User sender = createUser(UserType.SENDER);
        User driver = createUser(UserType.DRIVER);

        Shipment shipment1 = shipmentRepository.save(
                Shipment.builder()
                        .sender(sender)
                        .pickupAddress("Paris")
                        .pickupLatitude(BigDecimal.valueOf(48.85))
                        .pickupLongitude(BigDecimal.valueOf(2.35))
                        .deliveryAddress("Lyon")
                        .deliveryLatitude(BigDecimal.valueOf(45.76))
                        .deliveryLongitude(BigDecimal.valueOf(4.83))
                        .packageWeight(BigDecimal.valueOf(5))
                        .packageValue(BigDecimal.valueOf(100))
                        .requestedPickupDate(LocalDate.now())
                        .requestedDeliveryDate(LocalDate.now().plusDays(1))
                        .status(ShipmentStatus.CREATED)
                        .basePrice(BigDecimal.valueOf(50))
                        .extraInsuranceFee(BigDecimal.ZERO)
                        .build()
        );

        Shipment shipment2 = shipmentRepository.save(
                Shipment.builder()
                        .sender(sender)
                        .pickupAddress("Paris")
                        .pickupLatitude(BigDecimal.valueOf(48.85))
                        .pickupLongitude(BigDecimal.valueOf(2.35))
                        .deliveryAddress("Marseille")
                        .deliveryLatitude(BigDecimal.valueOf(43.29))
                        .deliveryLongitude(BigDecimal.valueOf(5.37))
                        .packageWeight(BigDecimal.valueOf(3))
                        .packageValue(BigDecimal.valueOf(80))
                        .requestedPickupDate(LocalDate.now())
                        .requestedDeliveryDate(LocalDate.now().plusDays(2))
                        .status(ShipmentStatus.CREATED)
                        .basePrice(BigDecimal.valueOf(40))
                        .extraInsuranceFee(BigDecimal.ZERO)
                        .build()
        );

        // ===================== CREATE BOOKING =====================

        Booking booking = bookingRepository.save(
                Booking.builder()
                        .driver(driver)
                        .status(BookingStatus.PENDING)
                        .totalPrice(BigDecimal.valueOf(90))
                        .platformCommission(BigDecimal.valueOf(9))
                        .driverEarnings(BigDecimal.valueOf(81))
                        .build()
        );

        shipment1.setBooking(booking);
        shipment1.setStatus(ShipmentStatus.ASSIGNED);

        shipment2.setBooking(booking);
        shipment2.setStatus(ShipmentStatus.ASSIGNED);

        shipmentRepository.saveAll(List.of(shipment1, shipment2));

        // ===================== ASSERTIONS =====================

        Booking persistedBooking =
                bookingRepository.findById(booking.getId()).orElseThrow();

        List<Shipment> persistedShipments =
                shipmentRepository.findByBookingId(booking.getId());

        assertThat(persistedBooking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(persistedShipments).hasSize(2);

        assertThat(persistedShipments)
                .allMatch(s -> s.getBooking().getId().equals(booking.getId()))
                .allMatch(s -> s.getStatus() == ShipmentStatus.ASSIGNED);
    }

    // ===================== HELPER =====================

    private User createUser(UserType userType) {
        String typePrefix = userType == UserType.DRIVER ? "driver" : "sender";
        return userRepository.save(
                User.builder()
                        .email(typePrefix + "-" + UUID.randomUUID() + "@shipmate.com")
                        .password(passwordEncoder.encode("Password123!"))
                        .firstName("Test")
                        .lastName("User")
                        .role(Role.USER)
                        .userType(userType)
                        .verified(true)
                        .active(true)
                        .build()
        );
    }
}