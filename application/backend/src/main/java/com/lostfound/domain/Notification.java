package com.lostfound.domain;

import com.lostfound.domain.Enums.NotificationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "notifications", indexes = {@Index(name = "idx_notif_user", columnList = "user_id")})
@Getter @Setter @NoArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "user_id")
    private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private NotificationType type;
    @Column(nullable = false, length = 500)
    private String message;
    @Column(name = "is_read", nullable = false)
    private boolean readStatus;
    @Column(length = 20)
    private String relatedType;
    private Long relatedId;
    @CreationTimestamp @Column(updatable = false)
    private Instant createdAt;
}
