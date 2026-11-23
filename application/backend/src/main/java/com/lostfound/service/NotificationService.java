package com.lostfound.service;

import com.lostfound.domain.Enums.NotificationType;
import com.lostfound.domain.Enums.RoleName;
import com.lostfound.domain.Notification;
import com.lostfound.domain.User;
import com.lostfound.exception.ResourceNotFoundException;
import com.lostfound.repo.NotificationRepository;
import com.lostfound.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository repo;
    private final UserRepository users;

    public void notify(User user, NotificationType type, String message, String relatedType, Long relatedId) {
        if (user == null) return;
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setMessage(message.length() > 500 ? message.substring(0, 500) : message);
        n.setRelatedType(relatedType);
        n.setRelatedId(relatedId);
        repo.save(n);
    }

    public void notifyRole(RoleName role, NotificationType type, String message, String relatedType, Long relatedId) {
        users.findByRoles_Name(role).forEach(u -> notify(u, type, message, relatedType, relatedId));
    }

    public Page<Notification> list(User user, boolean unreadOnly, Pageable pageable) {
        return unreadOnly ? repo.findUnread(user, pageable) : repo.findByUser(user, pageable);
    }

    public long unreadCount(User user) { return repo.countUnread(user); }

    @Transactional
    public Notification markRead(Long id, User user) {
        Notification n = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!n.getUser().getId().equals(user.getId())) throw new AccessDeniedException("You cannot modify another user's notification");
        n.setReadStatus(true);
        return repo.save(n);
    }

    @Transactional
    public int markAllRead(User user) { return repo.markAllRead(user); }
}
