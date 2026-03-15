package com.shipmate.dto.request.driver;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLicenseRequest {

    @NotBlank(message = "License number is required")
    private String licenseNumber;

    @NotNull(message = "License expiry date is required")
    private LocalDate licenseExpiry;
}
