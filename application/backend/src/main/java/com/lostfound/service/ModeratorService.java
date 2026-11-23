package com.lostfound.service;

import com.lostfound.domain.*;
import com.lostfound.domain.Enums.*;
import com.lostfound.dto.Dto.*;
import com.lostfound.exception.BusinessRuleException;
import com.lostfound.exception.ValidationFailedException;
import com.lostfound.repo.FoundItemRepository;
import com.lostfound.repo.LostItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ModeratorService {
    private final LostItemRepository lostRepo;
    private final FoundItemRepository foundRepo;
    private final ItemService items;
    private final StatusService status;
    private final MatchService matching;
    private final ClaimService claims;
    private final NotificationService notifications;
    private final ActivityService activity;
    private final Mapper mapper;

    private static final PageRequest TOP = PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "createdAt"));

    @Transactional(readOnly = true)
    public ModeratorDashboard dashboard(User mod) {
        List<ItemResponse> pendingLost = lostRepo.findByStatus(LostStatus.REPORTED, TOP).map(i -> mapper.item(i, mod, false)).getContent();
        List<ItemResponse> pendingFound = foundRepo.findByStatus(FoundStatus.REPORTED, TOP).map(i -> mapper.item(i, mod, false)).getContent();
        List<ItemResponse> awaiting = new ArrayList<>();
        lostRepo.findByStatus(LostStatus.UNDER_REVIEW, TOP).forEach(i -> awaiting.add(mapper.item(i, mod, false)));
        foundRepo.findByStatus(FoundStatus.UNDER_REVIEW, TOP).forEach(i -> awaiting.add(mapper.item(i, mod, false)));
        List<MatchResponse> matches = matching.list(MatchStatus.SUGGESTED, 0, 8).map(mapper::match).getContent();
        List<ClaimResponse> claimList = claims.needingReview().stream().map(c -> mapper.claim(c, true)).toList();
        List<ItemResponse> recent = new ArrayList<>();
        lostRepo.findByStatus(LostStatus.REJECTED, TOP).forEach(i -> recent.add(mapper.item(i, mod, false)));
        foundRepo.findByStatus(FoundStatus.AVAILABLE, TOP).forEach(i -> recent.add(mapper.item(i, mod, false)));
        foundRepo.findByStatus(FoundStatus.REJECTED, TOP).forEach(i -> recent.add(mapper.item(i, mod, false)));
        recent.sort(Comparator.comparing(ItemResponse::createdAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return new ModeratorDashboard(pendingLost, pendingFound, awaiting, matches, claimList, recent.stream().limit(8).toList());
    }

    @Transactional
    public ItemResponse review(String type, Long id, User mod) {
        if ("LOST".equalsIgnoreCase(type)) {
            LostItem i = items.loadLost(id);
            status.lost(i, LostStatus.UNDER_REVIEW, mod, "Review started");
            activity.log(mod, "REPORT_REVIEW_STARTED", "LOST", id, null);
            return mapper.item(i, mod, true);
        }
        FoundItem i = items.loadFound(id);
        status.found(i, FoundStatus.UNDER_REVIEW, mod, "Review started");
        activity.log(mod, "REPORT_REVIEW_STARTED", "FOUND", id, null);
        return mapper.item(i, mod, true);
    }

    /** Verifies a report. Found reports additionally become AVAILABLE, but only when all mandatory information is present. */
    @Transactional
    public ItemResponse approve(String type, Long id, User mod) {
        if ("LOST".equalsIgnoreCase(type)) {
            LostItem i = items.loadLost(id);
            if (i.getStatus() != LostStatus.UNDER_REVIEW) throw new BusinessRuleException("Only reports under review can be verified");
            i.setVerified(true);
            lostRepo.save(i);
            notifications.notify(i.getReporter(), NotificationType.REPORT_REVIEWED, "Your lost report \"" + i.getName() + "\" was verified by a moderator.", "LOST", id);
            activity.log(mod, "REPORT_VERIFIED", "LOST", id, null);
            return mapper.item(i, mod, true);
        }
        FoundItem i = items.loadFound(id);
        StateMachine.checkFound(i.getStatus(), FoundStatus.AVAILABLE);
        if (isBlank(i.getDescription()) || isBlank(i.getLocation()) || isBlank(i.getHandoverLocation()))
            throw new BusinessRuleException("Mandatory information is incomplete; the report cannot be approved");
        i.setVerified(true);
        status.found(i, FoundStatus.AVAILABLE, mod, "Verified and published");
        notifications.notify(i.getReporter(), NotificationType.REPORT_REVIEWED, "Your found report \"" + i.getName() + "\" was verified and is now listed.", "FOUND", id);
        activity.log(mod, "REPORT_VERIFIED", "FOUND", id, null);
        matching.runForFound(i);
        return mapper.item(i, mod, true);
    }

    @Transactional
    public ItemResponse reject(String type, Long id, String reason, User mod) {
        if (reason == null || reason.isBlank()) throw new ValidationFailedException(Map.of("reason", "A rejection reason is required"));
        ItemResponse res;
        if ("LOST".equalsIgnoreCase(type)) {
            LostItem i = items.loadLost(id);
            status.lost(i, LostStatus.REJECTED, mod, reason);
            notifications.notify(i.getReporter(), NotificationType.REPORT_REVIEWED, "Your lost report \"" + i.getName() + "\" was rejected: " + reason.trim(), "LOST", id);
            res = mapper.item(i, mod, true);
        } else {
            FoundItem i = items.loadFound(id);
            status.found(i, FoundStatus.REJECTED, mod, reason);
            notifications.notify(i.getReporter(), NotificationType.REPORT_REVIEWED, "Your found report \"" + i.getName() + "\" was rejected: " + reason.trim(), "FOUND", id);
            res = mapper.item(i, mod, true);
        }
        activity.log(mod, "REPORT_REJECTED", type.toUpperCase(), id, reason, true);
        return res;
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
