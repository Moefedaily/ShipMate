package com.shipmate.unit.service.auth;

import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.dto.request.auth.LoginRequest;
import com.shipmate.dto.response.auth.AuthResponse;
import com.shipmate.model.refreshToken.RefreshToken;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.repository.auth.RefreshTokenRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.auth.AuthService;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@Slf4j
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
                .email("auth-" + UUID.randomUUID() + "@shipmate.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("Auth")
                .lastName("User")
                .userType(UserType.SENDER)
                .role(Role.USER)
                .verified(true)
                .active(true)
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
        log.info("First login response: {}", firstLogin);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        AuthResponse secondLogin = authService.login(request);

        List<RefreshToken> tokens = refreshTokenRepository.findAllByUser(user);
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).isRevoked()).isFalse();

        assertThat(secondLogin.getRefreshToken()).isNotBlank();
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
        String oldRefreshToken = loginResponse.getRefreshToken();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        AuthResponse refreshResponse = authService.refresh(oldRefreshToken);

        assertThat(refreshResponse.getAccessToken()).isNotBlank();
        assertThat(refreshResponse.getRefreshToken()).isNotBlank();

        // updated in place
        List<RefreshToken> tokens = refreshTokenRepository.findAllByUser(user);
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).isRevoked()).isFalse();
        
        assertThat(tokens.get(0).getToken()).isEqualTo(refreshResponse.getRefreshToken());
        
        // Old token should no longer work
        assertThatThrownBy(() -> authService.refresh(oldRefreshToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid refresh token");
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
