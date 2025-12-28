package com.shipmate.controller.auth;

import com.shipmate.dto.request.auth.ForgotPasswordRequest;
import com.shipmate.dto.request.auth.LoginRequest;
import com.shipmate.dto.request.auth.RegisterRequest;
import com.shipmate.dto.request.auth.ResetPasswordRequest;
import com.shipmate.dto.request.auth.ResetPasswordResponse;
import com.shipmate.dto.request.auth.TokenRequest;
import com.shipmate.dto.response.auth.AuthResponse;
import com.shipmate.dto.response.auth.ForgotPasswordResponse;
import com.shipmate.dto.response.auth.RegisterResponse;
import com.shipmate.dto.response.auth.VerifyEmailResponse;
import com.shipmate.service.auth.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and session management APIs")
public class AuthController {

    private final AuthService authService;

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

        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
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
            @Valid @RequestBody TokenRequest request) {

        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
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
            @Valid @RequestBody TokenRequest request) {

        authService.logout(request.getRefreshToken());
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

        VerifyEmailResponse response = authService.verifyEmail(token);
        return ResponseEntity.ok(response);
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

        ForgotPasswordResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
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

        ResetPasswordResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

}
