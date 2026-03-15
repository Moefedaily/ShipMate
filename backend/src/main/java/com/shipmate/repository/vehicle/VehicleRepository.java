package com.shipmate.repository.vehicle;

import com.shipmate.model.DriverProfile.DriverProfile;
import com.shipmate.model.vehicle.Vehicle;
import com.shipmate.model.vehicle.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    List<Vehicle> findByDriverProfile(DriverProfile driverProfile);

    Optional<Vehicle> findByDriverProfileAndActiveTrue(DriverProfile driverProfile);

    List<Vehicle> findByStatus(VehicleStatus status);

    List<Vehicle> findByDriverProfileOrderByCreatedAtAsc(DriverProfile driverProfile);

    List<Vehicle> findByDriverProfileOrderByCreatedAtDesc(DriverProfile driverProfile);
}
