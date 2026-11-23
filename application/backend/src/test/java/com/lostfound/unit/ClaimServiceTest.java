package com.lostfound.unit;

import com.lostfound.config.ValidationProperties;
import com.lostfound.domain.*;
import com.lostfound.domain.Enums.*;
import com.lostfound.dto.Dto.*;
import com.lostfound.exception.*;
import com.lostfound.repo.*;
import com.lostfound.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Decision-table and state-transition checks for the claim workflow (Mockito, no database). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClaimServiceTest {
    @Mock ClaimRepository claimRepo;
    @Mock ClaimEvidenceRepository evidenceRepo;
    @Mock FoundItemRepository foundRepo;
    @Mock LostItemRepository lostRepo;
    @Mock RecoveryRepository recoveryRepo;
    @Mock UserRepository userRepo;
    @Mock ActivityService activity;
    @Mock NotificationService notifications;
    @Mock Mapper mapper;
    ClaimService service;

    User finder = TestData.user(1, RoleName.ROLE_USER);
    User claimant = TestData.user(2, RoleName.ROLE_USER);
    User admin = TestData.user(9, RoleName.ROLE_ADMIN);
    User moderator = TestData.user(8, RoleName.ROLE_MODERATOR);

    @BeforeEach
    void setUp() {
        StatusService status = new StatusService(lostRepo, foundRepo, claimRepo, recoveryRepo, activity);
        service = new ClaimService(claimRepo, evidenceRepo, foundRepo, userRepo, status, activity, notifications,
                new ValidationService(new ValidationProperties(), Clock.systemDefaultZone()), mapper);
        when(claimRepo.save(any(Claim.class))).thenAnswer(i -> i.getArgument(0));
        when(foundRepo.save(any(FoundItem.class))).thenAnswer(i -> i.getArgument(0));
    }

    private ClaimRequest request(long itemId) {
        return new ClaimRequest(itemId, "This is my laptop, I bought it last year", "Sticker on the lid", null, List.of("Invoice photo"));
    }

    private FoundItem item(FoundStatus s) {
        FoundItem f = TestData.found(10, finder, s);
        when(foundRepo.findByIdForUpdate(10L)).thenReturn(Optional.of(f));
        return f;
    }

    private Claim claim(ClaimStatus s, FoundItem f) {
        Claim c = TestData.claim(5, f, claimant, s);
        when(claimRepo.findById(5L)).thenReturn(Optional.of(c));
        return c;
    }

    // ---------- submission ----------
    @Test
    void submit_availableItem_createsPendingClaimAndLocksItem() {
        FoundItem f = item(FoundStatus.AVAILABLE);
        service.submit(request(10), claimant);
        verify(claimRepo).save(argThat(c -> c.getStatus() == ClaimStatus.PENDING));
        assertEquals(FoundStatus.CLAIM_PENDING, f.getStatus());
    }

    @Test
    void submit_duplicateActiveClaim_isRejected() {
        item(FoundStatus.CLAIM_PENDING);
        when(claimRepo.existsByClaimantAndFoundItemAndStatusIn(any(), any(), any())).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> service.submit(request(10), claimant));
    }

    @Test
    void submit_alreadyReturnedItem_isRejected() {
        item(FoundStatus.RETURNED);
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> service.submit(request(10), claimant));
        assertTrue(ex.getMessage().contains("already been returned"));
    }

    @Test
    void submit_unverifiedItem_isRejected() {
        item(FoundStatus.REPORTED);
        assertThrows(BusinessRuleException.class, () -> service.submit(request(10), claimant));
    }

    @Test
    void submit_ownReport_isRejected() {
        item(FoundStatus.AVAILABLE);
        assertThrows(BusinessRuleException.class, () -> service.submit(request(10), finder));
    }

    @Test
    void submit_missingExplanation_failsValidation() {
        assertThrows(ValidationFailedException.class, () -> service.submit(new ClaimRequest(10L, "", "", null, null), claimant));
    }

    @Test
    void submit_unknownItem_isNotFound() {
        when(foundRepo.findByIdForUpdate(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.submit(request(99), claimant));
    }

    // ---------- approval decision table ----------
    @Test
    void approve_underReviewVerifiedAvailableItem_succeeds() {
        FoundItem f = item(FoundStatus.CLAIM_PENDING);
        Claim c = claim(ClaimStatus.UNDER_REVIEW, f);
        service.approve(5L, new DecisionRequest(true, "Verified with invoice"), admin);
        assertEquals(ClaimStatus.APPROVED, c.getStatus());
        assertTrue(c.isOwnershipVerified());
    }

    @Test
    void approve_withoutOwnershipVerification_isRejected() {
        FoundItem f = item(FoundStatus.CLAIM_PENDING);
        Claim c = claim(ClaimStatus.UNDER_REVIEW, f);
        assertThrows(BusinessRuleException.class, () -> service.approve(5L, new DecisionRequest(false, "n/a"), admin));
        assertThrows(BusinessRuleException.class, () -> service.approve(5L, new DecisionRequest(null, "n/a"), admin));
        assertEquals(ClaimStatus.UNDER_REVIEW, c.getStatus());
    }

    @Test
    void approve_returnedItem_isRejected() {
        FoundItem f = item(FoundStatus.RETURNED);
        claim(ClaimStatus.UNDER_REVIEW, f);
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> service.approve(5L, new DecisionRequest(true, "x"), admin));
        assertTrue(ex.getMessage().contains("already been returned"));
    }

    @Test
    void approve_unavailableItem_isRejected() {
        FoundItem f = item(FoundStatus.AVAILABLE);
        claim(ClaimStatus.UNDER_REVIEW, f);
        assertThrows(BusinessRuleException.class, () -> service.approve(5L, new DecisionRequest(true, "x"), admin));
    }

    @Test
    void approve_byModerator_isForbidden() {
        FoundItem f = item(FoundStatus.CLAIM_PENDING);
        claim(ClaimStatus.UNDER_REVIEW, f);
        assertThrows(AccessDeniedException.class, () -> service.approve(5L, new DecisionRequest(true, "x"), moderator));
    }

    @Test
    void approve_pendingClaim_isInvalidTransition() {
        FoundItem f = item(FoundStatus.CLAIM_PENDING);
        claim(ClaimStatus.PENDING, f);
        assertThrows(InvalidStateTransitionException.class, () -> service.approve(5L, new DecisionRequest(true, "x"), admin));
    }

    @Test
    void approve_rejectedClaim_cannotSkipReview() {
        FoundItem f = item(FoundStatus.CLAIM_PENDING);
        claim(ClaimStatus.REJECTED, f);
        assertThrows(InvalidStateTransitionException.class, () -> service.approve(5L, new DecisionRequest(true, "x"), admin));
    }

    // ---------- rejection / reconsideration ----------
    @Test
    void reject_withReason_releasesItemWhenNoOtherActiveClaims() {
        FoundItem f = item(FoundStatus.CLAIM_PENDING);
        Claim c = claim(ClaimStatus.UNDER_REVIEW, f);
        service.reject(5L, new DecisionRequest(false, "Could not describe the item"), admin);
        assertEquals(ClaimStatus.REJECTED, c.getStatus());
        assertEquals(FoundStatus.AVAILABLE, f.getStatus());
    }

    @Test
    void reject_withoutReason_failsValidation() {
        FoundItem f = item(FoundStatus.CLAIM_PENDING);
        claim(ClaimStatus.UNDER_REVIEW, f);
        assertThrows(ValidationFailedException.class, () -> service.reject(5L, new DecisionRequest(false, " "), admin));
    }

    @Test
    void reconsider_rejectedClaim_byAdmin_movesBackToUnderReview() {
        FoundItem f = item(FoundStatus.AVAILABLE);
        Claim c = claim(ClaimStatus.REJECTED, f);
        service.startReview(5L, admin);
        assertEquals(ClaimStatus.UNDER_REVIEW, c.getStatus());
        assertEquals(FoundStatus.CLAIM_PENDING, f.getStatus());
    }

    @Test
    void reconsider_rejectedClaim_byModerator_isForbidden() {
        FoundItem f = item(FoundStatus.AVAILABLE);
        claim(ClaimStatus.REJECTED, f);
        assertThrows(AccessDeniedException.class, () -> service.startReview(5L, moderator));
    }

    // ---------- cancellation ----------
    @Test
    void cancel_ownPendingClaim_succeeds() {
        FoundItem f = item(FoundStatus.CLAIM_PENDING);
        Claim c = claim(ClaimStatus.PENDING, f);
        service.cancel(5L, claimant);
        assertEquals(ClaimStatus.CANCELLED, c.getStatus());
    }

    @Test
    void cancel_someoneElsesClaim_isForbidden() {
        FoundItem f = item(FoundStatus.CLAIM_PENDING);
        claim(ClaimStatus.PENDING, f);
        assertThrows(AccessDeniedException.class, () -> service.cancel(5L, finder));
    }

    @Test
    void cancel_approvedClaim_isInvalidTransition() {
        FoundItem f = item(FoundStatus.CLAIM_PENDING);
        claim(ClaimStatus.APPROVED, f);
        assertThrows(InvalidStateTransitionException.class, () -> service.cancel(5L, claimant));
    }
}
