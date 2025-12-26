package com.shipmate.service;

import com.shipmate.dto.AuthResponse;
import com.shipmate.dto.ForgotPasswordRequest;
import com.shipmate.dto.ForgotPasswordResponse;
import com.shipmate.dto.LoginRequest;
import com.shipmate.dto.RegisterRequest;
import com.shipmate.dto.RegisterResponse;
import com.shipmate.dto.ResetPasswordRequest;
import com.shipmate.dto.ResetPasswordResponse;
import com.shipmate.dto.VerifyEmailResponse;
import com.shipmate.mapper.UserMapper;
import com.shipmate.model.auth.VerificationToken;
import com.shipmate.model.auth.VerificationTokenType;
import com.shipmate.model.refreshToken.RefreshToken;
import com.shipmate.model.user.User;
import com.shipmate.repository.auth.RefreshTokenRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.security.JwtUtil;
import com.shipmate.service.mail.EmailService;

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
    private final EmailService mailService;
    private final VerificationTokenService verificationTokenService;
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
        user.setVerified(false);

        // Save user
        User savedUser = userRepository.save(user);

        // Create email verification token
        VerificationToken token =
                verificationTokenService.createToken(
                        savedUser,
                        VerificationTokenType.EMAIL_VERIFICATION,
                        Duration.ofHours(24)
                );

        // Send verification email
        mailService.sendVerificationEmail(
                savedUser.getEmail(),
                token.getToken()
        );

        return RegisterResponse.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .message("Registration successful. Please verify your email.")
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
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        // Generate new access token
        String accessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        // Find existing refresh token for this session (ANY state)
        RefreshToken refreshToken = refreshTokenRepository
                .findByUserAndDeviceIdAndSessionId(
                        user,
                        request.getDeviceId(),
                        request.getSessionId()
                )
                .orElse(null);

        String newRefreshTokenValue = jwtUtil.generateRefreshToken(user.getId());

        if (refreshToken != null) {
            // Rotate existing token IN PLACE
            refreshToken.setToken(newRefreshTokenValue);
            refreshToken.setExpiresAt(
                    Instant.now().plusSeconds(jwtUtil.getRefreshTokenTtl())
            );
            refreshToken.setRevoked(false);
        } else {
            // First login for this session so create row
            refreshToken = RefreshToken.builder()
                    .user(user)
                    .deviceId(request.getDeviceId())
                    .sessionId(request.getSessionId())
                    .token(newRefreshTokenValue)
                    .expiresAt(
                            Instant.now().plusSeconds(jwtUtil.getRefreshTokenTtl())
                    )
                    .revoked(false)
                    .build();
        }

        refreshTokenRepository.save(refreshToken);

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
    
    public VerifyEmailResponse verifyEmail(String tokenValue) {
        log.info("Verifying email with token");

        var token = verificationTokenService.validateToken(tokenValue, VerificationTokenType.EMAIL_VERIFICATION);
        var user = token.getUser();

        user.setVerified(true);
        userRepository.save(user);

        verificationTokenService.markAsUsed(token);

        return VerifyEmailResponse.builder()
                .message("Email verified successfully. You can now log in.")
                .build();
    }

    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        log.info("Forgot password request for {}", request.getEmail());

        // SECURITY: don’t reveal if email exists
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
        log.info("Resetting password using token");

        var token = verificationTokenService.validateToken(request.getToken(), VerificationTokenType.PASSWORD_RESET);
        var user = token.getUser();

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        verificationTokenService.markAsUsed(token);

        // Optional but recommended (enterprise): revoke all active refresh tokens for this user
        refreshTokenRepository.revokeAllActiveByUserId(user.getId());

        return ResetPasswordResponse.builder()
                .message("Password updated successfully. Please log in again.")
                .build();
    }

}
