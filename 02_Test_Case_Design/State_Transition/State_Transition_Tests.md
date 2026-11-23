# State Transition Testing
Automated: `StateMachineTest` (every valid/invalid pair), `ClaimServiceTest`, `RecoveryServiceTest`, `ApiWorkflowIT`.
Every successful transition also writes a `status_history` row (verify via the "Status history" panel on item detail, or the DB).

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

## Planned cases
| ID | Start | Event | Expected | Auto | Actual | Status |
|---|---|---|---|---|---|---|
| ST-L-01 | Lost REPORTED | moderator starts review | UNDER_REVIEW | ✔ | | |
| ST-L-02 | Lost REPORTED | confirm match directly | goes via UNDER_REVIEW to MATCHED | | | |
| ST-L-03 | Lost REPORTED | skip to RESOLVED | 409 | ✔ | | |
| ST-L-04 | Lost CLOSED | owner edits | 403 (R14) | ✔ | | |
| ST-L-05 | Lost CLOSED | withdraw again | 409 | ✔ | | |
| ST-F-01 | Found REPORTED | claim submitted | 422 (not available) | ✔ | | |
| ST-F-02 | Found AVAILABLE | claim submitted | CLAIM_PENDING | ✔ | | |
| ST-F-03 | Found CLAIM_PENDING | only claim rejected/cancelled | AVAILABLE | ✔ | | |
| ST-F-04 | Found CLAIM_PENDING | recovery completed | RETURNED | ✔ | | |
| ST-F-05 | Found RETURNED | new claim | 422 | ✔ | | |
| ST-C-01 | Claim PENDING | approve | 409 | ✔ | | |
| ST-C-02 | Claim UNDER_REVIEW | reject with reason | REJECTED, item released | ✔ | | |
| ST-C-03 | Claim REJECTED | approve | 409 | ✔ | | |
| ST-C-04 | Claim REJECTED | moderator reconsiders | 403 | ✔ | | |
| ST-C-05 | Claim REJECTED | admin reconsiders | UNDER_REVIEW | ✔ | | |
| ST-C-06 | Claim APPROVED | cancel | 409 | ✔ | | |
| ST-C-07 | Claim CANCELLED | any event | 409 | ✔ | | |
| ST-C-08 | Two open claims | one approved | other auto-REJECTED | | | |
| ST-R-01 | Recovery SCHEDULED | complete | COMPLETED + `REC-` code | ✔ | | |
| ST-R-02 | Recovery COMPLETED | complete | 409 | ✔ | | |
