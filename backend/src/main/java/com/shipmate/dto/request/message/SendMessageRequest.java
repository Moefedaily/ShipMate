package com.shipmate.dto.request.message;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest(
        @NotBlank String message
) {}
