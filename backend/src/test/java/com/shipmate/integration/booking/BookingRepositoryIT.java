package com.shipmate.integration.booking;

import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.model.booking.Booking;
import com.shipmate.model.booking.BookingStatus;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.repository.booking.BookingRepository;
import com.shipmate.repository.user.UserRepository;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@Transactional
class BookingRepositoryIT extends AbstractIntegrationTest {
    
    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ===================== SAVE =====================

    @Test
    void save_shouldPersistBooking() {
        User driver = createDriver();

        Booking booking = Booking.builder()
                .driver(driver)
                .status(BookingStatus.PENDING)
                .totalPrice(BigDecimal.valueOf(100))
                .platformCommission(BigDecimal.valueOf(10))
                .driverEarnings(BigDecimal.valueOf(90))
                .build();

        Booking saved = bookingRepository.save(booking);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDriver().getId()).isEqualTo(driver.getId());
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    // ===================== FIND BY ID =====================

    @Test
    void findById_shouldReturnBooking_whenExists() {
        User driver = createDriver();

        Booking booking = bookingRepository.save(
                Booking.builder()
                        .driver(driver)
                        .status(BookingStatus.CONFIRMED)
                        .totalPrice(BigDecimal.valueOf(200))
                        .platformCommission(BigDecimal.valueOf(20))
                        .driverEarnings(BigDecimal.valueOf(180))
                        .build()
        );

        Optional<Booking> result = bookingRepository.findById(booking.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    // ===================== FIND BY DRIVER =====================

    @Test
    void findByDriver_shouldReturnOnlyDriverBookings() {
        User driver1 = createDriver();
        User driver2 = createDriver();

        bookingRepository.save(
                Booking.builder()
                        .driver(driver1)
                        .status(BookingStatus.PENDING)
                        .totalPrice(BigDecimal.valueOf(100))
                        .platformCommission(BigDecimal.valueOf(10))
                        .driverEarnings(BigDecimal.valueOf(90))
                        .build()
        );

        bookingRepository.save(
                Booking.builder()
                        .driver(driver1)
                        .status(BookingStatus.CONFIRMED)
                        .totalPrice(BigDecimal.valueOf(200))
                        .platformCommission(BigDecimal.valueOf(20))
                        .driverEarnings(BigDecimal.valueOf(180))
                        .build()
        );

        bookingRepository.save(
                Booking.builder()
                        .driver(driver2)
                        .status(BookingStatus.PENDING)
                        .totalPrice(BigDecimal.valueOf(150))
                        .platformCommission(BigDecimal.valueOf(15))
                        .driverEarnings(BigDecimal.valueOf(135))
                        .build()
        );

        List<Booking> driver1Bookings = bookingRepository.findByDriver(driver1);

        assertThat(driver1Bookings).hasSize(2);
        assertThat(driver1Bookings)
                .allMatch(b -> b.getDriver().getId().equals(driver1.getId()));
    }

    // ===================== NOT FOUND =====================

    @Test
    void findById_shouldReturnEmpty_whenNotFound() {
        Optional<Booking> result = bookingRepository.findById(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    // ===================== HELPER =====================

    private User createDriver() {
        return userRepository.save(
                User.builder()
                        .email("driver-" + UUID.randomUUID() + "@shipmate.com")
                        .password(passwordEncoder.encode("Password123!"))
                        .firstName("Test")
                        .lastName("Driver")
                        .role(Role.USER)
                        .userType(UserType.DRIVER)
                        .verified(true)
                        .active(true)
                        .build()
        );
    }
}