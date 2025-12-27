package com.shipmate.integration.auth;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.dto.request.auth.LoginRequest;
import com.shipmate.model.auth.VerificationToken;
import com.shipmate.model.auth.VerificationTokenType;
import com.shipmate.model.refreshToken.RefreshToken;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.repository.auth.RefreshTokenRepository;
import com.shipmate.repository.auth.VerificationTokenRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.AuthService;
import com.shipmate.service.VerificationTokenService;

class AuthPersistenceIT extends AbstractIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private VerificationTokenService verificationTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // --------------------------------------------------
    // Refresh token is persisted on login
    // --------------------------------------------------
  @Test
    void shouldPersistRefreshToken_onLogin() {
        User user = createVerifiedUser("login-" + UUID.randomUUID() + "@shipmate.com");

        LoginRequest request = LoginRequest.builder()
                .email(user.getEmail())
                .password("Password123!")
                .deviceId("device-1")
                .sessionId("session-1")
                .build();

        authService.login(request);

        // Fetch tokens for this specific user to avoid lazy loading issues
        RefreshToken token = refreshTokenRepository
            .findByUserAndDeviceIdAndSessionId(
                    user,
                    "device-1",
                    "session-1"
            )
            .orElseThrow();


        assertThat(token.getUser().getId()).isEqualTo(user.getId());
        assertThat(token.isRevoked()).isFalse();
        assertThat(token.getExpiresAt()).isAfter(Instant.now());
    }

    // --------------------------------------------------
    // Refresh token is revoked on logout
    // --------------------------------------------------
    @Test
    void shouldRevokeRefreshToken_onLogout() {
        User user = createVerifiedUser("logout-" + UUID.randomUUID() + "@shipmate.com");

        LoginRequest request = LoginRequest.builder()
                .email(user.getEmail())
                .password("Password123!")
                .deviceId("device-1")
                .sessionId("session-1")
                .build();

        var authResponse = authService.login(request);
        String refreshTokenValue = authResponse.getRefreshToken();

        authService.logout(refreshTokenValue);

        RefreshToken token = refreshTokenRepository
                .findByToken(refreshTokenValue)
                .orElseThrow();

        assertThat(token.isRevoked()).isTrue();
    }

    // --------------------------------------------------
    // Email verification updates user state
    // --------------------------------------------------
    @Test
    void shouldMarkUserVerified_onEmailVerification() {
        User user = createUnverifiedUser("verify-" + UUID.randomUUID() + "@shipmate.com");

        VerificationToken token = verificationTokenService.createToken(
                user,
                VerificationTokenType.EMAIL_VERIFICATION,
                Duration.ofHours(24)
        );

        authService.verifyEmail(token.getToken());

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        VerificationToken updatedToken = verificationTokenRepository
                .findByToken(token.getToken())
                .orElseThrow();

        assertThat(updatedUser.isVerified()).isTrue();
        assertThat(updatedToken.isUsed()).isTrue();
    }

    // --------------------------------------------------
    // Helpers
    // --------------------------------------------------
    private User createVerifiedUser(String email) {
        return userRepository.save(
                User.builder()
                        .email(email)
                        .password(passwordEncoder.encode("Password123!"))
                        .firstName("Test")
                        .lastName("User")
                        .role(Role.USER)
                        .userType(UserType.SENDER)
                        .verified(true)
                        .active(true)
                        .build()
        );
    }

    private User createUnverifiedUser(String email) {
        return userRepository.save(
                User.builder()
                        .email(email)
                        .password(passwordEncoder.encode("Password123!"))
                        .firstName("Test")
                        .lastName("User")
                        .role(Role.USER)
                        .userType(UserType.SENDER)
                        .verified(false)
                        .active(true)
                        .build()
        );
    }
}