package com.shipmate.repository.shipment;


import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

    Page<Shipment> findBySender(User sender, Pageable pageable);

    Optional<Shipment> findByIdAndSender(UUID id, User sender);

    Page<Shipment> findByStatus(ShipmentStatus status, Pageable pageable);
    
    List<Shipment> findByBookingId(UUID bookingId);
    
    boolean existsByBooking_IdAndSender_Id(UUID bookingId, UUID senderId);

    @Query("""
        select s from Shipment s
        join s.booking b
        where s.sender.id = :userId
        or b.driver.id = :userId
    """)
    List<Shipment> findAllUserShipments(UUID userId);

    @Query("""
        select s from Shipment s
        join fetch s.booking b
        join fetch s.sender sender
        where s.id = :shipmentId
    """)
    Optional<Shipment> findWithBookingAndSender(@Param("shipmentId") UUID shipmentId);

}
