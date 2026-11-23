package com.lostfound.repo;

import com.lostfound.domain.Claim;
import com.lostfound.domain.Recovery;
import com.lostfound.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecoveryRepository extends JpaRepository<Recovery, Long> {
    boolean existsByClaim(Claim claim);
    Page<Recovery> findByClaim_Claimant(User claimant, Pageable pageable);
}
