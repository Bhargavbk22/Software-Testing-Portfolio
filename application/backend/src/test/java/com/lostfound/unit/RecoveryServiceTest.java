package com.lostfound.unit;

import com.lostfound.domain.*;
import com.lostfound.domain.Enums.*;
import com.lostfound.dto.Dto.RecoveryRequest;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecoveryServiceTest {
    @Mock RecoveryRepository recoveryRepo;
    @Mock ClaimRepository claimRepo;
    @Mock FoundItemRepository foundRepo;
    @Mock LostItemRepository lostRepo;
    @Mock ItemMatchRepository matchRepo;
    @Mock ActivityService activity;
    @Mock NotificationService notifications;
    @Mock Mapper mapper;
    RecoveryService service;

    User admin = TestData.user(9, RoleName.ROLE_ADMIN);
    User finder = TestData.user(1, RoleName.ROLE_USER);
    User claimant = TestData.user(2, RoleName.ROLE_USER);
    FoundItem item;

    @BeforeEach
    void setUp() {
        StatusService status = new StatusService(lostRepo, foundRepo, claimRepo, recoveryRepo, activity);
        service = new RecoveryService(recoveryRepo, claimRepo, foundRepo, matchRepo, status, activity, notifications, mapper);
        when(recoveryRepo.save(any(Recovery.class))).thenAnswer(i -> i.getArgument(0));
        when(claimRepo.save(any(Claim.class))).thenAnswer(i -> i.getArgument(0));
        when(foundRepo.save(any(FoundItem.class))).thenAnswer(i -> i.getArgument(0));
        item = TestData.found(10, finder, FoundStatus.CLAIM_PENDING);
        when(foundRepo.findByIdForUpdate(10L)).thenReturn(Optional.of(item));
    }

    private Claim claim(ClaimStatus s, boolean verified) {
        Claim c = TestData.claim(5, item, claimant, s);
        c.setOwnershipVerified(verified);
        when(claimRepo.findById(5L)).thenReturn(Optional.of(c));
        return c;
    }

    private RecoveryRequest request() { return new RecoveryRequest(5L, LocalDateTime.now().plusDays(1), "Security Office"); }

    private Recovery recovery(Claim c, RecoveryStatus s) {
        Recovery r = new Recovery();
        r.setId(3L);
        r.setClaim(c);
        r.setStatus(s);
        r.setLocation("Security Office");
        when(recoveryRepo.findById(3L)).thenReturn(Optional.of(r));
        return r;
    }

    @Test
    void schedule_approvedVerifiedClaim_schedulesHandover() {
        Claim c = claim(ClaimStatus.APPROVED, true);
        service.schedule(request(), admin);
        assertEquals(ClaimStatus.HANDOVER_SCHEDULED, c.getStatus());
    }

    @Test
    void schedule_claimNotApproved_isRejected() {
        claim(ClaimStatus.UNDER_REVIEW, false);
        assertThrows(BusinessRuleException.class, () -> service.schedule(request(), admin));
    }

    @Test
    void schedule_duplicateRecovery_isRejected() {
        Claim c = claim(ClaimStatus.HANDOVER_SCHEDULED, true);
        when(recoveryRepo.existsByClaim(c)).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> service.schedule(request(), admin));
    }

    @Test
    void schedule_missingFields_failValidation() {
        assertThrows(ValidationFailedException.class, () -> service.schedule(new RecoveryRequest(null, null, ""), admin));
    }

    @Test
    void complete_scheduledHandover_returnsItemAndIssuesConfirmation() {
        Claim c = claim(ClaimStatus.HANDOVER_SCHEDULED, true);
        Recovery r = recovery(c, RecoveryStatus.SCHEDULED);
        service.complete(3L, admin);
        assertEquals(FoundStatus.RETURNED, item.getStatus());
        assertEquals(ClaimStatus.RETURNED, c.getStatus());
        assertEquals(RecoveryStatus.COMPLETED, r.getStatus());
        assertTrue(r.getConfirmationCode().startsWith("REC-"));
        assertNotNull(r.getHandoverDate());
    }

    @Test
    void complete_withoutScheduledHandover_isRejected() {
        Claim c = claim(ClaimStatus.APPROVED, true);
        recovery(c, RecoveryStatus.SCHEDULED);
        assertThrows(BusinessRuleException.class, () -> service.complete(3L, admin));
        assertEquals(FoundStatus.CLAIM_PENDING, item.getStatus());
    }

    @Test
    void complete_twice_isInvalidTransition() {
        Claim c = claim(ClaimStatus.RETURNED, true);
        recovery(c, RecoveryStatus.COMPLETED);
        assertThrows(InvalidStateTransitionException.class, () -> service.complete(3L, admin));
    }

    @Test
    void complete_unknownRecovery_isNotFound() {
        when(recoveryRepo.findById(77L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.complete(77L, admin));
    }
}
