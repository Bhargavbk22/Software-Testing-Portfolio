package com.lostfound.domain;

import com.lostfound.domain.Enums.ClaimStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "claims", indexes = {@Index(name = "idx_claim_status", columnList = "status")})
@Getter @Setter @NoArgsConstructor
public class Claim {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "found_item_id")
    private FoundItem foundItem;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "claimant_id")
    private User claimant;
    @Column(nullable = false, length = 2000)
    private String ownershipExplanation;
    @Column(nullable = false, length = 1000)
    private String distinguishingCharacteristics;
    @Column(length = 1000)
    private String additionalDetails;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ClaimStatus status = ClaimStatus.PENDING;
    @Column(nullable = false)
    private boolean ownershipVerified;
    @Column(length = 1000)
    private String decisionReason;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "reviewed_by")
    private User reviewedBy;
    @CreationTimestamp @Column(updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
}
