# Test Plan — Digital Lost and Found Management System
| Item | Detail |
|---|---|
| Objective | Verify functional correctness, validation, access control, status lifecycles and API behaviour |
| Scope | Authentication, reporting, search, matching, claims, recovery, notifications, admin/moderator functions, REST API |
| Out of scope | Real e-mail delivery, performance/load, cross-browser matrix, malware scanning of images |
| Techniques | Equivalence Partitioning, Boundary Value Analysis, Decision Table, State Transition, API testing, automated JUnit/Mockito |
| Test basis | `documentation/Business_Rules.md`, `API_Documentation.md`, Swagger/OpenAPI |
| Environment | JDK 21, MySQL 8 (manual/Postman), H2 (automated), Chrome latest, Postman |
| Tools | JUnit 5, Mockito, Spring Boot Test/MockMvc, Postman, Maven Surefire report |
| Entry criteria | Code builds, demo data loaded, Swagger reachable |
| Exit criteria | All planned cases executed; no open Critical/High defects; automated suite green; report written |
| Deliverables | Designs in `Test_Case_Design/`, execution log, defect reports, automated report, `Conclusion.md` |

## Risks
Concurrent claims on one item (mitigation: row locking, test manually with two sessions); image content validation is by type/extension/size only;
rule drift between UI hints and backend limits (mitigation: backend is the source of truth).

## Schedule (suggested)
Design → Automation run → Postman run → Manual UI exploratory run → Defect retest → Report.

## Traceability
Rule numbers (R1–R14) in `Business_Rules.md` map to cases: EP (validation), BVA (limits), DT (R8, R9, R11, R12, R13), ST (R10, R12, R14).
