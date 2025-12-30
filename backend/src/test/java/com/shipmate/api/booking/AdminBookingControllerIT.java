package com.shipmate.api.booking;

import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.repository.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminBookingControllerIT extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String PASSWORD = "Password123!";

    // ===================== GET ALL BOOKINGS =====================

    @Test
    void admin_shouldGetAllBookings() throws Exception {
        User admin = createUser(Role.ADMIN);
        String adminToken = obtainAccessToken(admin.getEmail(), PASSWORD);

        mockMvc.perform(get("/api/admin/bookings")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ===================== GET BOOKING BY ID =====================

    @Test
    void admin_shouldFail_whenBookingNotFound() throws Exception {
        User admin = createUser(Role.ADMIN);
        String adminToken = obtainAccessToken(admin.getEmail(), PASSWORD);

        mockMvc.perform(get("/api/admin/bookings/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    // ===================== SECURITY =====================

    @Test
    void nonAdmin_shouldBeForbidden() throws Exception {
        User user = createUser(Role.USER);
        String token = obtainAccessToken(user.getEmail(), PASSWORD);

        mockMvc.perform(get("/api/admin/bookings")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ===================== HELPER =====================

    private User createUser(Role role) {
        return userRepository.save(
                User.builder()
                        .email(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@shipmate.com")
                        .password(passwordEncoder.encode(PASSWORD))
                        .firstName("Test")
                        .lastName("User")
                        .role(role)
                        .userType(UserType.DRIVER)
                        .verified(true)
                        .active(true)
                        .build()
        );
    }
}
