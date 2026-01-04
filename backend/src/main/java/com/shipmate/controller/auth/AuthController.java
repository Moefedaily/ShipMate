package com.shipmate.controller.auth;

import com.shipmate.dto.request.auth.*;
import com.shipmate.dto.response.auth.*;
import com.shipmate.security.JwtUtil;
import com.shipmate.service.auth.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and session management APIs")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    // ===================== REGISTER =====================

    @Operation(
        summary = "Register a new user",
        description = "Creates a new user account. Does not authenticate the user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input or email already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    // ===================== LOGIN =====================

    @Operation(
        summary = "Login",
        description = "Authenticates user credentials and creates a new session."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthResponse auth = authService.login(request);

        setRefreshCookie(response, auth.getRefreshToken());

        // never expose refresh token to frontend
        return ResponseEntity.ok(
                AuthResponse.builder()
                        .accessToken(auth.getAccessToken())
                        .build()
        );
    }

    // ===================== REFRESH =====================

    @Operation(
        summary = "Refresh access token",
        description = "Rotates the refresh token and returns a new access token."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AuthResponse auth = authService.refresh(refreshToken);

        // rotate cookie
        setRefreshCookie(response, auth.getRefreshToken());

        return ResponseEntity.ok(
                AuthResponse.builder()
                        .accessToken(auth.getAccessToken())
                        .build()
        );
    }

    // ===================== LOGOUT =====================

    @Operation(
        summary = "Logout",
        description = "Revokes the refresh token and closes the current session."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Logout successful")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        clearRefreshCookie(response);

        return ResponseEntity.noContent().build();
    }

    // ===================== VERIFY EMAIL =====================

    @Operation(
        summary = "Verify email",
        description = "Marks the user as verified using a verification token."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Email verified successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    @GetMapping("/verify-email")
    public ResponseEntity<VerifyEmailResponse> verifyEmail(
            @RequestParam("token") String token) {

        return ResponseEntity.ok(authService.verifyEmail(token));
    }

    // ===================== FORGOT PASSWORD =====================

    @Operation(
        summary = "Forgot password",
        description = "Sends a password reset email if the account exists."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Request accepted"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    // ===================== RESET PASSWORD =====================

    @Operation(
        summary = "Reset password",
        description = "Resets the user's password using a reset token."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password reset successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid token / invalid request")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<ResetPasswordResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        return ResponseEntity.ok(authService.resetPassword(request));
    }

    // ===================== COOKIE HELPERS =====================

    private void setRefreshCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", token)
                .httpOnly(true)
                .secure(false) // true in prod (HTTPS)
                .sameSite("Lax")
                .path("/api/auth/refresh")
                .maxAge(jwtUtil.getRefreshTokenTtl())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/api/auth/refresh")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
