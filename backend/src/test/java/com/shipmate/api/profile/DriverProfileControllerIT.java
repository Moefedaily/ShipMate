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


class DriverProfileControllerIT extends AbstractIntegrationTest {
    
    @Autowired
    private UserRepository userRepository;
    
    
    @Test
    void apply_shouldCreateDriverProfile() throws Exception {
        User user = createUser("driver@shipmate.com");
        String token = obtainAccessToken(user.getEmail(), "Password123!");
        
        DriverApplyRequest request = DriverApplyRequest.builder()
                .licenseNumber("API-123")
                .vehicleType(VehicleType.CAR)
                .maxWeightCapacity(BigDecimal.valueOf(100))
                .build();
        
        mockMvc.perform(post("/api/drivers/apply")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"));
    }
    
    @Test
    void getMyDriverProfile_shouldReturnProfile() throws Exception {

        User user = createUser("DRIVER-" + UUID.randomUUID() + "@shipmate.com");

        String token = obtainAccessToken(user.getEmail(), "Password123!");
        
        applyDriver(user, token);
        
        mockMvc.perform(get("/api/drivers/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.licenseNumber").exists());
    }
    
    private void applyDriver(User user, String token) throws Exception {
        DriverApplyRequest request = DriverApplyRequest.builder()
                .licenseNumber("API-456")
                .vehicleType(VehicleType.VAN)
                .maxWeightCapacity(BigDecimal.valueOf(200))
                .build();
        
        mockMvc.perform(post("/api/drivers/apply")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
    }
    
    private User createUser(String email) {
        return userRepository.save(
                User.builder()
                        .email(email)
                        .password(passwordEncoder.encode("Password123!"))
                        .firstName("Driver")
                        .lastName("User")
                        .role(Role.USER)
                        .userType(UserType.DRIVER)
                        .verified(true)
                        .active(true)
                        .build()
        );
    }
}