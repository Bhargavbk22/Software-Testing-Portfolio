package com.lostfound.domain;

import com.lostfound.domain.Enums.LostStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lost_items", indexes = {@Index(name = "idx_lost_status", columnList = "status"), @Index(name = "idx_lost_date", columnList = "lost_date")})
@AttributeOverrides({@AttributeOverride(name = "eventDate", column = @Column(name = "lost_date", nullable = false)),
        @AttributeOverride(name = "location", column = @Column(name = "lost_location", nullable = false, length = 150))})
@Getter @Setter @NoArgsConstructor
public class LostItem extends BaseItem {
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private LostStatus status = LostStatus.REPORTED;
    @Column(length = 150)
    private String contactInfo;
}
