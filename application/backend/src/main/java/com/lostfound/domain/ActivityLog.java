package com.lostfound.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "activity_logs", indexes = {@Index(name = "idx_log_suspicious", columnList = "suspicious")})
@Getter @Setter @NoArgsConstructor
public class ActivityLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "user_id")
    private User user;
    @Column(nullable = false, length = 60)
    private String action;
    @Column(length = 20)
    private String entityType;
    private Long entityId;
    @Column(length = 500)
    private String details;
    @Column(nullable = false)
    private boolean suspicious;
    @CreationTimestamp @Column(updatable = false)
    private Instant createdAt;
}
