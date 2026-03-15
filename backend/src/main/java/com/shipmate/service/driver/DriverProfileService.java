package com.shipmate.service.driver;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.shipmate.dto.request.driver.UpdateDriverLocationRequest;
import com.shipmate.dto.request.driver.UpdateLicenseRequest;
import com.shipmate.dto.response.driver.DriverProfileResponse;
import com.shipmate.listener.notification.NotificationRequestedEvent;
import com.shipmate.mapper.driver.DriverProfileMapper;
import com.shipmate.model.DriverProfile.DriverProfile;
import com.shipmate.model.DriverProfile.DriverStatus;
import com.shipmate.model.notification.NotificationType;
import com.shipmate.model.notification.ReferenceType;
import com.shipmate.model.user.User;
import com.shipmate.repository.driver.DriverProfileRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.admin.AdminActionLogger;
import com.shipmate.service.mail.MailService;
import com.shipmate.service.photo.PhotoService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.shipmate.dto.request.driver.DriverApplyRequest;
import com.shipmate.model.vehicle.Vehicle;
import com.shipmate.model.vehicle.VehicleStatus;
import com.shipmate.repository.vehicle.VehicleRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DriverProfileService {

    private final DriverProfileRepository driverProfileRepository;
    private final DriverProfileMapper mapper;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final MailService mailService;
    private final ApplicationEventPublisher eventPublisher;
    private final AdminActionLogger adminActionLogger;
    private final PhotoService photoService;

    public DriverProfileResponse apply(UUID userId, DriverApplyRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (driverProfileRepository.existsByUser(user)) {
            throw new IllegalStateException("Driver profile already exists");
        }

        DriverProfile profile = mapper.toEntity(request, user);
        profile.setPendingLicenseNumber(request.getLicenseNumber());
        profile.setPendingLicenseExpiry(request.getLicenseExpiry());
        profile.setLicenseNumber(null);
        profile.setLicenseExpiry(null);

        DriverProfile savedProfile = driverProfileRepository.save(profile);

        // Create initial Vehicle
        Vehicle vehicle = Vehicle.builder()
                .driverProfile(savedProfile)
                .vehicleType(request.getVehicleType())
                .maxWeightCapacity(request.getMaxWeightCapacity())
                .plateNumber(request.getPlateNumber())
                .vehicleDescription(request.getVehicleDescription())
                .status(VehicleStatus.PENDING)
                .active(false)
                .build();
        vehicleRepository.save(vehicle);

        return mapper.toResponse(savedProfile);
    }

    public DriverProfileResponse applyWithLicensePhotos(UUID userId, DriverApplyRequest request, List<MultipartFile> files) {
        apply(userId, request);
        uploadLicensePhotos(userId, files);
        return getMyProfile(userId);
    }

    public DriverProfileResponse updateLicense(UUID userId, UpdateLicenseRequest request) {
        DriverProfile profile = driverProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new IllegalArgumentException("Driver profile not found"));

        profile.setPendingLicenseNumber(request.getLicenseNumber());
        profile.setPendingLicenseExpiry(request.getLicenseExpiry());
        profile.setStatus(DriverStatus.PENDING);
        profile.setApprovedAt(null);

        Vehicle activeVehicle = profile.getActiveVehicle();
        if (activeVehicle != null) {
            activeVehicle.setActive(false);
            vehicleRepository.save(activeVehicle);
        }

        return toDriverViewResponse(driverProfileRepository.save(profile));
    }

    public DriverProfileResponse uploadLicensePhotos(UUID userId, List<MultipartFile> files) {
        DriverProfile profile = driverProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new IllegalArgumentException("Driver profile not found"));

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one license photo is required");
        }

        photoService.getPendingDriverLicensePhotos(profile.getId()).forEach(photoService::deletePhoto);
        photoService.uploadPendingDriverLicensePhotos(profile, files);
        return toDriverViewResponse(driverProfileRepository.save(profile));
    }

    public DriverProfileResponse getMyProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        DriverProfile profile = driverProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Driver profile not found"));
        return toDriverViewResponse(profile);
    }

    public DriverProfileResponse getDriver(UUID driverProfileId) {
        return toAdminReviewResponse(getProfileOrThrow(driverProfileId));
    }


    public List<DriverProfileResponse> getPendingDrivers() {
        return driverProfileRepository.findByStatus(DriverStatus.PENDING)
                .stream()
                .map(this::toAdminReviewResponse)
                .toList();
    }

    public DriverProfileResponse approve(UUID driverProfileId) {
        DriverProfile profile = getProfileOrThrow(driverProfileId);

        if (profile.getStatus() != DriverStatus.PENDING) {
            throw new IllegalStateException("Only pending drivers can be approved");
        }

        profile.setStatus(DriverStatus.APPROVED);
        profile.setApprovedAt(Instant.now());
        promotePendingLicense(profile);

        // Unified Approval: Approve and activate the first vehicle
        List<Vehicle> vehicles = vehicleRepository.findByDriverProfileOrderByCreatedAtAsc(profile);
        if (!vehicles.isEmpty()) {
            Vehicle firstVehicle = vehicles.get(0);
            firstVehicle.setStatus(VehicleStatus.APPROVED);
            firstVehicle.setActive(true);
            vehicleRepository.save(firstVehicle);
        }

        mailService.sendDriverApprovedEmail(profile.getUser().getEmail());

        adminActionLogger.driverApproved(
                profile.getId(),
                "Admin approved driver application"
        );

        return toAdminReviewResponse(profile);
    }

    // --- SCHEDULED TASK: SUSPEND IF LICENSE EXPIRED ---

    @Scheduled(cron = "0 0 1 * * ?") // Runs every day at 1:00 AM
    public void checkExpiredLicenses() {
        log.info("Checking for expired driver licenses...");
        LocalDate today = LocalDate.now();

        // Profiles with expired licenses
        List<DriverProfile> expiredProfiles = driverProfileRepository
                .findAllByLicenseExpiryBefore(today);

        List<DriverProfile> activeExpired = expiredProfiles.stream()
                .filter(p -> p.getStatus() == DriverStatus.APPROVED)
                .toList();

        for (DriverProfile profile : activeExpired) {
            log.warn("Suspending driver {} due to expired license (expired on {})",
                    profile.getUser().getEmail(), profile.getLicenseExpiry());

            profile.setStatus(DriverStatus.SUSPENDED);

            // Deactivate active vehicle
            Vehicle active = profile.getActiveVehicle();
            if (active != null) {
                active.setActive(false);
                vehicleRepository.save(active);
            }

            driverProfileRepository.save(profile);
            mailService.sendDriverSuspendedEmail(profile.getUser().getEmail());
        }
        log.info("Expired license check completed. {} profiles suspended.", activeExpired.size());
    }

    public DriverProfileResponse reject(UUID driverProfileId) {
        DriverProfile profile = getProfileOrThrow(driverProfileId);

        if (profile.getStatus() != DriverStatus.PENDING) {
            throw new IllegalStateException("Only pending drivers can be rejected");
        }

        profile.setStatus(DriverStatus.REJECTED);

        mailService.sendDriverRejectedEmail(profile.getUser().getEmail());

        adminActionLogger.driverRejected(
                profile.getId(),
                "Admin rejected driver application"
        );

        return toAdminReviewResponse(profile);
    }
    private DriverProfile getProfileOrThrow(UUID id) {
        return driverProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Driver profile not found"));
    }

    public DriverProfileResponse suspend(UUID driverProfileId) {
        DriverProfile profile = getProfileOrThrow(driverProfileId);

        if (profile.getStatus() != DriverStatus.APPROVED) {
            throw new IllegalStateException("Only approved drivers can be suspended");
        }

        profile.setStatus(DriverStatus.SUSPENDED);

        mailService.sendDriverSuspendedEmail(profile.getUser().getEmail());

        adminActionLogger.driverSuspended(
                profile.getId(),
                "Admin suspended driver"
        );

        return toAdminReviewResponse(profile);
    }
    public void updateLocation(UUID userId, UpdateDriverLocationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        DriverProfile profile = driverProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver profile not found"));
        
        profile.setLastLatitude(request.getLatitude());
        profile.setLastLongitude(request.getLongitude());
        profile.setLastLocationUpdatedAt(Instant.now());
        
        driverProfileRepository.save(profile);
    }

    @Transactional
    public List<DriverProfileResponse> getDriversWithStrikes() {
        return driverProfileRepository
                .findByStrikeCountGreaterThan(0)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
    @Transactional
    public DriverProfileResponse resetStrikes(UUID driverProfileId) {

        DriverProfile profile = getProfileOrThrow(driverProfileId);

        if (profile.getStrikeCount() == 0) {
            throw new IllegalStateException("Driver has no strikes to reset");
        }

        profile.setStrikeCount(0);

        if (profile.getStatus() == DriverStatus.SUSPENDED) {
            profile.setStatus(DriverStatus.APPROVED);
        }

        eventPublisher.publishEvent(
                new NotificationRequestedEvent(
                        profile.getUser().getId(),
                        "Account Restored",
                        "Your driver account has been restored and strike count has been reset.",
                        NotificationType.SYSTEM_ALERT,
                        null,
                        ReferenceType.SYSTEM
                )
        );

        mailService.sendDriverReactivatedEmail(profile.getUser().getEmail());

        adminActionLogger.driverResetStrikes(
                profile.getId(),
                "Admin reset driver strikes"
        );

        log.info("[ADMIN] Strikes reset for driverProfileId={}", driverProfileId);

        return mapper.toResponse(profile);
    }
    @Transactional
    public Page<DriverProfileResponse> getDrivers(
            DriverStatus status,
            Pageable pageable
    ) {

        Page<DriverProfile> page;

        if (status != null) {
            page = driverProfileRepository.findByStatus(status, pageable);
        } else {
            page = driverProfileRepository.findAll(pageable);
        }

        return page.map(this::toAdminReviewResponse);
    }

    @Transactional
    public DriverProfileResponse addStrike(UUID driverId, String note) {

        DriverProfile profile = getProfileOrThrow(driverId);

        profile.setStrikeCount(profile.getStrikeCount() + 1);

        if (profile.getStrikeCount() >= 5) {
            profile.setStatus(DriverStatus.SUSPENDED);
            adminActionLogger.driverSuspended(driverId, "Auto suspension after strikes");
        }

        adminActionLogger.driverStrike(driverId, note);

        return toAdminReviewResponse(profile);
    }

    private void promotePendingLicense(DriverProfile profile) {
        List<com.shipmate.model.photo.Photo> pendingPhotos = photoService.getPendingDriverLicensePhotos(profile.getId());
        boolean hasPendingFields = profile.getPendingLicenseNumber() != null || profile.getPendingLicenseExpiry() != null;
        if (!hasPendingFields && pendingPhotos.isEmpty()) {
            return;
        }

        if (profile.getPendingLicenseNumber() != null) {
            profile.setLicenseNumber(profile.getPendingLicenseNumber());
        }
        if (profile.getPendingLicenseExpiry() != null) {
            profile.setLicenseExpiry(profile.getPendingLicenseExpiry());
        }
        profile.setPendingLicenseNumber(null);
        profile.setPendingLicenseExpiry(null);

        photoService.getDriverLicensePhotos(profile.getId()).forEach(photoService::deletePhoto);
        pendingPhotos.forEach(photo -> photo.setPhotoType("DRIVER_LICENSE"));
    }

    private DriverProfileResponse toDriverViewResponse(DriverProfile profile) {
        DriverProfileResponse response = mapper.toResponse(profile);
        boolean hasApprovedLicense = profile.getLicenseNumber() != null
                || profile.getLicenseExpiry() != null
                || !photoService.getDriverLicensePhotos(profile.getId()).isEmpty();

        if (!hasApprovedLicense) {
            applyPendingLicenseToResponse(profile, response);
        }

        return response;
    }

    private DriverProfileResponse toAdminReviewResponse(DriverProfile profile) {
        DriverProfileResponse response = mapper.toResponse(profile);
        if (hasPendingLicenseUpdate(profile)) {
            applyPendingLicenseToResponse(profile, response);
        }

        return response;
    }

    private boolean hasPendingLicenseUpdate(DriverProfile profile) {
        return profile.getPendingLicenseNumber() != null
                || profile.getPendingLicenseExpiry() != null
                || !photoService.getPendingDriverLicensePhotos(profile.getId()).isEmpty();
    }

    private void applyPendingLicenseToResponse(DriverProfile profile, DriverProfileResponse response) {
        response.setLicenseNumber(valueOrFallback(profile.getPendingLicenseNumber(), profile.getLicenseNumber()));
        response.setLicenseExpiry(valueOrFallback(profile.getPendingLicenseExpiry(), profile.getLicenseExpiry()));
        List<String> pendingUrls = photoService.getPendingDriverLicensePhotos(profile.getId())
                .stream()
                .map(photo -> photo.getUrl())
                .toList();
        response.setLicensePhotoUrls(pendingUrls);
        response.setLicensePhotoUrl(pendingUrls.isEmpty() ? null : pendingUrls.get(0));
    }

    private <T> T valueOrFallback(T preferred, T fallback) {
        return preferred != null ? preferred : fallback;
    }
}
