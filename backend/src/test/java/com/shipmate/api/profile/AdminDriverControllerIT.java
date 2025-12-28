package com.shipmate.api.profile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.dto.request.driver.DriverApplyRequest;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.model.user.VehicleType;
import com.shipmate.repository.user.UserRepository;

class AdminDriverControllerIT extends AbstractIntegrationTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void admin_shouldApproveDriver() throws Exception {
        User admin = createUser("admin@shipmate.com", Role.ADMIN);
        User driver = createUser("pending@shipmate.com", Role.USER);
        
        String adminToken = obtainAccessToken(admin.getEmail(), "Password123!");
        String driverToken = obtainAccessToken(driver.getEmail(), "Password123!");
        
        applyDriver(driverToken);
        
        mockMvc.perform(get("/api/admin/drivers/pending")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("PENDING"));
    }
    
    @Test
    void nonAdmin_shouldBeForbidden() throws Exception {
        User user = createUser("admin-" + UUID.randomUUID() + "@shipmate.com", Role.USER);

        String token = obtainAccessToken(user.getEmail(), "Password123!");
        
    mockMvc.perform(get("/api/admin/drivers/pending")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());

    }
    
    private void applyDriver(String token) throws Exception {
        DriverApplyRequest request = DriverApplyRequest.builder()
                .licenseNumber("ADMIN-001")
                .vehicleType(VehicleType.TRUCK)
                .maxWeightCapacity(BigDecimal.valueOf(300))
                .build();
        
        mockMvc.perform(post("/api/drivers/apply")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
    }
    
    private User createUser(String email, Role role) {
        return userRepository.save(
                User.builder()
                        .email(email)
                        .password(passwordEncoder.encode("Password123!"))
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