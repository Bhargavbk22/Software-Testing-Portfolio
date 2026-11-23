package com.lostfound.domain;

import com.lostfound.domain.Enums.FoundStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "found_items", indexes = {@Index(name = "idx_found_status", columnList = "status"), @Index(name = "idx_found_date", columnList = "found_date")})
@AttributeOverrides({@AttributeOverride(name = "eventDate", column = @Column(name = "found_date", nullable = false)),
        @AttributeOverride(name = "location", column = @Column(name = "found_location", nullable = false, length = 150))})
@Getter @Setter @NoArgsConstructor
public class FoundItem extends BaseItem {
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private FoundStatus status = FoundStatus.REPORTED;
    @Column(length = 150)
    private String handoverLocation;
}
