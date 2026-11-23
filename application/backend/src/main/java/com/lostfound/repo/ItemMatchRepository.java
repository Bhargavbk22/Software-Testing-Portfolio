package com.lostfound.repo;

import com.lostfound.domain.Enums.MatchStatus;
import com.lostfound.domain.FoundItem;
import com.lostfound.domain.ItemMatch;
import com.lostfound.domain.LostItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemMatchRepository extends JpaRepository<ItemMatch, Long> {
    boolean existsByLostItemAndFoundItem(LostItem lost, FoundItem found);
    List<ItemMatch> findByLostItemOrderByScoreDesc(LostItem lost);
    List<ItemMatch> findByFoundItemOrderByScoreDesc(FoundItem found);
    List<ItemMatch> findByFoundItemAndStatus(FoundItem found, MatchStatus status);
    Page<ItemMatch> findByStatus(MatchStatus status, Pageable pageable);
}
