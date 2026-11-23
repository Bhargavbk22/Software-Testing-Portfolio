package com.lostfound.domain;

import com.lostfound.domain.Enums.MatchStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "item_matches", uniqueConstraints = @UniqueConstraint(columnNames = {"lost_item_id", "found_item_id"}))
@Getter @Setter @NoArgsConstructor
public class ItemMatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "lost_item_id")
    private LostItem lostItem;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "found_item_id")
    private FoundItem foundItem;
    @Column(nullable = false)
    private int score;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private MatchStatus status = MatchStatus.SUGGESTED;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "reviewed_by")
    private User reviewedBy;
    @CreationTimestamp @Column(updatable = false)
    private Instant createdAt;
}
