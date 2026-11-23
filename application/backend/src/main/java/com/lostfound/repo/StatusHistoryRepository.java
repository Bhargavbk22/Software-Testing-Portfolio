package com.lostfound.repo;

import com.lostfound.domain.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
    List<StatusHistory> findByEntityTypeAndEntityIdOrderByCreatedAtAsc(String entityType, Long entityId);
}
