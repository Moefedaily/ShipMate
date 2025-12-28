package com.shipmate.api.profile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.dto.request.user.UpdateUserProfileRequest;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.repository.user.UserRepository;

class UserProfileControllerIT extends AbstractIntegrationTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void getMe_shouldReturnProfile_whenAuthenticated() throws Exception {
        User user = createUser("me@shipmate.com");
        String token = obtainAccessToken(user.getEmail(), "Password123!");
        
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(user.getEmail()))
            .andExpect(jsonPath("$.firstName").value("Test"));
    }
    
    @Test
    void updateMe_shouldUpdateNames() throws Exception {
        User user = createUser("update@shipmate.com");
        String token = obtainAccessToken(user.getEmail(), "Password123!");
        
        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .firstName("Updated")
                .lastName("Name")
                .phone("+33744857775")
                .build();
        
        mockMvc.perform(put("/api/users/me")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    }
    
    @Test
    void getMe_shouldFail_whenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isForbidden());
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