# Module Description
| # | Module | Backend | Frontend |
|---|---|---|---|
| 1 | Authentication & users | `AuthController`, `AuthService`, `JwtService`, `SecurityConfig` | Login, Register, Forgot password, Profile |
| 2 | Lost item reporting | `ItemController`, `ItemService` (create/update/withdraw/images) | Report lost, My reports, Item detail |
| 3 | Found item reporting | same; identifying details hidden from public | Report found |
| 4 | Search & discovery | `GET /api/items/search` (JPA Specifications, paging, sort) | Search page with filters, empty states |
| 5 | Matching | `MatchService` (score 0–100; category 25, name 25, colour 15, brand 15, location 10, date 10) | Matches on item detail, Moderator page |
| 6 | Claims | `ClaimService` (submit, review, approve, reject, cancel, evidence) | Item detail claim dialog, My claims, Admin claims |
| 7 | Recovery | `RecoveryService` (schedule, complete, confirmation code) | Admin recoveries |
| 8 | Notifications | `NotificationService` | Bell with unread count, Notifications page |
| 9 | Admin dashboard | `AdminService` (stats, charts data, users, logs, categories) | Dashboard (Recharts), Users, Reports, Categories, Logs |
| 10 | Moderator dashboard | `ModeratorService`, `ModeratorController` | Moderation page |

Matching is deliberately simple and explainable: a match is created when the score reaches `app.matching.min-score` (50).
It is only an indication; a moderator must confirm, and ownership is still verified through the claim process.
