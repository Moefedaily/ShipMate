package com.shipmate.unit.service.profile;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shipmate.dto.request.driver.DriverApplyRequest;
import com.shipmate.dto.response.driver.DriverProfileResponse;
import com.shipmate.mapper.DriverProfileMapper;
import com.shipmate.model.DriverProfile.DriverProfile;
import com.shipmate.model.DriverProfile.DriverStatus;
import com.shipmate.model.user.User;
import com.shipmate.repository.driver.DriverProfileRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.driver.DriverProfileService;

@ExtendWith(MockitoExtension.class)
class DriverProfileServiceTest {

    @Mock
    private DriverProfileRepository driverProfileRepository;

    @Mock
    private DriverProfileMapper mapper;

    @Mock
    private UserRepository userRepository; 

    @InjectMocks
    private DriverProfileService driverProfileService;

@Test
    void apply_shouldCreateDriverProfile_whenNotExists() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).build();

        DriverApplyRequest request = DriverApplyRequest.builder().build();
        DriverProfile profile = DriverProfile.builder()
                .id(UUID.randomUUID())
                .status(DriverStatus.PENDING)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(driverProfileRepository.existsByUser(user)).thenReturn(false);
        when(mapper.toEntity(request, user)).thenReturn(profile);
        when(driverProfileRepository.save(profile)).thenReturn(profile);
        when(mapper.toResponse(profile)).thenReturn(new DriverProfileResponse());

        driverProfileService.apply(userId, request);

        verify(userRepository).findById(userId);
        verify(driverProfileRepository).existsByUser(user);
        verify(driverProfileRepository).save(profile);
    }


    @Test
    void apply_shouldFail_whenProfileAlreadyExists() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(driverProfileRepository.existsByUser(user)).thenReturn(true);

        assertThatThrownBy(() -> driverProfileService.apply(userId, new DriverApplyRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Driver profile already exists");
    }

    @Test
    void approve_shouldFail_whenNotPending() {
        DriverProfile profile = DriverProfile.builder()
                .id(UUID.randomUUID())
                .status(DriverStatus.APPROVED)
                .build();

        when(driverProfileRepository.findById(profile.getId()))
                .thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> driverProfileService.approve(profile.getId()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void suspend_shouldFail_whenNotApproved() {
        DriverProfile profile = DriverProfile.builder()
                .id(UUID.randomUUID())
                .status(DriverStatus.PENDING)
                .build();

        when(driverProfileRepository.findById(profile.getId()))
                .thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> driverProfileService.suspend(profile.getId()))
                .isInstanceOf(IllegalStateException.class);
    }
}
