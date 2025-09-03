package com.shipmate.model.insuranceClaim;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.user.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "insurance_claims")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @ManyToOne(optional = false)
    @JoinColumn(name = "claimant_id", nullable = false)
    private User claimant;

    @Column(name = "claim_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal claimAmount;

    @Column(name = "claim_description", columnDefinition = "TEXT")
    private String claimDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "claim_status", nullable = false)
    private ClaimStatus claimStatus;

    @Column(columnDefinition = "TEXT")
    private String photos; // JSON string representing uploaded photos

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @ManyToOne
    @JoinColumn(name = "admin_user_id")
    private User adminUser; // optional, null if not yet reviewed

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
