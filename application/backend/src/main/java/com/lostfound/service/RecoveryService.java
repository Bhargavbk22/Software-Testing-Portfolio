package com.lostfound.service;

import com.lostfound.domain.*;
import com.lostfound.domain.Enums.*;
import com.lostfound.dto.Dto.*;
import com.lostfound.exception.BusinessRuleException;
import com.lostfound.exception.DuplicateResourceException;
import com.lostfound.exception.ResourceNotFoundException;
import com.lostfound.exception.ValidationFailedException;
import com.lostfound.repo.ClaimRepository;
import com.lostfound.repo.FoundItemRepository;
import com.lostfound.repo.ItemMatchRepository;
import com.lostfound.repo.RecoveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecoveryService {
    private final RecoveryRepository recoveryRepo;
    private final ClaimRepository claimRepo;
    private final FoundItemRepository foundRepo;
    private final ItemMatchRepository matchRepo;
    private final StatusService status;
    private final ActivityService activity;
    private final NotificationService notifications;
    private final Mapper mapper;

    @Transactional
    public RecoveryResponse schedule(RecoveryRequest r, User admin) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (r.claimId() == null) errors.put("claimId", "Claim is required");
        if (r.scheduledAt() == null) errors.put("scheduledAt", "Handover date and time are required");
        if (r.location() == null || r.location().isBlank()) errors.put("location", "Handover location is required");
        if (!errors.isEmpty()) throw new ValidationFailedException(errors);

        Claim c = claimRepo.findById(r.claimId()).orElseThrow(() -> new ResourceNotFoundException("Claim not found"));
        if (recoveryRepo.existsByClaim(c)) throw new DuplicateResourceException("A recovery already exists for this claim");
        if (c.getStatus() != ClaimStatus.APPROVED || !c.isOwnershipVerified())
            throw new BusinessRuleException("An item cannot be handed over before its claim is approved and ownership is verified");

        Recovery rec = new Recovery();
        rec.setClaim(c);
        rec.setScheduledAt(r.scheduledAt());
        rec.setLocation(r.location().trim());
        rec.setStatus(RecoveryStatus.SCHEDULED);
        rec.setHandledBy(admin);
        recoveryRepo.save(rec);
        status.initial("RECOVERY", rec.getId(), "SCHEDULED", admin);
        status.claim(c, ClaimStatus.HANDOVER_SCHEDULED, admin, "Handover scheduled");
        notifications.notify(c.getClaimant(), NotificationType.RECOVERY_SCHEDULED,
                "Handover for \"" + c.getFoundItem().getName() + "\" is scheduled at " + rec.getLocation() + " on " + rec.getScheduledAt() + ".", "CLAIM", c.getId());
        activity.log(admin, "RECOVERY_SCHEDULED", "RECOVERY", rec.getId(), rec.getLocation());
        return mapper.recovery(rec);
    }

    @Transactional
    public RecoveryResponse complete(Long id, User admin) {
        Recovery rec = recoveryRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Recovery not found"));
        StateMachine.checkRecovery(rec.getStatus(), RecoveryStatus.COMPLETED);             // 409 if already completed/cancelled
        Claim c = rec.getClaim();
        if (c.getStatus() != ClaimStatus.HANDOVER_SCHEDULED)
            throw new BusinessRuleException("An item cannot be returned before its claim is approved and the handover is scheduled");
        FoundItem f = foundRepo.findByIdForUpdate(c.getFoundItem().getId()).orElseThrow(() -> new ResourceNotFoundException("Found item not found"));
        if (f.getStatus() == FoundStatus.RETURNED) throw new BusinessRuleException("This item has already been returned");

        status.found(f, FoundStatus.RETURNED, admin, "Item handed over to verified owner");
        status.claim(c, ClaimStatus.RETURNED, admin, "Item returned");
        status.recovery(rec, RecoveryStatus.COMPLETED, admin, "Handover completed");
        rec.setHandoverDate(Instant.now());
        rec.setHandledBy(admin);
        rec.setConfirmationCode("REC-" + Year.now().getValue() + "-" + String.format("%05d", rec.getId()));
        recoveryRepo.save(rec);

        for (ItemMatch m : matchRepo.findByFoundItemAndStatus(f, MatchStatus.CONFIRMED)) {
            LostItem l = m.getLostItem();
            if (l.getStatus() == LostStatus.MATCHED) {
                status.lost(l, LostStatus.RESOLVED, admin, "Item recovered");
                notifications.notify(l.getReporter(), NotificationType.RECOVERY_COMPLETED, "Your lost item \"" + l.getName() + "\" has been recovered.", "LOST", l.getId());
            }
        }
        notifications.notify(c.getClaimant(), NotificationType.RECOVERY_COMPLETED,
                "Your item \"" + f.getName() + "\" was returned. Confirmation code: " + rec.getConfirmationCode(), "CLAIM", c.getId());
        activity.log(admin, "RECOVERY_COMPLETED", "RECOVERY", rec.getId(), rec.getConfirmationCode());
        return mapper.recovery(rec);
    }

    @Transactional(readOnly = true)
    public Page<Recovery> list(int page, int size) {
        return recoveryRepo.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional(readOnly = true)
    public Page<Recovery> mine(User user, int page, int size) {
        return recoveryRepo.findByClaim_Claimant(user, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }
}
