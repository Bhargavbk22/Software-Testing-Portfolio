package com.lostfound.repo;

import com.lostfound.domain.Claim;
import com.lostfound.domain.ClaimEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClaimEvidenceRepository extends JpaRepository<ClaimEvidence, Long> {
    List<ClaimEvidence> findByClaimOrderByCreatedAtAsc(Claim claim);
}
