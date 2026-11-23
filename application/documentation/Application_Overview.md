# Application Overview
**Digital Lost and Found Management System** lets students report lost items, report found items, search verified found
items, submit ownership claims and track everything, while moderators and administrators verify reports, review claims and
complete the physical return with an auditable trail.

## Actors
| Role | Capabilities |
|---|---|
| Student/User | Register, log in, report lost/found items, search, submit/cancel claims, add evidence, track statuses, notifications, profile |
| Moderator | Review/approve/reject reports, confirm/dismiss potential matches, see claims needing review and escalate them |
| Administrator | Everything above plus: approve/reject/reconsider claims, request evidence, schedule/complete recoveries, manage users and categories, dashboard, activity logs |

## Architecture
```
React (Vite, :5173) --HTTP/JSON + JWT--> Spring Boot REST API (:8080) --JPA--> MySQL
                                         ├─ web/        controllers (thin, DTOs only)
                                         ├─ service/    business rules, StateMachine, ValidationService, MatchService
                                         ├─ repo/       Spring Data repositories
                                         ├─ security/   JWT filter, SecurityConfig, role rules
                                         └─ exception/  GlobalExceptionHandler -> consistent JSON errors
```
Every response uses `{ success, message, data, errors, timestamp }`. Lists use `{ content, page, size, totalElements, totalPages }`.

## Error-to-status mapping
400 validation / malformed · 401 unauthenticated or expired token · 403 forbidden · 404 not found · 409 duplicate or invalid
state transition · 413 upload too large · 422 business-rule violation · 503 database unavailable · 500 unexpected.
