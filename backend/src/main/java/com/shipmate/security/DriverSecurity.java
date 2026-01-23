package com.shipmate.security;

import com.shipmate.model.DriverProfile.DriverStatus;
import com.shipmate.repository.driver.DriverProfileRepository;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("driverSecurity")
@RequiredArgsConstructor
public class DriverSecurity {

    private final DriverProfileRepository driverProfileRepository;

    /**
     * True if user has an APPROVED driver profile
     */
    public boolean isApprovedDriver(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());

        return driverProfileRepository
                .findByUser_Id(userId)
                .map(profile -> profile.getStatus() == DriverStatus.APPROVED)
                .orElse(false);
    }
}
