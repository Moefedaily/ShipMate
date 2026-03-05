package com.shipmate.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminStrikeRequest(

        @NotBlank(message = "Strike note is required")
        @Size(max = 500, message = "Strike note must be less than 500 characters")
        String note

) {}