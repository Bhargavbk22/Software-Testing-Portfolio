package com.lostfound.service;

import com.lostfound.domain.ActivityLog;
import com.lostfound.domain.StatusHistory;
import com.lostfound.domain.User;
import com.lostfound.repo.ActivityLogRepository;
import com.lostfound.repo.StatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActivityService {
    private final ActivityLogRepository logs;
    private final StatusHistoryRepository history;

    public void log(User user, String action, String entityType, Long entityId, String details, boolean suspicious) {
        ActivityLog l = new ActivityLog();
        l.setUser(user);
        l.setAction(action);
        l.setEntityType(entityType);
        l.setEntityId(entityId);
        l.setDetails(details != null && details.length() > 500 ? details.substring(0, 500) : details);
        l.setSuspicious(suspicious);
        logs.save(l);
    }

    public void log(User user, String action, String entityType, Long entityId, String details) {
        log(user, action, entityType, entityId, details, false);
    }

    public void recordStatus(String entityType, Long entityId, String from, String to, User by, String note) {
        StatusHistory h = new StatusHistory();
        h.setEntityType(entityType);
        h.setEntityId(entityId);
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setChangedBy(by);
        h.setNote(note);
        history.save(h);
    }
}
