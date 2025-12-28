package com.shipmate.integration.profile;


import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;

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

class DriverProfilePersistenceIT extends AbstractIntegrationTest {

    @Autowired
    private DriverProfileService driverProfileService;

    @Autowired
    private DriverProfileRepository driverProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldPersistDriverProfile_onApply() {
        User user = createUser("driver1@shipmate.com");

        DriverApplyRequest request = DriverApplyRequest.builder()
                .licenseNumber("LIC-12345")
                .vehicleType(VehicleType.CAR)
                .maxWeightCapacity(BigDecimal.valueOf(100))
                .build();

        driverProfileService.apply(user.getId(), request);

        DriverProfile profile = driverProfileRepository
                .findByUser(user)
                .orElseThrow();

        assertThat(profile.getStatus()).isEqualTo(DriverStatus.PENDING);
        assertThat(profile.getLicenseNumber()).isEqualTo("LIC-12345");
        assertThat(profile.getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void shouldFail_whenDriverProfileAlreadyExists() {
        User user = createUser("driver2@shipmate.com");

        DriverApplyRequest request = DriverApplyRequest.builder()
                .licenseNumber("LIC-67890")
                .vehicleType(VehicleType.VAN)
                .maxWeightCapacity(BigDecimal.valueOf(200))
                .build();

        driverProfileService.apply(user.getId(), request);

        assertThatThrownBy(() -> driverProfileService.apply(user.getId(), request))
                .isInstanceOf(IllegalStateException.class);
    }

    private User createUser(String email) {
        User user = User.builder()
            .email(email)
            .firstName("Test")
            .lastName("User")
            .password("test1234")
            .userType(UserType.DRIVER)
            .role(Role.USER)
            .active(true)
            .verified(true)
            .build();
        
        return userRepository.save(user);
    }
}
