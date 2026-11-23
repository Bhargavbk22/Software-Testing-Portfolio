# Conclusion
> Complete this document **after** running the application and tests. Replace every `___` with real results.

## Summary of work
The Digital Lost and Found Management System implements 10 modules across a React frontend, Spring Boot API and MySQL database, with
JWT authentication, role-based access (student, moderator, administrator), enforced status lifecycles and 14 explicit business rules.

## Testing performed
| Technique | Cases designed | Executed | Passed | Failed |
|---|---|---|---|---|
| Equivalence Partitioning | ___ | ___ | ___ | ___ |
| Boundary Value Analysis | ___ | ___ | ___ | ___ |
| Decision Table | ___ | ___ | ___ | ___ |
| State Transition | ___ | ___ | ___ | ___ |
| Automated (JUnit/Mockito/MockMvc) | ___ | ___ | ___ | ___ |
| Postman API | ___ | ___ | ___ | ___ |

## Defects found
___ (list DEF-IDs, severity, status)

## Observations and lessons learned
___ (e.g. which technique found the most defects; effect of configurable limits on BVA; value of a single state-machine class)

## Limitations and future work
- Image validation checks type, extension and size, not file content.
- Forgot-password sends no e-mail; notifications are in-app only.
- Matching is rule-based and simple; no fuzzy/ML similarity.
- No load or cross-browser testing performed.
