package com.shipmate.integration.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.dto.request.auth.LoginRequest;
import com.shipmate.dto.request.booking.CreateBookingRequest;
import com.shipmate.model.booking.BookingStatus;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.repository.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookingDriverIT extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String DEFAULT_PASSWORD = "Password123!";

    // ===================== GET MY BOOKINGS =====================

    @Test
    void getMyBookings_shouldReturnOnlyDriverBookings() throws Exception {
        String driverToken = createAndLoginDriver();

        mockMvc.perform(get("/api/bookings/my")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ===================== GET MY BOOKING =====================

    @Test
    void getMyBooking_shouldSucceed_whenOwner() throws Exception {
        String driverToken = createAndLoginDriver();
        UUID bookingId = createBookingForLoggedDriver(driverToken);

        mockMvc.perform(get("/api/bookings/{id}", bookingId)
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId.toString()));
    }

    @Test
    void getMyBooking_shouldFail_whenNotOwner() throws Exception {
        String driverToken = createAndLoginDriver();
        UUID otherBookingId = createBookingForAnotherDriver();

        mockMvc.perform(get("/api/bookings/{id}", otherBookingId)
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isForbidden());
    }

    // ===================== STATUS TRANSITIONS =====================

    @Test
    void confirm_shouldSucceed_whenPending() throws Exception {
        String driverToken = createAndLoginDriver();
        UUID bookingId = createBookingForLoggedDriver(driverToken);

        mockMvc.perform(post("/api/bookings/{id}/confirm", bookingId)
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(BookingStatus.CONFIRMED.name()));
    }

    @Test
    void complete_shouldFail_whenNotInProgress() throws Exception {
        String driverToken = createAndLoginDriver();
        UUID bookingId = createBookingForLoggedDriver(driverToken);

        mockMvc.perform(post("/api/bookings/{id}/complete", bookingId)
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isBadRequest());
    }

    // ===================== HELPERS =====================

    private String createAndLoginDriver() throws Exception {
        String email = "driver-" + UUID.randomUUID() + "@shipmate.com";

        User driver = User.builder()
                .email(email)
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .firstName("Test")
                .lastName("Driver")
                .role(Role.USER)
                .userType(UserType.DRIVER)
                .verified(true)
                .active(true)
                .build();

        userRepository.saveAndFlush(driver);

        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(DEFAULT_PASSWORD)
                .deviceId("test-device-" + UUID.randomUUID())
                .sessionId("test-session-" + UUID.randomUUID())
                .build();

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private UUID createBookingForLoggedDriver(String token) throws Exception {
        UUID shipmentId = createAvailableShipment();
        CreateBookingRequest request = new CreateBookingRequest(List.of(shipmentId));

        String response = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return UUID.fromString(
                objectMapper.readTree(response).get("id").asText()
        );
    }

    private UUID createBookingForAnotherDriver() throws Exception {
        String anotherDriverToken = createAndLoginDriver();
        return createBookingForLoggedDriver(anotherDriverToken);
    }

    private UUID createAvailableShipment() {
        String senderEmail = "sender-" + UUID.randomUUID() + "@shipmate.com";

        User sender = User.builder()
                .email(senderEmail)
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .firstName("Test")
                .lastName("Sender")
                .role(Role.USER)
                .userType(UserType.SENDER)
                .verified(true)
                .active(true)
                .build();

        userRepository.saveAndFlush(sender);

        Shipment shipment = Shipment.builder()
        .sender(sender)
        .pickupAddress("Paris")
        .pickupLatitude(BigDecimal.valueOf(48.8566))
        .pickupLongitude(BigDecimal.valueOf(2.3522))
        .deliveryAddress("Lyon")
        .deliveryLatitude(BigDecimal.valueOf(45.7640))
        .deliveryLongitude(BigDecimal.valueOf(4.8357))
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
        return shipmentRepository.saveAndFlush(shipment).getId();
    }
}