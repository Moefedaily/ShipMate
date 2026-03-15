package com.shipmate.service.vehicle;

import com.shipmate.dto.request.vehicle.CreateVehicleRequest;
import com.shipmate.dto.response.vehicle.VehicleResponse;
import com.shipmate.mapper.vehicle.VehicleMapper;
import com.shipmate.model.DriverProfile.DriverProfile;
import com.shipmate.model.booking.BookingStatus;
import com.shipmate.model.vehicle.Vehicle;
import com.shipmate.model.vehicle.VehicleStatus;
import com.shipmate.repository.booking.BookingRepository;
import com.shipmate.repository.driver.DriverProfileRepository;
import com.shipmate.repository.vehicle.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final BookingRepository bookingRepository;
    private final VehicleMapper vehicleMapper;

    // --- DRIVER ACTIONS ---

    public VehicleResponse addVehicle(UUID userId, CreateVehicleRequest request) {
        DriverProfile profile = driverProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new IllegalArgumentException("Driver profile not found"));

        if (!profile.hasApprovedLicense()) {
            throw new IllegalStateException("Your driver profile must be approved before adding additional vehicles");
        }

        Vehicle vehicle = vehicleMapper.toEntity(request);
        vehicle.setDriverProfile(profile);
        vehicle.setStatus(VehicleStatus.APPROVED);
        vehicle.setActive(false);

        Vehicle saved = vehicleRepository.save(vehicle);
        return vehicleMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> getMyVehicles(UUID userId) {
        DriverProfile profile = driverProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new IllegalArgumentException("Driver profile not found"));

        return vehicleRepository.findByDriverProfileOrderByCreatedAtDesc(profile)
                .stream()
                .map(vehicleMapper::toResponse)
                .collect(Collectors.toList());
    }

    public VehicleResponse activateVehicle(UUID userId, UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

        DriverProfile profile = vehicle.getDriverProfile();
        if (!profile.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this vehicle");
        }

        if (vehicle.getStatus() != VehicleStatus.APPROVED) {
            throw new IllegalStateException("Vehicle not approved yet");
        }

        // --- CONSTRAINT: CANNOT SWITCH IF HAS ONGOING DELIVERY ---
        boolean hasOngoing = bookingRepository.findFirstByDriverAndStatusInOrderByCreatedAtDesc(
                profile.getUser(),
                Arrays.asList(BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS)
        ).isPresent();

        if (hasOngoing) {
            throw new IllegalStateException("Cannot switch vehicles while you have an ongoing delivery.");
        }

        // Deactivate all others
        List<Vehicle> myVehicles = vehicleRepository.findByDriverProfile(profile);
        for (Vehicle v : myVehicles) {
            v.setActive(v.getId().equals(vehicleId));
        }
        vehicleRepository.saveAll(myVehicles);

        return vehicleMapper.toResponse(vehicle);
    }
}
