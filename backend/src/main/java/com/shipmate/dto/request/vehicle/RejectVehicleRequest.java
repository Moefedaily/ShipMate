package com.shipmate.dto.request.vehicle;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectVehicleRequest {

    @NotBlank(message = "Rejection reason is required")
    private String reason;
}
