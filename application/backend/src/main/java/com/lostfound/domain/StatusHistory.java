package com.lostfound.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "status_history", indexes = {@Index(name = "idx_history_entity", columnList = "entity_type,entity_id")})
@Getter @Setter @NoArgsConstructor
public class StatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "entity_type", nullable = false, length = 20)
    private String entityType;
    @Column(name = "entity_id", nullable = false)
    private Long entityId;
    @Column(length = 30)
    private String fromStatus;
    @Column(nullable = false, length = 30)
    private String toStatus;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "changed_by")
    private User changedBy;
    @Column(length = 500)
    private String note;
    @CreationTimestamp @Column(updatable = false)
    private Instant createdAt;
}
