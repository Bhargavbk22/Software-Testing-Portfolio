package com.lostfound.service;

import com.lostfound.domain.Enums.*;
import com.lostfound.exception.InvalidStateTransitionException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Single source of truth for every allowed status transition (see documentation/Business_Rules.md). */
public final class StateMachine {
    private StateMachine() {}

    private static final Map<LostStatus, Set<LostStatus>> LOST = new EnumMap<>(LostStatus.class);
    private static final Map<FoundStatus, Set<FoundStatus>> FOUND = new EnumMap<>(FoundStatus.class);
    private static final Map<ClaimStatus, Set<ClaimStatus>> CLAIM = new EnumMap<>(ClaimStatus.class);
    private static final Map<RecoveryStatus, Set<RecoveryStatus>> RECOVERY = new EnumMap<>(RecoveryStatus.class);

    static {
        LOST.put(LostStatus.REPORTED, EnumSet.of(LostStatus.UNDER_REVIEW, LostStatus.CLOSED, LostStatus.REJECTED));
        LOST.put(LostStatus.UNDER_REVIEW, EnumSet.of(LostStatus.MATCHED, LostStatus.CLOSED, LostStatus.REJECTED));
        LOST.put(LostStatus.MATCHED, EnumSet.of(LostStatus.RESOLVED, LostStatus.UNDER_REVIEW, LostStatus.CLOSED));
        LOST.put(LostStatus.RESOLVED, EnumSet.of(LostStatus.CLOSED));
        LOST.put(LostStatus.CLOSED, EnumSet.noneOf(LostStatus.class));
        LOST.put(LostStatus.REJECTED, EnumSet.noneOf(LostStatus.class));

        FOUND.put(FoundStatus.REPORTED, EnumSet.of(FoundStatus.UNDER_REVIEW, FoundStatus.REJECTED));
        FOUND.put(FoundStatus.UNDER_REVIEW, EnumSet.of(FoundStatus.AVAILABLE, FoundStatus.REJECTED));
        FOUND.put(FoundStatus.AVAILABLE, EnumSet.of(FoundStatus.CLAIM_PENDING));
        FOUND.put(FoundStatus.CLAIM_PENDING, EnumSet.of(FoundStatus.AVAILABLE, FoundStatus.RETURNED));
        FOUND.put(FoundStatus.RETURNED, EnumSet.noneOf(FoundStatus.class));
        FOUND.put(FoundStatus.REJECTED, EnumSet.noneOf(FoundStatus.class));

        CLAIM.put(ClaimStatus.PENDING, EnumSet.of(ClaimStatus.UNDER_REVIEW, ClaimStatus.CANCELLED));
        CLAIM.put(ClaimStatus.UNDER_REVIEW, EnumSet.of(ClaimStatus.APPROVED, ClaimStatus.REJECTED, ClaimStatus.CANCELLED));
        CLAIM.put(ClaimStatus.APPROVED, EnumSet.of(ClaimStatus.HANDOVER_SCHEDULED));
        CLAIM.put(ClaimStatus.HANDOVER_SCHEDULED, EnumSet.of(ClaimStatus.RETURNED));
        // A rejected claim can only be reconsidered (back to UNDER_REVIEW) by an administrator; never straight to APPROVED.
        CLAIM.put(ClaimStatus.REJECTED, EnumSet.of(ClaimStatus.UNDER_REVIEW));
        CLAIM.put(ClaimStatus.CANCELLED, EnumSet.noneOf(ClaimStatus.class));   // terminal
        CLAIM.put(ClaimStatus.RETURNED, EnumSet.noneOf(ClaimStatus.class));    // terminal

        RECOVERY.put(RecoveryStatus.SCHEDULED, EnumSet.of(RecoveryStatus.COMPLETED, RecoveryStatus.CANCELLED));
        RECOVERY.put(RecoveryStatus.COMPLETED, EnumSet.noneOf(RecoveryStatus.class));
        RECOVERY.put(RecoveryStatus.CANCELLED, EnumSet.noneOf(RecoveryStatus.class));
    }

    public static boolean canLost(LostStatus from, LostStatus to) { return LOST.get(from).contains(to); }
    public static boolean canFound(FoundStatus from, FoundStatus to) { return FOUND.get(from).contains(to); }
    public static boolean canClaim(ClaimStatus from, ClaimStatus to) { return CLAIM.get(from).contains(to); }
    public static boolean canRecovery(RecoveryStatus from, RecoveryStatus to) { return RECOVERY.get(from).contains(to); }

    public static void checkLost(LostStatus from, LostStatus to) { if (!canLost(from, to)) throw new InvalidStateTransitionException("Lost report", from.name(), to.name()); }
    public static void checkFound(FoundStatus from, FoundStatus to) { if (!canFound(from, to)) throw new InvalidStateTransitionException("Found report", from.name(), to.name()); }
    public static void checkClaim(ClaimStatus from, ClaimStatus to) { if (!canClaim(from, to)) throw new InvalidStateTransitionException("Claim", from.name(), to.name()); }
    public static void checkRecovery(RecoveryStatus from, RecoveryStatus to) { if (!canRecovery(from, to)) throw new InvalidStateTransitionException("Recovery", from.name(), to.name()); }
}
