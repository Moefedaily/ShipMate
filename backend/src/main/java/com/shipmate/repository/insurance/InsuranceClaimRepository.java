package com.shipmate.repository.insurance;

import com.shipmate.model.insuranceClaim.ClaimStatus;
import com.shipmate.model.insuranceClaim.InsuranceClaim;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.user.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InsuranceClaimRepository
        extends JpaRepository<InsuranceClaim, UUID> {

    boolean existsByShipment(Shipment shipment);

    Optional<InsuranceClaim> findByShipment(Shipment shipment);

    List<InsuranceClaim> findByClaimant(User claimant);

    Page<InsuranceClaim> findByClaimant(User claimant, Pageable pageable);

    List<InsuranceClaim> findByClaimStatus(ClaimStatus status);

    @EntityGraph(attributePaths = {"shipment", "shipment.sender", "claimant", "photos"})
    Page<InsuranceClaim> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"shipment", "shipment.sender", "claimant", "photos"})
    Page<InsuranceClaim> findByClaimStatus(ClaimStatus status, Pageable pageable);

    Optional<InsuranceClaim> findByShipmentId(UUID shipmentId);

    long countByClaimStatus(ClaimStatus status);

    long countByClaimant(User claimant);

    long countByClaimStatusIn(Collection<ClaimStatus> statuses);

    @Query("""
        select distinct c
        from InsuranceClaim c
        join fetch c.shipment s
        join fetch s.sender sender
        left join fetch s.booking b
        left join fetch b.driver driver
        order by c.createdAt desc
    """)
    List<InsuranceClaim> findAllForAdmin();

    @Query("""
        select distinct c
        from InsuranceClaim c
        join fetch c.shipment s
        join fetch s.sender sender
        left join fetch s.booking b
        left join fetch b.driver driver
        where c.claimStatus = :status
        order by c.createdAt desc
    """)
    List<InsuranceClaim> findAllForAdminByStatus(@Param("status") ClaimStatus status);

    @Query("""
        select c
        from InsuranceClaim c
        join fetch c.shipment s
        join fetch s.sender sender
        left join fetch s.booking b
        left join fetch b.driver driver
        join fetch c.claimant claimant
        left join fetch c.photos
        left join fetch c.adminUser adminUser
        where c.id = :id
    """)
    Optional<InsuranceClaim> findAdminById(@Param("id") UUID id);
}