package com.lostfound.service;

import com.lostfound.domain.*;
import com.lostfound.domain.Enums.*;
import com.lostfound.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Applies a status change only if the state machine allows it, persists it and records status history. */
@Service
@RequiredArgsConstructor
public class StatusService {
    private final LostItemRepository lostRepo;
    private final FoundItemRepository foundRepo;
    private final ClaimRepository claimRepo;
    private final RecoveryRepository recoveryRepo;
    private final ActivityService activity;

    public void initial(String type, Long id, String to, User by) { activity.recordStatus(type, id, null, to, by, "Created"); }

    public void lost(LostItem i, LostStatus to, User by, String note) {
        StateMachine.checkLost(i.getStatus(), to);
        String from = i.getStatus().name();
        i.setStatus(to);
        lostRepo.save(i);
        activity.recordStatus("LOST", i.getId(), from, to.name(), by, note);
    }

    public void found(FoundItem i, FoundStatus to, User by, String note) {
        StateMachine.checkFound(i.getStatus(), to);
        String from = i.getStatus().name();
        i.setStatus(to);
        foundRepo.save(i);
        activity.recordStatus("FOUND", i.getId(), from, to.name(), by, note);
    }

    public void claim(Claim c, ClaimStatus to, User by, String note) {
        StateMachine.checkClaim(c.getStatus(), to);
        String from = c.getStatus().name();
        c.setStatus(to);
        claimRepo.save(c);
        activity.recordStatus("CLAIM", c.getId(), from, to.name(), by, note);
    }

    public void recovery(Recovery r, RecoveryStatus to, User by, String note) {
        StateMachine.checkRecovery(r.getStatus(), to);
        String from = r.getStatus().name();
        r.setStatus(to);
        recoveryRepo.save(r);
        activity.recordStatus("RECOVERY", r.getId(), from, to.name(), by, note);
    }
}
