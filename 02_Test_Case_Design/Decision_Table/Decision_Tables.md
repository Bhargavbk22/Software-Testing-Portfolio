# Decision Tables
## DT-1 Claim approval (rule R8, R10, R12)
Conditions: **C1** claim is UNDER_REVIEW · **C2** item is CLAIM_PENDING (available) · **C3** ownership verified · **C4** actor is admin

| Rule | C1 | C2 | C3 | C4 | Outcome | HTTP | Test |
|---|---|---|---|---|---|---|---|
| DT1-R1 | Y | Y | Y | Y | Approved | 200 | `approve_underReviewVerifiedAvailableItem_succeeds` |
| DT1-R2 | Y | Y | N | Y | Rejected: ownership not verified | 422 | `approve_withoutOwnershipVerification_isRejected` |
| DT1-R3 | Y | N (returned) | Y | Y | Rejected: item returned | 422 | `approve_returnedItem_isRejected` |
| DT1-R4 | Y | N (available/other) | Y | Y | Rejected: item unavailable | 422 | `approve_unavailableItem_isRejected` |
| DT1-R5 | N (PENDING/REJECTED) | Y | Y | Y | Invalid transition | 409 | `approve_pendingClaim_isInvalidTransition`, `approve_rejectedClaim_cannotSkipReview` |
| DT1-R6 | Y | Y | Y | N | Forbidden | 403 | `approve_byModerator_isForbidden` |

## DT-2 Found report approval by moderator
Conditions: **C1** status UNDER_REVIEW · **C2** mandatory info complete · **C3** reason supplied (reject only)

| Rule | C1 | C2 | Action | Outcome | HTTP |
|---|---|---|---|---|---|
| DT2-R1 | Y | Y | approve | AVAILABLE, verified | 200 (`foundReport_isNotPublic_untilVerified...`) |
| DT2-R2 | N (REPORTED) | Y | approve | invalid transition | 409 |
| DT2-R3 | Y | N | approve | mandatory info incomplete | 422 |
| DT2-R4 | Y/REPORTED | – | reject with reason | REJECTED | 200 |
| DT2-R5 | Y/REPORTED | – | reject without reason | validation error | 400 |

## DT-3 Access control (rule R11)
| Rule | Authenticated | Role | Endpoint class | Outcome | Test |
|---|---|---|---|---|---|
| DT3-R1 | N | – | any protected | 401 | `protectedEndpoint_withoutToken_returns401` |
| DT3-R2 | Y (invalid/expired token) | – | any protected | 401 | `protectedEndpoint_withGarbageToken_returns401` |
| DT3-R3 | Y | Student | `/api/admin/**`, `/api/moderator/**` | 403 | `adminEndpoints_enforceRoles` |
| DT3-R4 | Y | Moderator | `/api/admin/**`, `/api/recoveries` | 403 | same |
| DT3-R5 | Y | Moderator | `/api/moderator/**` | 200 | same |
| DT3-R6 | Y | Admin | all | 200 | same |

## DT-4 Item handover (rules R9, R10)
Conditions: **C1** claim status · **C2** ownership verified · **C3** recovery already exists

| Rule | C1 | C2 | C3 | Action | Outcome | HTTP | Test |
|---|---|---|---|---|---|---|---|
| DT4-R1 | APPROVED | Y | N | schedule | HANDOVER_SCHEDULED | 201 | `schedule_approvedVerifiedClaim_schedulesHandover` |
| DT4-R2 | UNDER_REVIEW / other | N | N | schedule | blocked | 422 | `schedule_claimNotApproved_isRejected` |
| DT4-R3 | HANDOVER_SCHEDULED | Y | Y | schedule | duplicate | 409 | `schedule_duplicateRecovery_isRejected` |
| DT4-R4 | HANDOVER_SCHEDULED | Y | Y | complete | RETURNED + code | 200 | `complete_scheduledHandover_returnsItem...` |
| DT4-R5 | APPROVED (not scheduled) | Y | Y | complete | blocked | 422 | `complete_withoutScheduledHandover_isRejected` |
| DT4-R6 | RETURNED | Y | Y | complete again | invalid transition | 409 | `complete_twice_isInvalidTransition` |
