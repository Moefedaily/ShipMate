package com.shipmate.dto.response.driver;

import java.util.UUID;

import com.shipmate.model.user.VehicleType;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignedDriverResponse {
    private UUID id;
    private String firstName;
    private String avatarUrl;
    private VehicleType vehicleType;
}
