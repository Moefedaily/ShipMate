package com.shipmate.integration.auth;

import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.model.refreshToken.RefreshToken;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.repository.auth.RefreshTokenRepository;
import com.shipmate.repository.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class RefreshTokenRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    // Generate unique email per test to avoid duplicates
    private User createUser() {
        User user = User.builder()
                .email("test-" + UUID.randomUUID() + "@shipmate.com")
                .password("hashed-password")
                .firstName("Test")
                .lastName("User")
                .userType(UserType.SENDER) 
                .role(Role.USER)
                .verified(true)
                .build();

        return userRepository.save(user);
    }

    @Test
    void shouldPersistRefreshToken() {
        User user = createUser();

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .deviceId("device-1")
                .sessionId("session-1")
                .token(UUID.randomUUID().toString())
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        RefreshToken saved = refreshTokenRepository.save(token);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.isRevoked()).isFalse();
    }

    @Test
    void shouldFindByToken() {
        User user = createUser();

        String tokenValue = UUID.randomUUID().toString();

        refreshTokenRepository.save(
                RefreshToken.builder()
                        .user(user)
                        .deviceId("device-1")
                        .sessionId("session-1")
                        .token(tokenValue)
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .revoked(false)
                        .build()
        );

        Optional<RefreshToken> found = refreshTokenRepository.findByToken(tokenValue);

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void shouldEnforceUniqueUserDeviceSession() {
        User user = createUser();

        refreshTokenRepository.save(
                RefreshToken.builder()
                        .user(user)
                        .deviceId("device-1")
                        .sessionId("session-1")
                        .token(UUID.randomUUID().toString())
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .revoked(false)
                        .build()
        );

        RefreshToken duplicate = RefreshToken.builder()
                .user(user)
                .deviceId("device-1")
                .sessionId("session-1")
                .token(UUID.randomUUID().toString())
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        assertThatThrownBy(() -> refreshTokenRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRevokeRefreshToken() {
        User user = createUser();

        RefreshToken token = refreshTokenRepository.save(
                RefreshToken.builder()
                        .user(user)
                        .deviceId("device-1")
                        .sessionId("session-1")
                        .token(UUID.randomUUID().toString())
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .revoked(false)
                        .build()
        );

        token.setRevoked(true);
        refreshTokenRepository.save(token);

        RefreshToken updated = refreshTokenRepository.findById(token.getId()).orElseThrow();

        assertThat(updated.isRevoked()).isTrue();
    }
}