package com.shipmate.api.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.dto.request.auth.ForgotPasswordRequest;
import com.shipmate.dto.request.auth.ResetPasswordRequest;
import com.shipmate.model.auth.VerificationToken;
import com.shipmate.model.auth.VerificationTokenType;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.repository.auth.VerificationTokenRepository;
import com.shipmate.repository.user.UserRepository;

@AutoConfigureMockMvc
class PasswordResetControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenRepository tokenRepository;

    @Test
    void forgotPassword_shouldReturnOk_evenIfUserDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                ForgotPasswordRequest.builder()
                                        .email("unknown@test.com")
                                        .build()
                        )))
                .andExpect(status().isOk());
    }

    @Test
    void resetPassword_shouldReturnOk_withValidToken() throws Exception {

        String uniqueEmail = "api-reset-" + UUID.randomUUID() + "@test.com";
        
        User user = userRepository.save(
                User.builder()
                        .email(uniqueEmail)
                        .password("oldPassword123!")
                        .firstName("Reset")
                        .lastName("User")
                        .role(Role.USER)
                        .userType(UserType.SENDER)
                        .verified(true)
                        .active(true)
                        .build()
        );

        VerificationToken token = tokenRepository.save(
                VerificationToken.builder()
                        .user(user)
                        .token("api-valid-token-" + UUID.randomUUID())
                        .type(VerificationTokenType.PASSWORD_RESET)
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .used(false)
                        .build()
        );


        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                ResetPasswordRequest.builder()
                                        .token(token.getToken())
                                        .newPassword("NewPassword123!")
                                        .build()
                        )))
                .andExpect(status().isOk());
    }

    @Test
    void resetPassword_shouldFail_withInvalidToken() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                ResetPasswordRequest.builder()
                                        .token("invalid-token")
                                        .newPassword("NewPassword123!")
                                        .build()
                        )))
                .andExpect(status().isBadRequest());
    }
}
