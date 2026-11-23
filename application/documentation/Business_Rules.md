# Business Rules and Configurable Limits
Basis for every test design in `testing/`. Limits live in `application.yml` under `app.validation.*`.

## Validation limits (defaults)
| Key | Default | Rule |
|---|---|---|
| password-min/max-length | 8 / 64 | plus upper, lower, digit, special character |
| name-min/max-length | 2 / 100 | full name |
| item-name-min/max-length | 2 / 100 | item name |
| description-min/max-length | 10 / 1000 | report description |
| claim-explanation-min/max-length | 20 / 1000 | claim ownership explanation |
| location-max-length | 150 | |
| date-max-age-days | 730 | date must be ≤ today and ≥ today − 730 days |
| image-max-size-bytes | 2 097 152 | per image |
| max-images-per-item | 3 | |
| allowed-image-types / extensions | jpeg, png, webp / jpg, jpeg, png, webp | both content type and extension are checked |
| phone-pattern | `^\+?[0-9]{10,15}$` | |
| student-id-pattern | `^[A-Za-z0-9]{5,20}$` | unique |
| matching.min-score | 50 | threshold to create a suggested match |

## The 14 mandatory rules
| # | Rule | Enforced in | HTTP |
|---|---|---|---|
| 1 | Missing mandatory fields rejected | `ValidationService` | 400 |
| 2 | Duplicate email rejected | `AuthService.register` | 409 |
| 3 | Invalid login rejected | `AuthService.login` | 401 |
| 4 | Description length limits | `ValidationService.descriptionError` | 400 |
| 5 | Only supported image formats | `ValidationService.validateImage` | 400 |
| 6 | Image size limit | same | 400 / 413 |
| 7 | No future dates | `ValidationService.dateError` | 400 |
| 8 | Claim cannot be approved without verified ownership | `ClaimService.approve` | 422 |
| 9 | Item cannot be returned before claim is approved/handover scheduled | `RecoveryService.schedule/complete` | 422 |
| 10 | Returned item cannot be claimed again | `ClaimService.submit` | 422 |
| 11 | Admin operations restricted by role | `SecurityConfig` + service checks | 401/403 |
| 12 | Rejected claim cannot become approved directly | `StateMachine` (REJECTED→UNDER_REVIEW only, admin only) | 409/403 |
| 13 | No duplicate active claims per user and item | `ClaimService.submit` | 409 |
| 14 | Closed report cannot be modified by unauthorised users | `ItemService.update*` | 403 |

## State machines (single source: `StateMachine.java`)
- **Lost:** REPORTED→UNDER_REVIEW→MATCHED→RESOLVED→CLOSED; also REPORTED/UNDER_REVIEW→REJECTED; REPORTED/UNDER_REVIEW/MATCHED/RESOLVED→CLOSED (withdraw); MATCHED→UNDER_REVIEW. CLOSED and REJECTED are terminal.
- **Found:** REPORTED→UNDER_REVIEW→AVAILABLE→CLAIM_PENDING→RETURNED; CLAIM_PENDING→AVAILABLE (no active claims left); REPORTED/UNDER_REVIEW→REJECTED. RETURNED and REJECTED are terminal.
- **Claim:** PENDING→UNDER_REVIEW→APPROVED/REJECTED; PENDING/UNDER_REVIEW→CANCELLED; APPROVED→HANDOVER_SCHEDULED→RETURNED; REJECTED→UNDER_REVIEW (admin reconsideration). CANCELLED and RETURNED are terminal.
- **Recovery:** SCHEDULED→COMPLETED/CANCELLED. Terminal afterwards.
Invalid transitions return **409** with a message such as `Claim cannot change status from REJECTED to APPROVED`. Every change is stored in `status_history` (from, to, user, time, note).

## Role matrix
| Capability | Anonymous | Student | Moderator | Admin |
|---|---|---|---|---|
| Register/login | ✔ | ✔ | ✔ | ✔ |
| Report, search, claim | ✘ | ✔ | ✔ | ✔ |
| Review/approve/reject reports, confirm matches, escalate claims | ✘ | ✘ | ✔ | ✔ |
| Approve/reject/reconsider claims, recoveries, users, categories, logs | ✘ | ✘ | ✘ | ✔ |
