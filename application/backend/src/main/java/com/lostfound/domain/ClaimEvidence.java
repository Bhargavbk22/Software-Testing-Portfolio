package com.lostfound.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "claim_evidence")
@Getter @Setter @NoArgsConstructor
public class ClaimEvidence {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "claim_id")
    private Claim claim;
    @Column(nullable = false, length = 1000)
    private String description;
    @CreationTimestamp @Column(updatable = false)
    private Instant createdAt;

    public ClaimEvidence(Claim claim, String description) { this.claim = claim; this.description = description; }
}
