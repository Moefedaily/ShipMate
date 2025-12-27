package com.shipmate.integration.auth;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.dto.request.auth.LoginRequest;
import com.shipmate.dto.request.auth.RegisterRequest;
import com.shipmate.dto.response.auth.AuthResponse;
import com.shipmate.model.auth.VerificationToken;
import com.shipmate.model.auth.VerificationTokenType;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.AuthService;
import com.shipmate.service.VerificationTokenService;

class AuthFlowIT extends AbstractIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenService verificationTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // --------------------------------------------------
    // Register → Verify Email → Login
    // --------------------------------------------------
    @Test
    void shouldAllowLogin_onlyAfterEmailVerification() {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email("flow@shipmate.com")
                .password("Password123!")
                .firstName("Flow")
                .lastName("Test")
                .userType(UserType.SENDER)
                .build();

        authService.register(registerRequest);

        User user = userRepository.findByEmail(registerRequest.getEmail())
                .orElseThrow();

        // Login BEFORE verification should fail
        LoginRequest loginRequest = LoginRequest.builder()
                .email(user.getEmail())
                .password("Password123!")
                .deviceId("device-1")
                .sessionId("session-1")
                .build();

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(Exception.class);

        // Verify email
        VerificationToken token = verificationTokenService.createToken(
                user,
                VerificationTokenType.EMAIL_VERIFICATION,
                Duration.ofHours(24)
        );
        authService.verifyEmail(token.getToken());

        // Login AFTER verification should succeed
        AuthResponse response = authService.login(loginRequest);
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
    }

    // --------------------------------------------------
    // Login → Refresh → Logout → Refresh fails
    // --------------------------------------------------
    @Test
    void shouldInvalidateSession_afterLogout() {
        User user = userRepository.save(
                User.builder()
                        .email("session@shipmate.com")
                        .password(passwordEncoder.encode("Password123!"))
                        .firstName("Session")
                        .lastName("Test")
                        .role(Role.USER)
                        .userType(UserType.SENDER)
                        .verified(true)
                        .active(true)
                        .build()
        );

        LoginRequest loginRequest = LoginRequest.builder()
                .email(user.getEmail())
                .password("Password123!")
                .deviceId("device-1")
                .sessionId("session-1")
                .build();

        // Login and get tokens
        AuthResponse loginResponse = authService.login(loginRequest);
        String refreshToken = loginResponse.getRefreshToken();

        // First refresh should work
        AuthResponse refreshResponse = authService.refresh(refreshToken);
        assertThat(refreshResponse.getAccessToken()).isNotBlank();

        // Logout
        authService.logout(refreshToken);

        // Second refresh after logout should fail
        assertThatThrownBy(() -> authService.refresh(refreshToken))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("revoked");
    }
}