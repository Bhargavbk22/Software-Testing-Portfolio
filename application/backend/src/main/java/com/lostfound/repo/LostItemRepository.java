package com.lostfound.repo;

import com.lostfound.domain.Category;
import com.lostfound.domain.Enums.LostStatus;
import com.lostfound.domain.LostItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

public interface LostItemRepository extends JpaRepository<LostItem, Long>, JpaSpecificationExecutor<LostItem> {
    List<LostItem> findByStatusIn(Collection<LostStatus> statuses);
    Page<LostItem> findByStatus(LostStatus status, Pageable pageable);
    long countByStatus(LostStatus status);
    boolean existsByCategory(Category category);
}
