package com.shipmate.exception;

import com.shipmate.dto.ErrorResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ===================== AUTHENTICATION =====================

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials() {
        return buildError(
                HttpStatus.UNAUTHORIZED,
                "Invalid credentials"
        );
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabledAccount() {
        return buildError(
                HttpStatus.FORBIDDEN,
                "Account is disabled"
        );
    }

    // ===================== BUSINESS ERRORS =====================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex) {

        return buildError(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex) {

        return buildError(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage()
        );
    }

    // ===================== VALIDATION =====================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError() {

        return buildError(
                HttpStatus.BAD_REQUEST,
                "Invalid request data"
        );
    }

    // ===================== FALLBACK =====================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException() {

        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected error occurred"
        );
    }

    // ===================== HELPER =====================

    private ResponseEntity<ErrorResponse> buildError(
            HttpStatus status,
            String message) {

        ErrorResponse error = ErrorResponse.builder()
                .status(status.value())
                .message(message)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(status).body(error);
    }
}
