package com.shipmate.integration.shipment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.dto.request.auth.LoginRequest;
import com.shipmate.dto.request.shipment.CreateShipmentRequest;
import com.shipmate.dto.response.auth.AuthResponse;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.shipment.ShipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class AdminShipmentFlowIT extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ShipmentService shipmentService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;
    private User regularUser;

    @BeforeEach
    void setUp() throws Exception {
        User admin = createVerifiedUser("admin-" + UUID.randomUUID() + "@shipmate.com", Role.ADMIN, UserType.SENDER);
        adminToken = getAuthToken(admin.getEmail(), "Password123!");

        regularUser = createVerifiedUser("user-" + UUID.randomUUID() + "@shipmate.com", Role.USER, UserType.SENDER);
        userToken = getAuthToken(regularUser.getEmail(), "Password123!");
    }

    @Test
    void getAllShipments_shouldReturnAllShipments_whenAdmin() throws Exception {
        User user1 = createVerifiedUser("user1-" + UUID.randomUUID() + "@shipmate.com", Role.USER, UserType.SENDER);
        User user2 = createVerifiedUser("user2-" + UUID.randomUUID() + "@shipmate.com", Role.USER, UserType.SENDER);
        shipmentService.create(user1.getId(), createRequest());
        shipmentService.create(user2.getId(), createRequest());

        mockMvc.perform(get("/api/admin/shipments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2));
    }

    @Test
    void getShipmentById_shouldReturnShipment_whenAdmin() throws Exception {
        var created = shipmentService.create(regularUser.getId(), createRequest());

        mockMvc.perform(get("/api/admin/shipments/{id}", created.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId().toString()))
                .andExpect(jsonPath("$.senderId").value(regularUser.getId().toString()));
    }

    @Test
    void getAllShipments_shouldReturnForbidden_whenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/shipments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getShipmentById_shouldReturnForbidden_whenNotAdmin() throws Exception {
        var created = shipmentService.create(regularUser.getId(), createRequest());

        mockMvc.perform(get("/api/admin/shipments/{id}", created.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ---------------- helpers ----------------

    private User createVerifiedUser(String email, Role role, UserType userType) {
        return userRepository.save(
                User.builder()
                        .email(email)
                        .password(passwordEncoder.encode("Password123!"))
                        .firstName("Test")
                        .lastName("User")
                        .role(role)
                        .userType(userType)
                        .verified(true)
                        .active(true)
                        .build()
        );
    }

    private String getAuthToken(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);
        loginRequest.setDeviceId("test-device");
        loginRequest.setSessionId("test-session");

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        AuthResponse authResponse = objectMapper.readValue(response, AuthResponse.class);
        return authResponse.getAccessToken();
    }

     private CreateShipmentRequest createRequest() {
        CreateShipmentRequest req = new CreateShipmentRequest();
        req.setPickupAddress("Paris");
        req.setPickupLatitude(BigDecimal.valueOf(48.8566));
        req.setPickupLongitude(BigDecimal.valueOf(2.3522));
        req.setDeliveryAddress("Lyon");
        req.setDeliveryLatitude(BigDecimal.valueOf(45.7640));
        req.setDeliveryLongitude(BigDecimal.valueOf(4.8357));
        req.setPackageDescription("Box");
        req.setPackageWeight(BigDecimal.valueOf(2.5));
        req.setPackageValue(BigDecimal.valueOf(100));
        req.setRequestedPickupDate(LocalDate.now());
        req.setRequestedDeliveryDate(LocalDate.now().plusDays(1));
        req.setBasePrice(BigDecimal.valueOf(20));
        return req;
    }
}