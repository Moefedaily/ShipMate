package com.shipmate.api.shipment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.dto.request.shipment.CreateShipmentRequest;
import com.shipmate.dto.request.shipment.UpdateShipmentRequest;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.repository.user.UserRepository;

class ShipmentControllerIT extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    /* =========================
       CREATE
       ========================= */

    @Test
    void createShipment_shouldSucceed_whenAuthenticated() throws Exception {
        User user = createUser("create-" + UUID.randomUUID() + "@shipmate.com");

        String token = obtainAccessToken(user.getEmail(), "Password123!");

        CreateShipmentRequest request = createRequest();

        mockMvc.perform(post("/api/shipments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void createShipment_shouldFail_whenUnauthorized() throws Exception {
        mockMvc.perform(post("/api/shipments")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    /* =========================
       READ
       ========================= */

    @Test
    void getMyShipments_shouldReturnCreatedShipment() throws Exception {
        User user = createUser("list-" + UUID.randomUUID() + "@shipmate.com");

        String token = obtainAccessToken(user.getEmail(), "Password123!");

        createShipment(token);

        mockMvc.perform(get("/api/shipments/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getMyShipmentById_shouldReturnShipment() throws Exception {
        User user = createUser("get-" + UUID.randomUUID() + "@shipmate.com");
        String token = obtainAccessToken(user.getEmail(), "Password123!");

        UUID shipmentId = createShipmentAndReturnId(token);

        mockMvc.perform(get("/api/shipments/{id}", shipmentId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(shipmentId.toString()));
    }

    /* =========================
       UPDATE
       ========================= */

    @Test
    void updateShipment_shouldSucceed_whenCreated() throws Exception {
        User user = createUser("update-" + UUID.randomUUID() + "@shipmate.com");

        String token = obtainAccessToken(user.getEmail(), "Password123!");

        UUID shipmentId = createShipmentAndReturnId(token);

        UpdateShipmentRequest update = new UpdateShipmentRequest();
        update.setPickupAddress("Updated Paris");
        update.setDeliveryAddress("Updated Lyon");
        update.setPackageWeight(BigDecimal.valueOf(3));
        update.setRequestedPickupDate(LocalDate.now());
        update.setRequestedDeliveryDate(LocalDate.now().plusDays(2));

        mockMvc.perform(put("/api/shipments/{id}", shipmentId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
            .andExpect(status().isOk());
    }

    /* =========================
       DELETE
       ========================= */

    @Test
    void deleteShipment_shouldSucceed_whenCreated() throws Exception {
        User user = createUser("delete-" + UUID.randomUUID() + "@shipmate.com");

        String token = obtainAccessToken(user.getEmail(), "Password123!");

        UUID shipmentId = createShipmentAndReturnId(token);

        mockMvc.perform(delete("/api/shipments/{id}", shipmentId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());
    }

    /* =========================
       Helpers
       ========================= */

    private UUID createShipmentAndReturnId(String token) throws Exception {
        String response = createShipment(token);

        return UUID.fromString(
                objectMapper.readTree(response).get("id").asText()
        );
    }

    private String createShipment(String token) throws Exception {
        return mockMvc.perform(post("/api/shipments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest())))
            .andReturn()
            .getResponse()
            .getContentAsString();
    }

    private CreateShipmentRequest createRequest() {
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setPickupAddress("Paris");
        request.setPickupLatitude(BigDecimal.valueOf(48.8566));
        request.setPickupLongitude(BigDecimal.valueOf(2.3522));
        request.setDeliveryAddress("Lyon");
        request.setDeliveryLatitude(BigDecimal.valueOf(45.7640));
        request.setDeliveryLongitude(BigDecimal.valueOf(4.8357));
        request.setPackageWeight(BigDecimal.valueOf(2.5));
        request.setPackageValue(BigDecimal.valueOf(100));
        request.setRequestedPickupDate(LocalDate.now());
        request.setRequestedDeliveryDate(LocalDate.now().plusDays(1));
        request.setBasePrice(BigDecimal.valueOf(20));
        return request;
    }

    private User createUser(String email) {
        return userRepository.save(
                User.builder()
                        .email(email)
                        .password(passwordEncoder.encode("Password123!"))
                        .firstName("Test")
                        .lastName("User")
                        .role(Role.USER)
                        .userType(UserType.DRIVER)
                        .verified(true)
                        .active(true)
                        .build()
        );
    }
}
