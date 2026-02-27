package com.shipmate.api.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.dto.request.booking.CreateBookingRequest;
import com.shipmate.model.DriverProfile.DriverProfile;
import com.shipmate.model.DriverProfile.DriverStatus;
import com.shipmate.model.booking.BookingStatus;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.model.user.VehicleType;
import com.shipmate.repository.driver.DriverProfileRepository;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.repository.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
class BookingControllerIT extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DriverProfileRepository driverProfileRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String PASSWORD = "Password123!";


    @Test
    void createBooking_shouldSucceed_whenAuthenticatedDriver() throws Exception {
        String token = createAndLoginDriver();
        updateDriverLocation(token);
        UUID shipmentId = createAvailableShipment();

        CreateBookingRequest request =
                new CreateBookingRequest(List.of(shipmentId));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(BookingStatus.PENDING.name()));
    }


    @Test
    void getMyBookings_shouldReturnDriverBookings() throws Exception {
        String token = createAndLoginDriver();
        createBookingForToken(token);

        mockMvc.perform(get("/api/bookings/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ===================== GET MY BOOKING =====================

    @Test
    void getMyBooking_shouldSucceed_whenOwner() throws Exception {
        String token = createAndLoginDriver();
        UUID bookingId = createBookingForToken(token);

        mockMvc.perform(get("/api/bookings/{id}", bookingId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId.toString()));
    }

    @Test
    void getMyBooking_shouldFail_whenNotOwner() throws Exception {
        String ownerToken = createAndLoginDriver();
        UUID bookingId = createBookingForToken(ownerToken);

        String otherDriverToken = createAndLoginDriver();

        mockMvc.perform(get("/api/bookings/{id}", bookingId)
                        .header("Authorization", "Bearer " + otherDriverToken))
                .andExpect(status().isForbidden());
    }

    // ===================== STATUS TRANSITIONS =====================

    @Test
    void confirmBooking_shouldSucceed_whenPending() throws Exception {
        String token = createAndLoginDriver();
        UUID bookingId = createBookingForToken(token);

        mockMvc.perform(post("/api/bookings/{id}/confirm", bookingId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(BookingStatus.CONFIRMED.name()));
    }

    @Test
    void completeBooking_shouldFail_whenNotInProgress() throws Exception {
        String token = createAndLoginDriver();
        UUID bookingId = createBookingForToken(token);

        mockMvc.perform(post("/api/bookings/{id}/complete", bookingId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    // ===================== HELPERS =====================

    private String createAndLoginDriver() throws Exception {
        String email = "driver-" + UUID.randomUUID() + "@shipmate.com";

        User driver = userRepository.save(
                User.builder()
                        .email(email)
                        .password(passwordEncoder.encode(PASSWORD))
                        .firstName("Test")
                        .lastName("Driver")
                        .role(Role.USER)
                        .userType(UserType.DRIVER)
                        .verified(true)
                        .active(true)
                        .build()
        );

        driverProfileRepository.saveAndFlush(
                DriverProfile.builder()
                        .user(driver)
                        .licenseNumber("LIC-" + UUID.randomUUID())
                        .vehicleType(VehicleType.CAR)
                        .maxWeightCapacity(BigDecimal.valueOf(500))
                        .vehicleDescription("Test vehicle")
                        .status(DriverStatus.APPROVED)
                        .build()
        );


        return obtainAccessToken(driver.getEmail(), PASSWORD);
    }

        private UUID createFarShipment() {
        User sender = userRepository.save(
                User.builder()
                        .email("sender-" + UUID.randomUUID() + "@shipmate.com")
                        .password(passwordEncoder.encode(PASSWORD))
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
                .deliveryAddress("Very Far")
                .deliveryLatitude(BigDecimal.valueOf(50.8566))
                .deliveryLongitude(BigDecimal.valueOf(4.3522))
                .packageWeight(BigDecimal.valueOf(2.5))
                .packageValue(BigDecimal.valueOf(100))
                .requestedPickupDate(LocalDate.now())
                .requestedDeliveryDate(LocalDate.now().plusDays(1))
                .status(ShipmentStatus.CREATED)
                .basePrice(BigDecimal.valueOf(20))
                .build();

        return shipmentRepository.saveAndFlush(shipment).getId();
        }
        @Test
        void createBooking_shouldFail_whenTripTooLong() throws Exception {
        String token = createAndLoginDriver();
        updateDriverLocation(token);

        UUID farShipmentId = createFarShipment();

        CreateBookingRequest request =
                new CreateBookingRequest(List.of(farShipmentId));

        mockMvc.perform(post("/api/bookings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRIP_DISTANCE_CAP_EXCEEDED"));
        }
    private void updateDriverLocation(String token) throws Exception {
        mockMvc.perform(post("/api/drivers/me/location")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                            "latitude": 48.8566,
                            "longitude": 2.3522
                            }
                            """))
                .andExpect(status().isOk());
    }

    private UUID createBookingForToken(String token) throws Exception {
        updateDriverLocation(token);
        UUID shipmentId = createAvailableShipment();

        CreateBookingRequest request =
                new CreateBookingRequest(List.of(shipmentId));

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

    private UUID createAvailableShipment() {
        User sender = userRepository.save(
                User.builder()
                        .email("sender-" + UUID.randomUUID() + "@shipmate.com")
                        .password(passwordEncoder.encode(PASSWORD))
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
                .packageWeight(BigDecimal.valueOf(2.5))
                .packageValue(BigDecimal.valueOf(100))
                .requestedPickupDate(LocalDate.now())
                .requestedDeliveryDate(LocalDate.now().plusDays(1))
                .status(ShipmentStatus.CREATED)
                .basePrice(BigDecimal.valueOf(20))
                .build();

        return shipmentRepository.saveAndFlush(shipment).getId();
    }
    
}
