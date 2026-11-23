package com.lostfound.repo;

import com.lostfound.domain.Claim;
import com.lostfound.domain.Enums.ClaimStatus;
import com.lostfound.domain.FoundItem;
import com.lostfound.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    boolean existsByClaimantAndFoundItemAndStatusIn(User claimant, FoundItem item, Collection<ClaimStatus> statuses);
    boolean existsByFoundItemAndStatusIn(FoundItem item, Collection<ClaimStatus> statuses);
    List<Claim> findByFoundItemAndStatusIn(FoundItem item, Collection<ClaimStatus> statuses);
    Page<Claim> findByClaimant(User claimant, Pageable pageable);
    Page<Claim> findByStatus(ClaimStatus status, Pageable pageable);
    List<Claim> findTop10ByStatusInOrderByCreatedAtDesc(Collection<ClaimStatus> statuses);
    long countByStatus(ClaimStatus status);
}
