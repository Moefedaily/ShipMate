package com.shipmate.service.driver;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.shipmate.dto.request.driver.DriverApplyRequest;
import com.shipmate.dto.request.driver.UpdateDriverLocationRequest;
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
import com.shipmate.service.mail.MailService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DriverProfileService {

    private final DriverProfileRepository driverProfileRepository;
    private final DriverProfileMapper mapper;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final ApplicationEventPublisher eventPublisher;
    public DriverProfileResponse apply(UUID userId, DriverApplyRequest request) {

    User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (driverProfileRepository.existsByUser(user)) {
            throw new IllegalStateException("Driver profile already exists");
        }
        
        DriverProfile profile = mapper.toEntity(request, user);
        DriverProfile saved = driverProfileRepository.save(profile);
        return mapper.toResponse(saved);
    }

    public DriverProfileResponse getMyProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        DriverProfile profile = driverProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Driver profile not found"));
        return mapper.toResponse(profile);
    }


    public List<DriverProfileResponse> getPendingDrivers() {
        return driverProfileRepository.findByStatus(DriverStatus.PENDING)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    public DriverProfileResponse approve(UUID driverProfileId) {
        DriverProfile profile = getProfileOrThrow(driverProfileId);

        if (profile.getStatus() != DriverStatus.PENDING) {
            throw new IllegalStateException("Only pending drivers can be approved");
        }

        profile.setStatus(DriverStatus.APPROVED);
        profile.setApprovedAt(Instant.now());

         mailService.sendDriverApprovedEmail(
            profile.getUser().getEmail()
        );
        return mapper.toResponse(profile);
    }

    public DriverProfileResponse reject(UUID driverProfileId) {
        DriverProfile profile = getProfileOrThrow(driverProfileId);

        if (profile.getStatus() != DriverStatus.PENDING) {
            throw new IllegalStateException("Only pending drivers can be rejected");
        }

        profile.setStatus(DriverStatus.REJECTED);
        profile.setApprovedAt(Instant.now());

         mailService.sendDriverRejectedEmail(
            profile.getUser().getEmail()
        );
        return mapper.toResponse(profile);
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

        mailService.sendDriverSuspendedEmail(
            profile.getUser().getEmail()
        );
        return mapper.toResponse(profile);
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

        // Notify driver
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

        log.info("[ADMIN] Strikes reset for driverProfileId={}", driverProfileId);

        return mapper.toResponse(profile);
    }
}
