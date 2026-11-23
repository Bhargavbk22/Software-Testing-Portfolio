package com.lostfound.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

/** Fields shared by lost and found reports. */
@MappedSuperclass
@Getter @Setter
public abstract class BaseItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "reporter_id")
    private User reporter;
    @Column(nullable = false, length = 100)
    private String name;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "category_id")
    private Category category;
    @Column(nullable = false, length = 2000)
    private String description;
    @Column(nullable = false)
    private LocalDate eventDate;
    @Column(nullable = false, length = 150)
    private String location;
    @Column(length = 50)
    private String color;
    @Column(length = 50)
    private String brand;
    @Column(length = 1000)
    private String identifyingCharacteristics;
    @Column(length = 1000)
    private String imagePaths;
    @Column(nullable = false)
    private boolean verified;
    @CreationTimestamp @Column(updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
}
