package com.shipmate.service;

import com.shipmate.dto.AuthResponse;
import com.shipmate.dto.LoginRequest;
import com.shipmate.dto.RegisterRequest;
import com.shipmate.dto.RegisterResponse;
import com.shipmate.mapper.UserMapper;
import com.shipmate.model.refreshToken.RefreshToken;
import com.shipmate.model.user.User;
import com.shipmate.repository.RefreshTokenRepository;
import com.shipmate.repository.UserRepository;
import com.shipmate.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public RegisterResponse register(RegisterRequest request) {
        log.info("Registering user with email {}", request.getEmail());

        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Map RegisterRequest → User entity
        User user = userMapper.toEntity(request);

        // Encode password
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        //  Save user
        User savedUser = userRepository.save(user);

        // Return registration response (NO TOKENS)
        return RegisterResponse.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .message("Registration successful. Please log in.")
                .build();
    }


    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email {}", request.getEmail());

        // Authenticate credentials (Spring Security)
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
            )
        );

        // Load user (business layer, not security layer)
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        // Generate new access token
        String accessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        //  Rotate or create refresh token for this session
        refreshTokenRepository.findByUserAndDeviceIdAndSessionIdAndRevokedFalse( user,request.getDeviceId(),request.getSessionId())
            .ifPresent(existingToken -> {
                existingToken.setRevoked(true);
                refreshTokenRepository.save(existingToken);
            });


        // Create new refresh token
        String newRefreshTokenValue = jwtUtil.generateRefreshToken(user.getId());

        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .deviceId(request.getDeviceId())
                .sessionId(request.getSessionId())
                .token(newRefreshTokenValue)
                .expiresAt(Instant.now().plusSeconds(jwtUtil.getRefreshTokenTtl()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(newRefreshToken);

        // Return tokens
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshTokenValue)
                .build();
    }

   public AuthResponse refresh(String refreshTokenValue) {
        log.info("Refreshing access token");

        // Find refresh token in DB
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        // Validate token
        if (refreshToken.isRevoked()) {
            throw new IllegalStateException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalStateException("Refresh token has expired");
        }

        User user = refreshToken.getUser();

        // Rotate refresh token IN PLACE
        String newRefreshTokenValue = jwtUtil.generateRefreshToken(user.getId());

        refreshToken.setToken(newRefreshTokenValue);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(jwtUtil.getRefreshTokenTtl()));
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);

        //  Generate new access token
        String newAccessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        // Return response
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenValue)
                .build();
    }

    public void logout(String refreshTokenValue) {
        log.info("Logout requested");

        refreshTokenRepository.findByToken(refreshTokenValue)
                .ifPresent(refreshToken -> {
                    refreshToken.setRevoked(true);
                    refreshTokenRepository.save(refreshToken);
                });
    }

}
