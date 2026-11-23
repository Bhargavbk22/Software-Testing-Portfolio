package com.lostfound.repo;

import com.lostfound.domain.Category;
import com.lostfound.domain.Enums.FoundStatus;
import com.lostfound.domain.FoundItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FoundItemRepository extends JpaRepository<FoundItem, Long>, JpaSpecificationExecutor<FoundItem> {
    List<FoundItem> findByStatusIn(Collection<FoundStatus> statuses);
    Page<FoundItem> findByStatus(FoundStatus status, Pageable pageable);
    long countByStatus(FoundStatus status);
    boolean existsByCategory(Category category);

    /** Pessimistic lock used to serialise concurrent claim submissions / approvals on the same item. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FoundItem f where f.id = :id")
    Optional<FoundItem> findByIdForUpdate(@Param("id") Long id);
}
