package com.shipmate.integration.auth;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
import com.shipmate.service.auth.AuthService;

class PasswordResetServiceIT extends AbstractIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenRepository tokenRepository;

    @Test
    void forgotPassword_shouldCreatePasswordResetToken() {
        User user = createUser("reset@test.com", "Password123!");

        authService.forgotPassword(
                ForgotPasswordRequest.builder()
                        .email(user.getEmail())
                        .build()
        );

        VerificationToken token = tokenRepository
                .findByUserAndTypeAndUsedFalse(user, VerificationTokenType.PASSWORD_RESET)
                .orElseThrow();

        assertThat(token.getExpiresAt()).isAfter(Instant.now());
        assertThat(token.isUsed()).isFalse();
    }

    @Test
    void resetPassword_shouldChangePassword_andInvalidateToken() {
        User user = createUser("change@test.com", "OldPassword123!");

        VerificationToken token = tokenRepository.save(
                VerificationToken.builder()
                        .user(user)
                        .token("valid-reset-token")
                        .type(VerificationTokenType.PASSWORD_RESET)
                        .expiresAt(Instant.now().plus(Duration.ofHours(1)))
                        .used(false)
                        .build()
        );

        authService.resetPassword(
                ResetPasswordRequest.builder()
                        .token(token.getToken())
                        .newPassword("NewPassword123!")
                        .build()
        );

        VerificationToken updatedToken = tokenRepository.findById(token.getId()).orElseThrow();
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();

        assertThat(updatedToken.isUsed()).isTrue();
        assertThat(updatedUser.getPassword()).isNotEqualTo("OldPassword123!");
    }

    @Test
    void resetPassword_shouldFail_whenTokenIsExpired() {
        User user = createUser("expired@test.com", "Password123!");

        VerificationToken token = tokenRepository.save(
                VerificationToken.builder()
                        .user(user)
                        .token("expired-token")
                        .type(VerificationTokenType.PASSWORD_RESET)
                        .expiresAt(Instant.now().minusSeconds(10))
                        .used(false)
                        .build()
        );

        assertThatThrownBy(() ->
                authService.resetPassword(
                        ResetPasswordRequest.builder()
                                .token(token.getToken())
                                .newPassword("NewPassword123!")
                                .build()
                )
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("expired");
    }

    private User createUser(String email, String rawPassword) {
        User user = User.builder()
                .email(email)
                .password(rawPassword)
                .firstName("Test")
                .lastName("User")
                .userType(UserType.SENDER)
                .role(Role.USER)
                .verified(true)
                .build();
        return userRepository.save(user);
    }
}
