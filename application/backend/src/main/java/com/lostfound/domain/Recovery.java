package com.lostfound.domain;

import com.lostfound.domain.Enums.RecoveryStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "recoveries")
@Getter @Setter @NoArgsConstructor
public class Recovery {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "claim_id", unique = true)
    private Claim claim;
    private LocalDateTime scheduledAt;
    private Instant handoverDate;
    @Column(nullable = false, length = 150)
    private String location;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private RecoveryStatus status = RecoveryStatus.SCHEDULED;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "handled_by")
    private User handledBy;
    @Column(length = 40)
    private String confirmationCode;
    @CreationTimestamp @Column(updatable = false)
    private Instant createdAt;
}
