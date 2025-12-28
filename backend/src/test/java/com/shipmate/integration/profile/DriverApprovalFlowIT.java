package com.shipmate.integration.profile;


import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.dto.request.driver.DriverApplyRequest;
import com.shipmate.model.DriverProfile.DriverProfile;
import com.shipmate.model.DriverProfile.DriverStatus;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.model.user.VehicleType;
import com.shipmate.repository.driver.DriverProfileRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.driver.DriverProfileService;

class DriverApprovalFlowIT extends AbstractIntegrationTest {

    @Autowired
    private DriverProfileService driverProfileService;

    @Autowired
    private DriverProfileRepository driverProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void driverLifecycle_shouldFollowValidTransitions() {
        User user = createUser("flow-" + UUID.randomUUID() + "@shipmate.com");

        DriverApplyRequest request = DriverApplyRequest.builder()
                .licenseNumber("FLOW-001")
                .vehicleType(VehicleType.TRUCK)
                .maxWeightCapacity(BigDecimal.valueOf(500))
                .build();

        // User applies
        driverProfileService.apply(user.getId() , request);

        DriverProfile profile = driverProfileRepository
                .findByUser(user)
                .orElseThrow();

        assertThat(profile.getStatus()).isEqualTo(DriverStatus.PENDING);

        // Admin approves
        driverProfileService.approve(profile.getId());

        DriverProfile approved = driverProfileRepository
                .findById(profile.getId())
                .orElseThrow();

        assertThat(approved.getStatus()).isEqualTo(DriverStatus.APPROVED);
        assertThat(approved.getApprovedAt()).isNotNull();

        // Admin suspends
        driverProfileService.suspend(profile.getId());

        DriverProfile suspended = driverProfileRepository
                .findById(profile.getId())
                .orElseThrow();

        assertThat(suspended.getStatus()).isEqualTo(DriverStatus.SUSPENDED);
    }

    private User createUser(String email) {
        User user = User.builder()
            .email(email)
            .firstName("Test")
            .lastName("User")
            .password("test1234")
            .role(Role.USER)
            .userType(UserType.DRIVER)
            .active(true)
            .verified(true)
            .build();
        
        return userRepository.save(user);
    }

}
