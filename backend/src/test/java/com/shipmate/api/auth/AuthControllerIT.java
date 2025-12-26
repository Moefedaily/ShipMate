package com.shipmate.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.dto.LoginRequest;
import com.shipmate.dto.RegisterRequest;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.repository.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class AuthControllerIT extends AbstractIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Test
    void register_shouldCreateUser_andReturn201() throws Exception {
        String uniqueEmail = "api-" + UUID.randomUUID() + "@shipmate.com";
        
        RegisterRequest request = RegisterRequest.builder()
                .email(uniqueEmail)
                .password("StrongPassword123!")
                .firstName("API")
                .lastName("User")
                .phone("+33600000000")
                .userType(UserType.SENDER)
                .build();
        
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.email").value(uniqueEmail))
                .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    void login_shouldReturnAccessAndRefreshTokens() throws Exception {
        // Create a verified user directly in the database
        String uniqueEmail = "login-" + UUID.randomUUID() + "@shipmate.com";
        
        userRepository.save(
                User.builder()
                        .email(uniqueEmail)
                        .password(passwordEncoder.encode("StrongPassword123!"))
                        .firstName("API")
                        .lastName("User")
                        .phone("+33600000001")
                        .role(Role.USER)
                        .userType(UserType.SENDER)
                        .verified(true)
                        .active(true)
                        .build()
        );
        
        LoginRequest loginRequest = LoginRequest.builder()
                .email(uniqueEmail)
                .password("StrongPassword123!")
                .deviceId("device-api-test")
                .sessionId("session-api-test")
                .build();
        
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }
}