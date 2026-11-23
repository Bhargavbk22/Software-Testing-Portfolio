# API Documentation
Interactive docs: `/swagger-ui.html`. Authenticated calls send `Authorization: Bearer <jwt>` (from `POST /api/auth/login`).

| Area | Method & path | Access | Success | Notable errors |
|---|---|---|---|---|
| Auth | POST `/api/auth/register` | public | 201 | 400 validation, 409 duplicate email/student ID |
| | POST `/api/auth/login` | public | 200 token | 400, 401 invalid credentials |
| | POST `/api/auth/forgot-password` | public | 200 (generic) | 400 |
| | GET/PUT `/api/users/profile` | any user | 200 | 400 |
| Lost | POST `/api/lost-items` | user | 201 | 400 |
| | GET `/api/lost-items?mine=true` | user | 200 page | |
| | GET/PUT/DELETE `/api/lost-items/{id}` | owner/staff; DELETE = withdraw | 200 | 403 closed/foreign, 404, 409 already closed |
| | POST `/api/lost-items/{id}/images` (multipart `file`) | owner | 200 | 400 format/size/count, 413 |
| Found | POST `/api/found-items`, GET list, GET/PUT `/{id}`, POST `/{id}/images` | user | 201/200 | 403, 422 edit after verification |
| Search | GET `/api/items/search?type&q&categoryId&location&from&to&status&sort&page&size` | user | 200 page | 400 bad status |
| Categories | GET `/api/categories` | user | 200 | |
| Claims | POST `/api/claims` | user | 201 | 400, 409 duplicate, 422 item returned/unavailable |
| | GET `/api/claims/my-claims`, GET `/api/claims/{id}` | owner/staff | 200 | 403 |
| | PUT `/api/claims/{id}/cancel`, POST `/api/claims/{id}/evidence` | owner | 200 | 409, 422 |
| | GET `/api/admin/claims?status=` | admin | 200 | |
| | PUT `/api/admin/claims/{id}/review` | admin | 200 | 403 (moderator reconsidering), 409 |
| | PUT `/api/admin/claims/{id}/approve` body `{ownershipVerified, reason}` | admin | 200 | 409 invalid transition, 422 unverified/unavailable/returned |
| | PUT `/api/admin/claims/{id}/reject` body `{reason}` | admin | 200 | 400 no reason, 409 |
| | PUT `/api/admin/claims/{id}/request-evidence` body `{message}` | admin | 200 | 400, 422 |
| Recovery | POST `/api/recoveries` `{claimId, scheduledAt, location}` | admin | 201 | 400, 409 duplicate, 422 claim not approved |
| | GET `/api/recoveries`, GET `/api/recoveries/mine` | admin / user | 200 | |
| | PUT `/api/recoveries/{id}/complete` | admin | 200 + confirmation code | 409 already completed, 422 |
| Moderator | GET `/api/moderator/dashboard`, `/matches`, `/claims` | moderator/admin | 200 | 403 |
| | PUT `/api/moderator/reports/{LOST\|FOUND}/{id}/review\|approve` ; `/reject` `{reason}` | moderator/admin | 200 | 409, 422 |
| | PUT `/api/moderator/matches/{id}/confirm\|dismiss`; PUT `/api/moderator/claims/{id}/escalate` | moderator/admin | 200 | 422, 409 |
| Admin | GET `/api/admin/dashboard`, `/users`, `/reports`, `/activity-logs` | admin | 200 | 403 |
| | PUT `/api/admin/users/{id}/status` `{enabled}`; POST/PUT/DELETE `/api/admin/categories` | admin | 200 | 409 duplicate, 422 category in use |
| Notifications | GET `/api/notifications?unreadOnly`, GET `/unread-count`, PUT `/{id}/read`, PUT `/read-all` | user | 200 | 403 |

Error body example: `{"success":false,"message":"Validation failed","errors":{"date":"Date cannot be in the future"},"timestamp":"..."}`
