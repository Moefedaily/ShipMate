package com.shipmate.unit.service.profile;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import com.shipmate.dto.request.driver.DriverApplyRequest;
import com.shipmate.dto.request.driver.UpdateLicenseRequest;
import com.shipmate.model.photo.Photo;
import com.shipmate.model.user.VehicleType;
import com.shipmate.model.vehicle.Vehicle;
import com.shipmate.repository.vehicle.VehicleRepository;
import com.shipmate.service.mail.MailService;
import com.shipmate.service.photo.PhotoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.shipmate.dto.response.driver.DriverProfileResponse;
import com.shipmate.mapper.driver.DriverProfileMapper;
import com.shipmate.model.DriverProfile.DriverProfile;
import com.shipmate.model.DriverProfile.DriverStatus;
import com.shipmate.model.user.User;
import com.shipmate.repository.driver.DriverProfileRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.admin.AdminActionLogger;
import com.shipmate.service.driver.DriverProfileService;

@ExtendWith(MockitoExtension.class)
class DriverProfileServiceTest {

    @Mock private DriverProfileRepository driverProfileRepository;
    @Mock private DriverProfileMapper mapper;
    @Mock private UserRepository userRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private MailService mailService;
    @Mock private AdminActionLogger adminActionLogger;
    @Mock private PhotoService photoService;

    @InjectMocks
    private DriverProfileService driverProfileService;

    @Test
    void apply_shouldCreateDriverProfileAndVehicle_whenNotExists() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).build();
        DriverApplyRequest request = DriverApplyRequest.builder()
                .licenseNumber("LIC-123")
                .licenseExpiry(LocalDate.now().plusYears(1))
                .vehicleType(VehicleType.CAR)
                .maxWeightCapacity(java.math.BigDecimal.valueOf(500))
                .build();

        DriverProfile profile = DriverProfile.builder()
                .id(UUID.randomUUID())
                .user(user)
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
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    void updateLicense_shouldSetProfileBackToPending() {
        UUID userId = UUID.randomUUID();
        Vehicle activeVehicle = Vehicle.builder().active(true).build();
        DriverProfile profile = DriverProfile.builder()
                .user(User.builder().id(userId).build())
                .status(DriverStatus.SUSPENDED)
                .licenseNumber("OLD-LIC")
                .licenseExpiry(LocalDate.now().plusMonths(1))
                .vehicles(new ArrayList<>(List.of(activeVehicle)))
                .build();
        UpdateLicenseRequest request = UpdateLicenseRequest.builder()
                .licenseNumber("NEW-LIC")
                .licenseExpiry(LocalDate.now().plusYears(2))
                .build();

        when(driverProfileRepository.findByUser_Id(userId)).thenReturn(Optional.of(profile));
        when(driverProfileRepository.save(profile)).thenReturn(profile);
        when(mapper.toResponse(profile)).thenReturn(new DriverProfileResponse());

        driverProfileService.updateLicense(userId, request);

        assertThat(profile.getStatus()).isEqualTo(DriverStatus.PENDING);
        assertThat(profile.getLicenseNumber()).isEqualTo("OLD-LIC");
        assertThat(profile.getPendingLicenseNumber()).isEqualTo("NEW-LIC");
        assertThat(profile.getPendingLicenseExpiry()).isEqualTo(request.getLicenseExpiry());
        assertThat(activeVehicle.isActive()).isFalse();
        verify(vehicleRepository).save(activeVehicle);
    }

    @Test
    void uploadLicensePhotos_shouldReplaceExistingPhotos() {
        UUID userId = UUID.randomUUID();
        DriverProfile profile = DriverProfile.builder()
                .id(UUID.randomUUID())
                .user(User.builder().id(userId).build())
                .licensePhotos(new ArrayList<>())
                .build();
        List<MultipartFile> files = List.of(mock(MultipartFile.class));
        List<Photo> uploaded = List.of(Photo.builder().publicId("new").url("new-url").build());
        Photo existingPending = Photo.builder().publicId("old").url("old-url").build();

        when(driverProfileRepository.findByUser_Id(userId)).thenReturn(Optional.of(profile));
        when(driverProfileRepository.save(profile)).thenReturn(profile);
        when(photoService.getPendingDriverLicensePhotos(profile.getId())).thenReturn(List.of(existingPending));
        when(photoService.uploadPendingDriverLicensePhotos(profile, files)).thenReturn(uploaded);
        when(mapper.toResponse(profile)).thenReturn(new DriverProfileResponse());

        driverProfileService.uploadLicensePhotos(userId, files);

        verify(photoService).deletePhoto(existingPending);
        verify(photoService).uploadPendingDriverLicensePhotos(profile, files);
    }

    @Test
    void uploadLicensePhotos_shouldRejectEmptyFileList() {
        UUID userId = UUID.randomUUID();
        DriverProfile profile = DriverProfile.builder()
                .id(UUID.randomUUID())
                .user(User.builder().id(userId).build())
                .licensePhotos(new ArrayList<>())
                .build();

        when(driverProfileRepository.findByUser_Id(userId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> driverProfileService.uploadLicensePhotos(userId, Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one license photo");

        verifyNoInteractions(photoService);
    }

    @Test
    void approve_shouldPromotePendingLicenseData() {
        DriverProfile profile = DriverProfile.builder()
                .id(UUID.randomUUID())
                .user(User.builder().email("test@test.com").build())
                .status(DriverStatus.PENDING)
                .licenseNumber("OLD-LIC")
                .pendingLicenseNumber("NEW-LIC")
                .pendingLicenseExpiry(LocalDate.now().plusYears(1))
                .build();
        Vehicle vehicle = Vehicle.builder().active(false).build();
        Photo pendingPhoto = Photo.builder().photoType("PENDING_DRIVER_LICENSE").build();

        when(driverProfileRepository.findById(profile.getId())).thenReturn(Optional.of(profile));
        when(vehicleRepository.findByDriverProfileOrderByCreatedAtAsc(profile)).thenReturn(List.of(vehicle));
        when(photoService.getDriverLicensePhotos(profile.getId())).thenReturn(List.of());
        when(photoService.getPendingDriverLicensePhotos(profile.getId())).thenReturn(List.of(pendingPhoto));
        when(mapper.toResponse(profile)).thenReturn(new DriverProfileResponse());

        driverProfileService.approve(profile.getId());

        assertThat(profile.getLicenseNumber()).isEqualTo("NEW-LIC");
        assertThat(profile.getPendingLicenseNumber()).isNull();
        assertThat(profile.getPendingLicenseExpiry()).isNull();
        assertThat(pendingPhoto.getPhotoType()).isEqualTo("DRIVER_LICENSE");
    }

    @Test
    void approve_shouldApproveProfileAndFirstVehicle() {
        DriverProfile profile = DriverProfile.builder()
                .id(UUID.randomUUID())
                .user(User.builder().email("test@test.com").build())
                .status(DriverStatus.PENDING)
                .build();
        Vehicle vehicle = Vehicle.builder().active(false).build();

        when(driverProfileRepository.findById(profile.getId())).thenReturn(Optional.of(profile));
        when(vehicleRepository.findByDriverProfileOrderByCreatedAtAsc(profile)).thenReturn(List.of(vehicle));
        when(mapper.toResponse(profile)).thenReturn(new DriverProfileResponse());

        driverProfileService.approve(profile.getId());

        assertThat(profile.getStatus()).isEqualTo(DriverStatus.APPROVED);
        assertThat(vehicle.isActive()).isTrue();
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void checkExpiredLicenses_shouldSuspendDrivers() {
        User user = User.builder().email("driver@test.com").build();
        DriverProfile expiredProfile = DriverProfile.builder()
                .user(user)
                .licenseExpiry(LocalDate.now().minusDays(1))
                .status(DriverStatus.APPROVED)
                .vehicles(new ArrayList<>())
                .build();

        when(driverProfileRepository.findAllByLicenseExpiryBefore(any())).thenReturn(List.of(expiredProfile));

        driverProfileService.checkExpiredLicenses();

        assertThat(expiredProfile.getStatus()).isEqualTo(DriverStatus.SUSPENDED);
        verify(driverProfileRepository).save(expiredProfile);
        verify(mailService).sendDriverSuspendedEmail(user.getEmail());
    }
}
