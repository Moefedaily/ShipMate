package com.shipmate.api.matching;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.dto.request.driver.UpdateDriverLocationRequest;
import com.shipmate.model.DriverProfile.DriverProfile;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ShipmentMatchingControllerIT extends AbstractIntegrationTest {

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

    // ===================== MATCH SHIPMENTS =====================

    @Test
    void matchShipments_shouldReturnResults_whenAuthenticatedDriver() throws Exception {
        String token = createAndLoginDriverWithProfile();

        // driver sets last known location
        updateDriverLocation(token);

        // create available shipment
        createAvailableShipment();

        mockMvc.perform(get("/api/matching/shipments")
                        .header("Authorization", "Bearer " + token)
                        .param("radiusKm", "20")
                        .param("maxResults", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].shipment.id").exists())
                .andExpect(jsonPath("$[0].metrics.distanceToPickupKm").exists())
                .andExpect(jsonPath("$[0].metrics.score").exists());
    }

    // ===================== SECURITY =====================

    @Test
    void matchShipments_shouldFail_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/matching/shipments"))
                .andExpect(status().isForbidden());
    }

    // ===================== DRIVER LOCATION =====================

    @Test
    void updateMyLocation_shouldSucceed_whenAuthenticatedDriver() throws Exception {
        String token = createAndLoginDriverWithProfile();

        UpdateDriverLocationRequest request = UpdateDriverLocationRequest.builder()
                .latitude(BigDecimal.valueOf(48.8566))
                .longitude(BigDecimal.valueOf(2.3522))
                .build();

        mockMvc.perform(post("/api/drivers/me/location")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ===================== HELPERS =====================

    private String createAndLoginDriverWithProfile() throws Exception {
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
                        .vehicleType(VehicleType.CAR)
                        .vehicleDescription("Test car")
                        .licenseNumber("TEST-" + UUID.randomUUID())
                        .maxWeightCapacity(BigDecimal.valueOf(50))
                        .build()
        );

        return obtainAccessToken(driver.getEmail(), PASSWORD);
    }

    private void updateDriverLocation(String token) throws Exception {
        UpdateDriverLocationRequest request = UpdateDriverLocationRequest.builder()
                .latitude(BigDecimal.valueOf(48.8566))
                .longitude(BigDecimal.valueOf(2.3522))
                .build();

        mockMvc.perform(post("/api/drivers/me/location")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private void createAvailableShipment() {
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
                .pickupLatitude(BigDecimal.valueOf(48.8570))
                .pickupLongitude(BigDecimal.valueOf(2.3530))
                .deliveryAddress("Paris")
                .deliveryLatitude(BigDecimal.valueOf(48.8600))
                .deliveryLongitude(BigDecimal.valueOf(2.3600))
                .packageWeight(BigDecimal.valueOf(5))
                .packageValue(BigDecimal.valueOf(100))
                .requestedPickupDate(LocalDate.now())
                .requestedDeliveryDate(LocalDate.now().plusDays(1))
                .status(ShipmentStatus.CREATED)
                .basePrice(BigDecimal.valueOf(20))
                .build();

        shipmentRepository.saveAndFlush(shipment);
    }

    @Test
    void matchShipments_shouldFail_whenAuthenticatedSender() throws Exception {
        // --------------------
        // GIVEN an authenticated sender
        // --------------------
        String email = "sender-" + UUID.randomUUID() + "@shipmate.com";

        User sender = userRepository.save(
                User.builder()
                        .email(email)
                        .password(passwordEncoder.encode(PASSWORD))
                        .firstName("Sender")
                        .lastName("Test")
                        .role(Role.USER)
                        .userType(UserType.SENDER)
                        .verified(true)
                        .active(true)
                        .build()
        );

        String token = obtainAccessToken(sender.getEmail(), PASSWORD);

        // --------------------
        // WHEN accessing matching endpoint
        // --------------------
        mockMvc.perform(get("/api/matching/shipments")
                        .header("Authorization", "Bearer " + token)
                        .param("radiusKm", "20")
                        .param("maxResults", "10"))
                // --------------------
                // THEN access is denied
                // --------------------
                .andExpect(status().isForbidden());
    }

}
