package com.lostfound.repo;

import com.lostfound.domain.Notification;
import com.lostfound.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUser(User user, Pageable pageable);

    @Query("select n from Notification n where n.user = :user and n.readStatus = false")
    Page<Notification> findUnread(@Param("user") User user, Pageable pageable);

    @Query("select count(n) from Notification n where n.user = :user and n.readStatus = false")
    long countUnread(@Param("user") User user);

    @org.springframework.data.jpa.repository.Modifying
    @Query("update Notification n set n.readStatus = true where n.user = :user and n.readStatus = false")
    int markAllRead(@Param("user") User user);
}
