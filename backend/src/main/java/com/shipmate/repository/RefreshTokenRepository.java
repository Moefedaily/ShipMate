package com.shipmate.repository;

import com.shipmate.model.refreshToken.RefreshToken;
import com.shipmate.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Used during refresh flow
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Used during login to rotate or reuse session token
     */
    Optional<RefreshToken> findByUserAndDeviceIdAndSessionId(
            User user,
            String deviceId,
            String sessionId
    );

    /**
     * Active (non-revoked) refresh token for a session
     */
    Optional<RefreshToken> findByUserAndDeviceIdAndSessionIdAndRevokedFalse(
            User user,
            String deviceId,
            String sessionId
    );

    /**
     * Logout current session
     */
    void deleteByUserAndDeviceIdAndSessionId(
            User user,
            String deviceId,
            String sessionId
    );

    /**
     * Logout all sessions on a device
     */
    void deleteByUserAndDeviceId(User user, String deviceId);

    /**
     * Logout all sessions for user
     */
    void deleteByUser(User user);

}
