package com.shipmate.integration.auth;

import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.dto.AuthResponse;
import com.shipmate.dto.LoginRequest;
import com.shipmate.model.refreshToken.RefreshToken;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.repository.auth.RefreshTokenRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.AuthService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class AuthServiceIT extends AbstractIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User createVerifiedUser() {
        User user = User.builder()
                .email("auth.user@shipmate.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("Auth")
                .lastName("User")
                .userType(UserType.SENDER)
                .role(Role.USER)
                .verified(true)
                .build();

        return userRepository.save(user);
    }

    @Test
    void login_shouldCreateAccessAndRefreshToken() {
        User user = createVerifiedUser();

        LoginRequest request = LoginRequest.builder()
                .email(user.getEmail())
                .password("password123")
                .deviceId("device-1")
                .sessionId("session-1")
                .build();

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();

        List<RefreshToken> tokens = refreshTokenRepository.findAllByUser(user);
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).isRevoked()).isFalse();
    }

    @Test
    void login_shouldRotateRefreshToken_forSameSession() {
        User user = createVerifiedUser();

        LoginRequest request = LoginRequest.builder()
                .email(user.getEmail())
                .password("password123")
                .deviceId("device-1")
                .sessionId("session-1")
                .build();

        AuthResponse firstLogin = authService.login(request);
        AuthResponse secondLogin = authService.login(request);

        List<RefreshToken> tokens = refreshTokenRepository.findAllByUser(user);

        assertThat(tokens).hasSize(2);
        assertThat(tokens)
                .filteredOn(RefreshToken::isRevoked)
                .hasSize(1);

        assertThat(firstLogin.getRefreshToken())
                .isNotEqualTo(secondLogin.getRefreshToken());
    }

    @Test
    void refresh_shouldRotateRefreshToken() {
        User user = createVerifiedUser();

        LoginRequest login = LoginRequest.builder()
                .email(user.getEmail())
                .password("password123")
                .deviceId("device-1")
                .sessionId("session-1")
                .build();

        AuthResponse loginResponse = authService.login(login);

        AuthResponse refreshResponse =
                authService.refresh(loginResponse.getRefreshToken());

        assertThat(refreshResponse.getAccessToken()).isNotBlank();
        assertThat(refreshResponse.getRefreshToken()).isNotBlank();
        assertThat(refreshResponse.getRefreshToken())
                .isNotEqualTo(loginResponse.getRefreshToken());

        List<RefreshToken> tokens = refreshTokenRepository.findAllByUser(user);
        assertThat(tokens)
                .filteredOn(RefreshToken::isRevoked)
                .hasSize(1);
    }

    @Test
    void logout_shouldRevokeRefreshToken() {
        User user = createVerifiedUser();

        LoginRequest request = LoginRequest.builder()
                .email(user.getEmail())
                .password("password123")
                .deviceId("device-1")
                .sessionId("session-1")
                .build();

        AuthResponse response = authService.login(request);

        authService.logout(response.getRefreshToken());

        RefreshToken token = refreshTokenRepository
                .findByToken(response.getRefreshToken())
                .orElseThrow();

        assertThat(token.isRevoked()).isTrue();
    }
}
