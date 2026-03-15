package com.shipmate.api.vehicle;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.dto.request.driver.DriverApplyRequest;
import com.shipmate.dto.request.vehicle.CreateVehicleRequest;
import com.shipmate.model.DriverProfile.DriverProfile;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.model.user.VehicleType;
import com.shipmate.repository.driver.DriverProfileRepository;
import com.shipmate.repository.user.UserRepository;

class VehicleControllerIT extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DriverProfileRepository driverProfileRepository;

    @Test
    void getMyVehicles_shouldAllowDriverWithProfileEvenWhenNotApproved() throws Exception {
        User user = createDriverUser("vehicle-list-" + UUID.randomUUID() + "@shipmate.com");
        String token = obtainAccessToken(user.getEmail(), "Password123!");

        applyDriver(token);

        mockMvc.perform(get("/api/vehicles/mine")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].plateNumber").exists());
    }

    @Test
    void addVehicle_shouldReturnBusinessErrorForPendingDriverInsteadOfForbidden() throws Exception {
        User user = createDriverUser("vehicle-add-" + UUID.randomUUID() + "@shipmate.com");
        String token = obtainAccessToken(user.getEmail(), "Password123!");

        applyDriver(token);

        CreateVehicleRequest request = CreateVehicleRequest.builder()
                .vehicleType(VehicleType.VAN)
                .maxWeightCapacity(BigDecimal.valueOf(250))
                .plateNumber("ADD-" + UUID.randomUUID().toString().substring(0, 8))
                .vehicleDescription("Second vehicle")
                .insuranceExpiry(LocalDate.now().plusYears(1))
                .build();

        mockMvc.perform(post("/api/vehicles")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Your driver profile must be approved before adding additional vehicles"));
    }

    @Test
    void getMyVehicles_shouldForbidUserWithoutDriverProfile() throws Exception {
        User user = createDriverUser("no-profile-" + UUID.randomUUID() + "@shipmate.com");
        String token = obtainAccessToken(user.getEmail(), "Password123!");

        mockMvc.perform(get("/api/vehicles/mine")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    private void applyDriver(String token) throws Exception {
        DriverApplyRequest request = DriverApplyRequest.builder()
                .licenseNumber("VEH-" + UUID.randomUUID())
                .licenseExpiry(LocalDate.now().plusYears(2))
                .vehicleType(VehicleType.CAR)
                .maxWeightCapacity(BigDecimal.valueOf(100))
                .plateNumber("TEST-" + UUID.randomUUID().toString().substring(0, 8))
                .vehicleDescription("Initial vehicle")
                .build();

        mockMvc.perform(post("/api/drivers/apply")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
    }

    private User createDriverUser(String email) {
        return userRepository.save(
                User.builder()
                        .email(email)
                        .password(passwordEncoder.encode("Password123!"))
                        .firstName("Driver")
                        .lastName("User")
                        .role(Role.USER)
                        .userType(UserType.DRIVER)
                        .verified(true)
                        .active(true)
                        .build()
        );
    }
}

