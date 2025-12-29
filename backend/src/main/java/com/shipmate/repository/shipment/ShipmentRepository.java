package com.shipmate.repository.shipment;


import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

    Page<Shipment> findBySender(User sender, Pageable pageable);

    Optional<Shipment> findByIdAndSender(UUID id, User sender);

    Page<Shipment> findByStatus(ShipmentStatus status, Pageable pageable);
}
