package com.lostfound.service;

import com.lostfound.domain.*;
import com.lostfound.domain.Enums.*;
import com.lostfound.dto.Dto.*;
import com.lostfound.exception.BusinessRuleException;
import com.lostfound.exception.DuplicateResourceException;
import com.lostfound.exception.ResourceNotFoundException;
import com.lostfound.exception.ValidationFailedException;
import com.lostfound.repo.ClaimEvidenceRepository;
import com.lostfound.repo.ClaimRepository;
import com.lostfound.repo.FoundItemRepository;
import com.lostfound.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClaimService {
    /** Claim statuses that count as "active" (used for duplicate prevention and item release). */
    public static final List<ClaimStatus> ACTIVE = List.of(ClaimStatus.PENDING, ClaimStatus.UNDER_REVIEW, ClaimStatus.APPROVED, ClaimStatus.HANDOVER_SCHEDULED);

    private final ClaimRepository claimRepo;
    private final ClaimEvidenceRepository evidenceRepo;
    private final FoundItemRepository foundRepo;
    private final UserRepository userRepo;
    private final StatusService status;
    private final ActivityService activity;
    private final NotificationService notifications;
    private final ValidationService validation;
    private final Mapper mapper;

    private Claim find(Long id) { return claimRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Claim not found")); }

    private void requireAdmin(User u) {
        if (u == null || !u.admin()) throw new AccessDeniedException("Only administrators can approve or reject claims");
    }

    @Transactional
    public ClaimResponse submit(ClaimRequest r, User user) {
        validation.validateClaim(r);
        FoundItem f = foundRepo.findByIdForUpdate(r.foundItemId()).orElseThrow(() -> new ResourceNotFoundException("Found item not found"));
        if (f.getStatus() == FoundStatus.RETURNED) throw new BusinessRuleException("This item has already been returned and cannot be claimed again");
        if (f.getStatus() != FoundStatus.AVAILABLE && f.getStatus() != FoundStatus.CLAIM_PENDING) throw new BusinessRuleException("This item is not available for claims");
        if (f.getReporter().getId().equals(user.getId())) throw new BusinessRuleException("You cannot claim an item that you reported as found");
        if (claimRepo.existsByClaimantAndFoundItemAndStatusIn(user, f, ACTIVE)) throw new DuplicateResourceException("You already have an active claim for this item");

        Claim c = new Claim();
        c.setFoundItem(f);
        c.setClaimant(user);
        c.setOwnershipExplanation(r.ownershipExplanation().trim());
        c.setDistinguishingCharacteristics(r.distinguishingCharacteristics().trim());
        c.setAdditionalDetails(r.additionalDetails() == null || r.additionalDetails().isBlank() ? null : r.additionalDetails().trim());
        c.setStatus(ClaimStatus.PENDING);
        claimRepo.save(c);
        if (r.evidence() != null) r.evidence().stream().filter(e -> e != null && !e.isBlank()).forEach(e -> evidenceRepo.save(new ClaimEvidence(c, e.trim())));
        status.initial("CLAIM", c.getId(), "PENDING", user);
        if (f.getStatus() == FoundStatus.AVAILABLE) status.found(f, FoundStatus.CLAIM_PENDING, user, "Claim submitted");
        notifications.notify(user, NotificationType.CLAIM_SUBMITTED, "Your claim for \"" + f.getName() + "\" was submitted and is pending review.", "CLAIM", c.getId());
        notifications.notifyRole(RoleName.ROLE_ADMIN, NotificationType.CLAIM_SUBMITTED, "New ownership claim for \"" + f.getName() + "\" by " + user.getFullName(), "CLAIM", c.getId());
        activity.log(user, "CLAIM_SUBMITTED", "CLAIM", c.getId(), f.getName());
        return mapper.claim(c, false);
    }

    /** Moves PENDING -> UNDER_REVIEW (admin or moderator escalation) or REJECTED -> UNDER_REVIEW (admin reconsideration only). */
    @Transactional
    public ClaimResponse startReview(Long id, User staff) {
        Claim c = find(id);
        if (c.getStatus() == ClaimStatus.REJECTED) {
            if (!staff.admin()) throw new AccessDeniedException("Only an administrator can reconsider a rejected claim");
            FoundItem f = foundRepo.findByIdForUpdate(c.getFoundItem().getId()).orElseThrow(() -> new ResourceNotFoundException("Found item not found"));
            if (f.getStatus() == FoundStatus.RETURNED) throw new BusinessRuleException("The item was already returned; the claim cannot be reconsidered");
            if (f.getStatus() == FoundStatus.AVAILABLE) status.found(f, FoundStatus.CLAIM_PENDING, staff, "Claim reconsidered");
            else if (f.getStatus() != FoundStatus.CLAIM_PENDING) throw new BusinessRuleException("The item is unavailable; the claim cannot be reconsidered");
        }
        status.claim(c, ClaimStatus.UNDER_REVIEW, staff, staff.admin() ? "Review started by administrator" : "Escalated by moderator");
        notifications.notify(c.getClaimant(), NotificationType.GENERAL, "Your claim for \"" + c.getFoundItem().getName() + "\" is now under review.", "CLAIM", c.getId());
        activity.log(staff, "CLAIM_REVIEW_STARTED", "CLAIM", c.getId(), null);
        return mapper.claim(c, true);
    }

    @Transactional
    public ClaimResponse approve(Long id, DecisionRequest d, User admin) {
        requireAdmin(admin);
        Claim c = find(id);
        StateMachine.checkClaim(c.getStatus(), ClaimStatus.APPROVED);                      // must be UNDER_REVIEW
        FoundItem f = foundRepo.findByIdForUpdate(c.getFoundItem().getId()).orElseThrow(() -> new ResourceNotFoundException("Found item not found"));
        if (f.getStatus() == FoundStatus.RETURNED) throw new BusinessRuleException("The item has already been returned; the claim cannot be approved");
        if (f.getStatus() != FoundStatus.CLAIM_PENDING) throw new BusinessRuleException("The item is unavailable; the claim cannot be approved");
        if (d == null || !Boolean.TRUE.equals(d.ownershipVerified())) throw new BusinessRuleException("Ownership must be verified before a claim can be approved");

        status.claim(c, ClaimStatus.APPROVED, admin, d.reason());
        c.setOwnershipVerified(true);
        c.setReviewedBy(admin);
        c.setDecisionReason(d.reason());
        claimRepo.save(c);
        for (Claim other : claimRepo.findByFoundItemAndStatusIn(f, List.of(ClaimStatus.PENDING, ClaimStatus.UNDER_REVIEW))) {
            if (!other.getId().equals(c.getId())) autoReject(other, admin);
        }
        notifications.notify(c.getClaimant(), NotificationType.CLAIM_APPROVED, "Your claim for \"" + f.getName() + "\" was approved. A handover will be scheduled.", "CLAIM", c.getId());
        activity.log(admin, "CLAIM_APPROVED", "CLAIM", c.getId(), d.reason());
        return mapper.claim(c, true);
    }

    private void autoReject(Claim other, User admin) {
        if (other.getStatus() == ClaimStatus.PENDING) status.claim(other, ClaimStatus.UNDER_REVIEW, admin, "Automatic review");
        status.claim(other, ClaimStatus.REJECTED, admin, "Automatically rejected: another claim was approved");
        other.setDecisionReason("Another claim for this item was approved");
        other.setReviewedBy(admin);
        claimRepo.save(other);
        notifications.notify(other.getClaimant(), NotificationType.CLAIM_REJECTED, "Your claim for \"" + other.getFoundItem().getName() + "\" was not successful because another claim was approved.", "CLAIM", other.getId());
    }

    @Transactional
    public ClaimResponse reject(Long id, DecisionRequest d, User admin) {
        requireAdmin(admin);
        if (d == null || d.reason() == null || d.reason().isBlank()) throw new ValidationFailedException(Map.of("reason", "A rejection reason is required"));
        Claim c = find(id);
        status.claim(c, ClaimStatus.REJECTED, admin, d.reason());
        c.setOwnershipVerified(false);
        c.setReviewedBy(admin);
        c.setDecisionReason(d.reason().trim());
        claimRepo.save(c);
        releaseItem(c.getFoundItem(), admin);
        notifications.notify(c.getClaimant(), NotificationType.CLAIM_REJECTED, "Your claim for \"" + c.getFoundItem().getName() + "\" was rejected: " + d.reason().trim(), "CLAIM", c.getId());
        activity.log(admin, "CLAIM_REJECTED", "CLAIM", c.getId(), d.reason(), true);
        return mapper.claim(c, true);
    }

    @Transactional
    public ClaimResponse requestEvidence(Long id, String message, User admin) {
        requireAdmin(admin);
        if (message == null || message.isBlank()) throw new ValidationFailedException(Map.of("message", "Please describe what evidence is needed"));
        Claim c = find(id);
        if (c.getStatus() != ClaimStatus.PENDING && c.getStatus() != ClaimStatus.UNDER_REVIEW) throw new BusinessRuleException("Evidence can only be requested for active claims under review");
        notifications.notify(c.getClaimant(), NotificationType.EVIDENCE_REQUESTED, "Additional evidence requested for \"" + c.getFoundItem().getName() + "\": " + message.trim(), "CLAIM", c.getId());
        activity.log(admin, "EVIDENCE_REQUESTED", "CLAIM", c.getId(), message);
        return mapper.claim(c, true);
    }

    @Transactional
    public ClaimResponse addEvidence(Long id, String message, User user) {
        Claim c = find(id);
        if (!c.getClaimant().getId().equals(user.getId())) throw new AccessDeniedException("You can only add evidence to your own claims");
        if (message == null || message.isBlank()) throw new ValidationFailedException(Map.of("message", "Evidence description is required"));
        if (c.getStatus() != ClaimStatus.PENDING && c.getStatus() != ClaimStatus.UNDER_REVIEW) throw new BusinessRuleException("Evidence can only be added while the claim is being reviewed");
        evidenceRepo.save(new ClaimEvidence(c, message.trim()));
        notifications.notifyRole(RoleName.ROLE_ADMIN, NotificationType.GENERAL, user.getFullName() + " added evidence to a claim for \"" + c.getFoundItem().getName() + "\"", "CLAIM", c.getId());
        activity.log(user, "EVIDENCE_ADDED", "CLAIM", c.getId(), null);
        return mapper.claim(c, false);
    }

    @Transactional
    public ClaimResponse cancel(Long id, User user) {
        Claim c = find(id);
        if (!c.getClaimant().getId().equals(user.getId())) throw new AccessDeniedException("You can only cancel your own claims");
        status.claim(c, ClaimStatus.CANCELLED, user, "Cancelled by claimant");
        releaseItem(c.getFoundItem(), user);
        activity.log(user, "CLAIM_CANCELLED", "CLAIM", c.getId(), null);
        return mapper.claim(c, false);
    }

    /** When no active claim remains, a CLAIM_PENDING item goes back to AVAILABLE. */
    private void releaseItem(FoundItem f, User by) {
        if (f.getStatus() == FoundStatus.CLAIM_PENDING && !claimRepo.existsByFoundItemAndStatusIn(f, ACTIVE)) {
            status.found(f, FoundStatus.AVAILABLE, by, "No active claims remain");
        }
    }

    @Transactional(readOnly = true)
    public Page<Claim> myClaims(User user, int page, int size) {
        return claimRepo.findByClaimant(user, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional(readOnly = true)
    public Page<Claim> adminList(ClaimStatus st, int page, int size) {
        Pageable p = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return st == null ? claimRepo.findAll(p) : claimRepo.findByStatus(st, p);
    }

    @Transactional(readOnly = true)
    public ClaimResponse get(Long id, User viewer) {
        Claim c = find(id);
        if (!viewer.staff() && !c.getClaimant().getId().equals(viewer.getId())) throw new AccessDeniedException("You cannot view this claim");
        return mapper.claim(c, viewer.staff());
    }

    @Transactional(readOnly = true)
    public List<Claim> needingReview() {
        return claimRepo.findTop10ByStatusInOrderByCreatedAtDesc(List.of(ClaimStatus.PENDING, ClaimStatus.UNDER_REVIEW));
    }
}
