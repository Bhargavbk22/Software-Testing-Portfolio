# State Transition Testing — test cases and results

Evidence: automated run `mvn clean test`, 23 Nov 2026, 141 tests, 0 failures (`06_Test_Reports/surefire-report.html`).
Automated: `StateMachineTest` (valid/invalid pairs and terminal states), `ClaimServiceTest`, `RecoveryServiceTest`, `ApiWorkflowIT`.
Every successful transition also writes a `status_history` row (verify via the "Status history" panel on item detail, or the DB).
The automated tests do not assert the history rows, so check them manually and record what you see.

## Lost report
`REPORTED → UNDER_REVIEW → MATCHED → RESOLVED → CLOSED` (+ `REJECTED`, withdraw to `CLOSED`)
## Found report
`REPORTED → UNDER_REVIEW → AVAILABLE → CLAIM_PENDING → RETURNED` (+ `CLAIM_PENDING → AVAILABLE`, `REJECTED`)
## Claim
`PENDING → UNDER_REVIEW → APPROVED → HANDOVER_SCHEDULED → RETURNED`, `UNDER_REVIEW → REJECTED → UNDER_REVIEW (admin)`, `CANCELLED`
## Recovery
`SCHEDULED → COMPLETED | CANCELLED`

## Transition table (state × event) — claim
| From \ To | PENDING | UNDER_REVIEW | APPROVED | REJECTED | CANCELLED | HANDOVER_SCHEDULED | RETURNED |
|---|---|---|---|---|---|---|---|
| PENDING | – | ✔ | ✘ | ✘ | ✔ | ✘ | ✘ |
| UNDER_REVIEW | ✘ | – | ✔ | ✔ | ✔ | ✘ | ✘ |
| APPROVED | ✘ | ✘ | – | ✘ | ✘ | ✔ | ✘ |
| REJECTED | ✘ | ✔ (admin) | ✘ | – | ✘ | ✘ | ✘ |
| CANCELLED | ✘ | ✘ | ✘ | ✘ | – | ✘ | ✘ |
| HANDOVER_SCHEDULED | ✘ | ✘ | ✘ | ✘ | ✘ | – | ✔ |
| RETURNED | ✘ | ✘ | ✘ | ✘ | ✘ | ✘ | – |

Automated coverage of this table (`StateMachineTest`): all 7 allowed (✔) transitions are tested. Eight forbidden (✘) transitions are tested explicitly
(PENDING→APPROVED, PENDING→REJECTED, REJECTED→APPROVED, APPROVED→RETURNED, APPROVED→REJECTED, CANCELLED→PENDING, RETURNED→APPROVED, UNDER_REVIEW→RETURNED).
For the terminal states CANCELLED and RETURNED, `terminalStates_haveNoOutgoingTransitions` checks every outgoing cell. Result: all passed.
The remaining ✘ cells are not tested individually.

## Test cases and results
| ID | Start | Event | Expected | Auto | Actual | Status |
|---|---|---|---|---|---|---|
| ST-L-01 | Lost REPORTED | moderator starts review | UNDER_REVIEW | ✔ | `StateMachineTest.lost_validTransitions` (REPORTED→UNDER_REVIEW) passed. Tested at state-machine level; the API call for a lost report was not automated | Pass |
| ST-L-02 | Lost REPORTED | confirm match directly | goes via UNDER_REVIEW to MATCHED | | | |
| ST-L-03 | Lost REPORTED | skip to RESOLVED | 409 | ✔ | `StateMachineTest.lost_invalidTransitions` (REPORTED→RESOLVED) passed; invalid transition raised (mapped to 409 by the exception handler, shown in `ApiWorkflowIT`) | Pass |
| ST-L-04 | Lost CLOSED | owner edits | 403 (R14) | ✔ | `ApiWorkflowIT.withdrawnLostReport_cannotBeModifiedByOwner` passed; edit after withdrawal returned 403 | Pass |
| ST-L-05 | Lost CLOSED | withdraw again | 409 | ✔ | Same test passed; second withdraw returned 409 | Pass |
| ST-F-01 | Found REPORTED | claim submitted | 422 (not available) | ✔ | `ClaimServiceTest.submit_unverifiedItem_isRejected` passed; `ApiWorkflowIT.fullWorkflow_...` returned 422 before verification | Pass |
| ST-F-02 | Found AVAILABLE | claim submitted | CLAIM_PENDING | ✔ | `submit_availableItem_createsPendingClaimAndLocksItem` passed; item became CLAIM_PENDING | Pass |
| ST-F-03 | Found CLAIM_PENDING | only claim rejected/cancelled | AVAILABLE | ✔ | `reject_withReason_releasesItemWhenNoOtherActiveClaims` passed; item returned to AVAILABLE after rejection. The cancel path does not assert the item status | Pass (rejection path) |
| ST-F-04 | Found CLAIM_PENDING | recovery completed | RETURNED | ✔ | `RecoveryServiceTest.complete_scheduledHandover_returnsItemAndIssuesConfirmation` passed; `fullWorkflow_...` showed the item as RETURNED | Pass |
| ST-F-05 | Found RETURNED | new claim | 422 | ✔ | `submit_alreadyReturnedItem_isRejected` passed; `fullWorkflow_...` returned 422 for a claim on the returned item | Pass |
| ST-C-01 | Claim PENDING | approve | 409 | ✔ | `approve_pendingClaim_isInvalidTransition` passed; `fullWorkflow_...` returned 409 | Pass |
| ST-C-02 | Claim UNDER_REVIEW | reject with reason | REJECTED, item released | ✔ | `reject_withReason_releasesItemWhenNoOtherActiveClaims` passed; claim REJECTED and item AVAILABLE | Pass |
| ST-C-03 | Claim REJECTED | approve | 409 | ✔ | `approve_rejectedClaim_cannotSkipReview` and `ApiWorkflowIT.rejectedClaim_cannotBeApprovedDirectly_butAdminMayReconsider` passed; 409 | Pass |
| ST-C-04 | Claim REJECTED | moderator reconsiders | 403 | ✔ | `reconsider_rejectedClaim_byModerator_isForbidden` and the integration test passed; 403 | Pass |
| ST-C-05 | Claim REJECTED | admin reconsiders | UNDER_REVIEW | ✔ | `reconsider_rejectedClaim_byAdmin_movesBackToUnderReview` and the integration test passed; status UNDER_REVIEW | Pass |
| ST-C-06 | Claim APPROVED | cancel | 409 | ✔ | `cancel_approvedClaim_isInvalidTransition` passed | Pass |
| ST-C-07 | Claim CANCELLED | any event | 409 | ✔ | `StateMachineTest.terminalStates_haveNoOutgoingTransitions` and `claim_invalidTransitions` (CANCELLED→PENDING) passed. Tested at state-machine level | Pass |
| ST-C-08 | Two open claims | one approved | other auto-REJECTED | | | |
| ST-R-01 | Recovery SCHEDULED | complete | COMPLETED + `REC-` code | ✔ | `complete_scheduledHandover_returnsItemAndIssuesConfirmation` passed; `fullWorkflow_...` confirmed a code starting with `REC-` | Pass |
| ST-R-02 | Recovery COMPLETED | complete | 409 | ✔ | `complete_twice_isInvalidTransition` passed; `fullWorkflow_...` returned 409 | Pass |