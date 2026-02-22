package com.shipmate.model.insuranceClaim;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.user.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "insurance_claims",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_insurance_claim_shipment", columnNames = "shipment_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false,fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @ManyToOne(optional = false,fetch = FetchType.LAZY)
    @JoinColumn(name = "claimant_id", nullable = false)
    private User claimant;

    @Column(name = "declared_value_snapshot", precision = 10, scale = 2, nullable = false)
    private BigDecimal declaredValueSnapshot;

    @Column(name = "coverage_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal coverageAmount;

    @Column(name = "deductible_rate", precision = 5, scale = 4, nullable = false)
    private BigDecimal deductibleRate;

    @Column(name = "compensation_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal compensationAmount;

    @Column(name = "claim_description", columnDefinition = "TEXT")
    private String claimDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "claim_status", nullable = false)
    private ClaimStatus claimStatus;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> photos;
    
    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id")
    private User adminUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "claim_reason", nullable = false)
    private ClaimReason claimReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
