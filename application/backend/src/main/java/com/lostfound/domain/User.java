package com.lostfound.domain;

import com.lostfound.domain.Enums.RoleName;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String fullName;
    @Column(nullable = false, unique = true, length = 254)
    private String email;
    @Column(nullable = false)
    private String passwordHash;
    @Column(nullable = false, unique = true, length = 20)
    private String studentId;
    @Column(length = 100)
    private String department;
    @Column(length = 20)
    private String phone;
    @Column(nullable = false)
    private boolean enabled = true;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();
    @CreationTimestamp @Column(updatable = false)
    private Instant createdAt;

    public boolean hasRole(RoleName n) { return roles.stream().anyMatch(r -> r.getName() == n); }
    public boolean admin() { return hasRole(RoleName.ROLE_ADMIN); }
    public boolean staff() { return admin() || hasRole(RoleName.ROLE_MODERATOR); }
}
