package com.shipmate.exception;

import org.apache.coyote.BadRequestException;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.shipmate.dto.globalPresenter.ErrorResponse;

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
        public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return buildError(
                HttpStatus.BAD_REQUEST,
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

    // ===================== NOT FOUND =====================

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound() {

        return buildError(
                HttpStatus.NOT_FOUND,
                "Resource not found"
        );
    }

    // ===================== BAD REQUEST =====================

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest() {

        return buildError(
                HttpStatus.BAD_REQUEST,
                "Bad request"
        );
    }
    

    // ===================== ACCESS DENIED =====================
    @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDenied() {
        return buildError(
                HttpStatus.FORBIDDEN,
                "Access is denied"
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
