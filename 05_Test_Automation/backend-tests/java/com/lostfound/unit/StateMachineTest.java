package com.lostfound.unit;

import com.lostfound.domain.Enums.*;
import com.lostfound.exception.InvalidStateTransitionException;
import com.lostfound.service.StateMachine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/** State Transition Testing: valid transitions, invalid transitions and terminal states. */
class StateMachineTest {

    @ParameterizedTest(name = "lost {0} -> {1} allowed")
    @CsvSource({"REPORTED,UNDER_REVIEW", "UNDER_REVIEW,MATCHED", "MATCHED,RESOLVED", "RESOLVED,CLOSED", "REPORTED,CLOSED"})
    void lost_validTransitions(LostStatus from, LostStatus to) { assertDoesNotThrow(() -> StateMachine.checkLost(from, to)); }

    @ParameterizedTest(name = "lost {0} -> {1} rejected")
    @CsvSource({"REPORTED,MATCHED", "REPORTED,RESOLVED", "CLOSED,REPORTED", "REJECTED,UNDER_REVIEW", "RESOLVED,MATCHED", "REPORTED,REPORTED"})
    void lost_invalidTransitions(LostStatus from, LostStatus to) { assertThrows(InvalidStateTransitionException.class, () -> StateMachine.checkLost(from, to)); }

    @ParameterizedTest(name = "found {0} -> {1} allowed")
    @CsvSource({"REPORTED,UNDER_REVIEW", "UNDER_REVIEW,AVAILABLE", "AVAILABLE,CLAIM_PENDING", "CLAIM_PENDING,RETURNED", "CLAIM_PENDING,AVAILABLE"})
    void found_validTransitions(FoundStatus from, FoundStatus to) { assertDoesNotThrow(() -> StateMachine.checkFound(from, to)); }

    @ParameterizedTest(name = "found {0} -> {1} rejected")
    @CsvSource({"REPORTED,AVAILABLE", "AVAILABLE,RETURNED", "RETURNED,AVAILABLE", "RETURNED,CLAIM_PENDING", "REJECTED,AVAILABLE"})
    void found_invalidTransitions(FoundStatus from, FoundStatus to) { assertThrows(InvalidStateTransitionException.class, () -> StateMachine.checkFound(from, to)); }

    @ParameterizedTest(name = "claim {0} -> {1} allowed")
    @CsvSource({"PENDING,UNDER_REVIEW", "PENDING,CANCELLED", "UNDER_REVIEW,APPROVED", "UNDER_REVIEW,REJECTED", "APPROVED,HANDOVER_SCHEDULED",
            "HANDOVER_SCHEDULED,RETURNED", "REJECTED,UNDER_REVIEW"})
    void claim_validTransitions(ClaimStatus from, ClaimStatus to) { assertDoesNotThrow(() -> StateMachine.checkClaim(from, to)); }

    @ParameterizedTest(name = "claim {0} -> {1} rejected")
    @CsvSource({"PENDING,APPROVED", "PENDING,REJECTED", "REJECTED,APPROVED", "APPROVED,RETURNED", "APPROVED,REJECTED", "CANCELLED,PENDING",
            "RETURNED,APPROVED", "UNDER_REVIEW,RETURNED"})
    void claim_invalidTransitions(ClaimStatus from, ClaimStatus to) { assertThrows(InvalidStateTransitionException.class, () -> StateMachine.checkClaim(from, to)); }

    @Test
    void recovery_transitions() {
        assertDoesNotThrow(() -> StateMachine.checkRecovery(RecoveryStatus.SCHEDULED, RecoveryStatus.COMPLETED));
        assertThrows(InvalidStateTransitionException.class, () -> StateMachine.checkRecovery(RecoveryStatus.COMPLETED, RecoveryStatus.COMPLETED));
        assertThrows(InvalidStateTransitionException.class, () -> StateMachine.checkRecovery(RecoveryStatus.COMPLETED, RecoveryStatus.SCHEDULED));
    }

    @Test
    void terminalStates_haveNoOutgoingTransitions() {
        for (LostStatus t : new LostStatus[]{LostStatus.CLOSED, LostStatus.REJECTED})
            for (LostStatus to : LostStatus.values()) assertFalse(StateMachine.canLost(t, to), t + "->" + to);
        for (FoundStatus t : new FoundStatus[]{FoundStatus.RETURNED, FoundStatus.REJECTED})
            for (FoundStatus to : FoundStatus.values()) assertFalse(StateMachine.canFound(t, to), t + "->" + to);
        for (ClaimStatus t : new ClaimStatus[]{ClaimStatus.CANCELLED, ClaimStatus.RETURNED})
            for (ClaimStatus to : ClaimStatus.values()) assertFalse(StateMachine.canClaim(t, to), t + "->" + to);
    }

    @Test
    void errorMessage_namesEntityAndStates() {
        InvalidStateTransitionException ex = assertThrows(InvalidStateTransitionException.class, () -> StateMachine.checkClaim(ClaimStatus.REJECTED, ClaimStatus.APPROVED));
        assertEquals("Claim cannot change status from REJECTED to APPROVED", ex.getMessage());
    }
}
