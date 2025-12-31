package com.shipmate.repository.driver;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shipmate.model.DriverProfile.DriverStatus;
import com.shipmate.model.user.User;
import com.shipmate.model.DriverProfile.DriverProfile;;

public interface DriverProfileRepository extends JpaRepository<DriverProfile, UUID> {

    Optional<DriverProfile> findByUser(User user);

    boolean existsByUser(User user);

    List<DriverProfile> findByStatus(DriverStatus status);

    Optional<DriverProfile> findByUser_Id(UUID userId);
}
