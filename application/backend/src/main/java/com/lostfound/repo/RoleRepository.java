package com.lostfound.repo;

import com.lostfound.domain.Enums.RoleName;
import com.lostfound.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
