package com.shipmate.unit.service.vehicle;

import com.shipmate.dto.request.vehicle.CreateVehicleRequest;
import com.shipmate.dto.response.vehicle.VehicleResponse;
import com.shipmate.mapper.vehicle.VehicleMapper;
import com.shipmate.model.DriverProfile.DriverProfile;
import com.shipmate.model.DriverProfile.DriverStatus;
import com.shipmate.model.booking.Booking;
import com.shipmate.model.user.User;
import com.shipmate.model.vehicle.Vehicle;
import com.shipmate.model.vehicle.VehicleStatus;
import com.shipmate.repository.booking.BookingRepository;
import com.shipmate.repository.driver.DriverProfileRepository;
import com.shipmate.repository.vehicle.VehicleRepository;
import com.shipmate.service.vehicle.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock private VehicleRepository vehicleRepository;
    @Mock private DriverProfileRepository driverProfileRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private VehicleMapper vehicleMapper;

    @InjectMocks private VehicleService vehicleService;

    private User user;
    private DriverProfile profile;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("driver@test.com").build();
        profile = DriverProfile.builder()
                .id(UUID.randomUUID())
                .user(user)
                .status(DriverStatus.APPROVED)
                .vehicles(new ArrayList<>())
                .build();
    }

    @Test
    void addVehicle_shouldSucceed_whenProfileApproved() {
        CreateVehicleRequest request = new CreateVehicleRequest();
        Vehicle vehicle = new Vehicle();
        
        when(driverProfileRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(profile));
        when(vehicleMapper.toEntity(any())).thenReturn(vehicle);
        when(vehicleRepository.save(any())).thenReturn(vehicle);
        when(vehicleMapper.toResponse(any())).thenReturn(new VehicleResponse());

        vehicleService.addVehicle(user.getId(), request);

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.APPROVED);
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void activateVehicle_shouldFail_whenOngoingDelivery() {
        UUID vehicleId = UUID.randomUUID();
        Vehicle vehicle = Vehicle.builder()
                .id(vehicleId)
                .driverProfile(profile)
                .status(VehicleStatus.APPROVED)
                .build();

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(bookingRepository.findFirstByDriverAndStatusInOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(new Booking()));

        assertThatThrownBy(() -> vehicleService.activateVehicle(user.getId(), vehicleId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot switch vehicles while you have an ongoing delivery.");
    }

    @Test
    void activateVehicle_shouldSucceed_whenNoOngoingDelivery() {
        UUID vehicleId = UUID.randomUUID();
        Vehicle vehicle = Vehicle.builder()
                .id(vehicleId)
                .driverProfile(profile)
                .status(VehicleStatus.APPROVED)
                .active(false)
                .build();

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(bookingRepository.findFirstByDriverAndStatusInOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.ofNullable(null));
        when(vehicleRepository.findByDriverProfile(profile)).thenReturn(List.of(vehicle));

        vehicleService.activateVehicle(user.getId(), vehicleId);

        assertThat(vehicle.isActive()).isTrue();
        verify(vehicleRepository).saveAll(anyList());
    }
}
