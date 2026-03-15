package com.shipmate.model.photo;

import com.shipmate.model.DriverProfile.DriverProfile;
import com.shipmate.model.insuranceClaim.InsuranceClaim;
import com.shipmate.model.message.Message;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "public_id", nullable = false)
    private String publicId;

    @Column(name = "photo_type", nullable = false, length = 50)
    private String photoType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_profile_id")
    private DriverProfile driverProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_claim_id")
    private InsuranceClaim insuranceClaim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private Message message;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
