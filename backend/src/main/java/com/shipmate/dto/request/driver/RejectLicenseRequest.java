package com.shipmate.dto.request.driver;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectLicenseRequest {

    @NotBlank(message = "Rejection reason is required")
    private String reason;
}
