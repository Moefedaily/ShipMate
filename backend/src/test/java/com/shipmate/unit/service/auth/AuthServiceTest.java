package com.shipmate.unit.service.auth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.shipmate.model.refreshToken.RefreshToken;
import com.shipmate.model.user.User;
import com.shipmate.repository.auth.RefreshTokenRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.security.JwtUtil;
import com.shipmate.service.auth.AuthService;
import com.shipmate.service.auth.VerificationTokenService;
import com.shipmate.service.mail.MailService;
import com.shipmate.dto.request.auth.RegisterRequest;
import com.shipmate.mapper.UserMapper;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private MailService mailService;

    @Mock
    private VerificationTokenService verificationTokenService;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("test@shipmate.com")
                .password("encoded-password")
                .verified(false)
                .build();
    }

    // --------------------------------------------------
    // register() rejects duplicate email
    // --------------------------------------------------
    @Test
    void register_shouldThrow_whenEmailAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@shipmate.com")
                .password("Password123!")
                .build();

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any());
        verify(mailService, never()).sendVerificationEmail(any(), any());
    }

    // --------------------------------------------------
    //register() encodes password & sends email
    // --------------------------------------------------
    @Test
    void register_shouldEncodePassword_andSendVerificationEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .email("new@shipmate.com")
                .password("Password123!")
                .build();

        User mappedUser = User.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .verified(false)
                .build();

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.empty());
        when(userMapper.toEntity(request)).thenReturn(mappedUser);
        when(passwordEncoder.encode(request.getPassword()))
                .thenReturn("encoded-password");
        when(userRepository.save(any(User.class)))
                .thenReturn(user);

        // Mock VerificationToken
        com.shipmate.model.auth.VerificationToken mockToken = 
            com.shipmate.model.auth.VerificationToken.builder()
                .token("mock-verification-token")
                .user(user)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        
        when(verificationTokenService.createToken(any(), any(), any()))
                .thenReturn(mockToken);

        authService.register(request);

        verify(passwordEncoder).encode(request.getPassword());
        verify(userRepository).save(any(User.class));
        verify(mailService).sendVerificationEmail(eq(user.getEmail()), any());
    }

    // --------------------------------------------------
    // refresh() rejects revoked token
    // --------------------------------------------------
    @Test
    void refresh_shouldReject_whenTokenIsRevoked() {
        RefreshToken refreshToken = RefreshToken.builder()
                .token("revoked-token")
                .revoked(true)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(refreshTokenRepository.findByToken("revoked-token"))
                .thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> authService.refresh("revoked-token"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("revoked");

        verify(jwtUtil, never()).generateAccessToken(any(), any(), any());
        verify(refreshTokenRepository, never()).save(any());
    }
}