package com.shipmate.repository.insurance;

import com.shipmate.model.insuranceClaim.ClaimStatus;
import com.shipmate.model.insuranceClaim.InsuranceClaim;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InsuranceClaimRepository
        extends JpaRepository<InsuranceClaim, UUID> {

    boolean existsByShipment(Shipment shipment);

    Optional<InsuranceClaim> findByShipment(Shipment shipment);

    List<InsuranceClaim> findByClaimant(User claimant);

    List<InsuranceClaim> findByClaimStatus(ClaimStatus status);

    Optional<InsuranceClaim> findByShipmentId(UUID shipmentId);
}