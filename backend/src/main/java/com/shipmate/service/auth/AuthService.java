package com.shipmate.service.auth;

import com.shipmate.dto.request.auth.*;
import com.shipmate.dto.response.auth.*;
import com.shipmate.mapper.UserMapper;
import com.shipmate.model.auth.VerificationTokenType;
import com.shipmate.model.refreshToken.RefreshToken;
import com.shipmate.model.user.User;
import com.shipmate.repository.auth.RefreshTokenRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.security.JwtUtil;
import com.shipmate.service.mail.MailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
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
    private final MailService mailService;
    private final VerificationTokenService verificationTokenService;

    // ===================== REGISTER =====================

    public RegisterResponse register(RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setVerified(false);

        User savedUser = userRepository.save(user);

        var token = verificationTokenService.createToken(
                savedUser,
                VerificationTokenType.EMAIL_VERIFICATION,
                Duration.ofHours(24)
        );

        mailService.sendVerificationEmail(savedUser.getEmail(), token.getToken());

        return RegisterResponse.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .message("Registration successful. Please verify your email.")
                .build();
    }

    // ===================== LOGIN =====================

    public AuthResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        String accessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        RefreshToken refreshToken = refreshTokenRepository
                .findByUserAndDeviceIdAndSessionId(
                        user,
                        request.getDeviceId(),
                        request.getSessionId()
                )
                .orElse(null);

        String refreshValue = jwtUtil.generateRefreshToken(user.getId());

        if (refreshToken != null) {
            refreshToken.setToken(refreshValue);
            refreshToken.setExpiresAt(Instant.now().plusSeconds(jwtUtil.getRefreshTokenTtl()));
            refreshToken.setRevoked(false);
        } else {
            refreshToken = RefreshToken.builder()
                    .user(user)
                    .deviceId(request.getDeviceId())
                    .sessionId(request.getSessionId())
                    .token(refreshValue)
                    .expiresAt(Instant.now().plusSeconds(jwtUtil.getRefreshTokenTtl()))
                    .revoked(false)
                    .build();
        }

        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshValue) // controller hides this
                .build();
    }

    // ===================== REFRESH =====================

    public AuthResponse refresh(String refreshTokenValue) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new IllegalStateException("Refresh token revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalStateException("Refresh token expired");
        }

        User user = refreshToken.getUser();

        String newRefreshValue = jwtUtil.generateRefreshToken(user.getId());

        refreshToken.setToken(newRefreshValue);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(jwtUtil.getRefreshTokenTtl()));
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);

        String newAccessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshValue)
                .build();
    }

    // ===================== LOGOUT =====================

    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    // ===================== EMAIL / PASSWORD =====================

    public VerifyEmailResponse verifyEmail(String tokenValue) {
        var token = verificationTokenService.validateToken(tokenValue, VerificationTokenType.EMAIL_VERIFICATION);
        var user = token.getUser();

        user.setVerified(true);
        userRepository.save(user);
        verificationTokenService.markAsUsed(token);

        return VerifyEmailResponse.builder()
                .message("Email verified successfully.")
                .build();
    }

    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {

        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            var token = verificationTokenService.createToken(
                    user,
                    VerificationTokenType.PASSWORD_RESET,
                    Duration.ofHours(1)
            );
            mailService.sendPasswordResetEmail(user.getEmail(), token.getToken());
        });

        return ForgotPasswordResponse.builder()
                .message("If an account exists, a password reset email has been sent.")
                .build();
    }

    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {

        var token = verificationTokenService.validateToken(
                request.getToken(),
                VerificationTokenType.PASSWORD_RESET
        );

        var user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        verificationTokenService.markAsUsed(token);

        refreshTokenRepository.revokeAllActiveByUserId(user.getId());

        return ResetPasswordResponse.builder()
                .message("Password updated successfully. Please log in again.")
                .build();
    }
}
