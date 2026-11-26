# Conclusion

## Summary of work
The Digital Lost and Found Management System implements 10 modules across a React frontend, Spring Boot API and MySQL database, with
JWT authentication, role-based access (student, moderator, administrator), enforced status lifecycles and 14 explicit business rules.
The team (Bhargav B K, Chirag B K, Ananda K) designed test cases with four techniques, automated them with JUnit 5, Mockito and
Spring Boot Test, ran an API suite in Postman, and executed the claim-to-return workflow manually in the browser against MySQL.

## Testing performed
| Technique | Cases designed | Executed | Passed | Failed |
|---|---|---|---|---|
| Equivalence Partitioning | 32 | 32 | 32 | 0 |
| Boundary Value Analysis | 26 | 23 | 23 | 0 |
| Decision Table | 23 | 19 | 19 | 0 |
| State Transition | 20 | 18 | 18 | 0 |
| **Total across the four techniques** | **101** | **92** | **92** | **0** |
| Automated (JUnit/Mockito/MockMvc), `mvn clean test` | 141 | 141 | 141 | 0 |
| Postman API (65 requests, 68 test assertions) | 68 | 68 | 68 | 0 |
| Manual UI, claim workflow (seeded Grey Hoodie) | 11 | 6 | 6 | 0 |

How to read the table:
- For the four techniques, "Executed" counts the cases traced to a passing automated test. The 9 cases not executed are the rows with no automated test: BVA-CE-01, BVA-CE-02, BVA-PH-01, DT2-R2 to DT2-R5, ST-L-02 and ST-C-08.
- The manual UI row counts the steps recorded so far. Update it as the remaining steps are completed (5 steps are still pending: WF-5, WF-6, WF-7, WF-12a, WF-12b).
- Evidence is in `06_Test_Reports/` (surefire report), `03_Test_Execution/` (Maven output, Postman run, manual log) and `07_Screenshots/`.

## Defects found
Four defects were recorded in `04_Defect_Reports/Defect_Reports.md`:

| ID | Title | Severity | Status |
|---|---|---|---|
| DEF-001 | Frontend failed to start: missing exports in `ui.jsx` | High | Closed |
| DEF-002 | Integration tests not executed by `mvn test` | Medium | Closed |
| DEF-003 | Postman collection import failed ("Incorrect format") | Low | Closed |
| DEF-004 | A scheduled handover can be completed before its scheduled time | Low | New (improvement) |

No automated test and no Postman assertion failed. The defects were found by running the system, and they were in the build,
tooling and one usability rule, not in the core business rules.

## Observations and lessons learned
- **Defects came from running the system, not from the rule tests.** All 141 automated tests and all 68 Postman assertions passed, but two defects (DEF-001, DEF-002) were only found by actually starting the frontend and reading the Maven output. A green test count only counts the tests that actually ran: the first Maven run showed 123 tests, and the 18 integration tests were silently skipped until the build configuration was fixed.
- **A single state-machine class made State Transition testing straightforward.** Every allowed and forbidden transition lives in `StateMachine.java`, so the transition table could be tested pair by pair, and the terminal states could be checked in one test.
- **Configurable limits made Boundary Value Analysis practical.** Because limits such as password length and image size come from `application.yml`, boundaries were tested at exactly min-1, min, max and max+1, and one test (BVA-CF-01) shows that changing the configured limit changes the behaviour.
- **Decision tables mapped directly to business rules.** Claim approval (item state, ownership verification and the actor's role) and handover were each covered by one rule per combination, with a test per rule.
- **Automated and manual testing complemented each other.** Automated tests proved rules quickly and repeatedly. The manual run showed what a user sees: for example the message "Ownership must be verified before a claim can be approved" when approval is attempted without verification.
- **Not everything was tested manually.** Some rows were only verified by automated tests, and the cases listed above remain unexecuted.

## Limitations and future work
- Image validation checks type, extension and size, not file content.
- Forgot-password sends no e-mail; notifications are in-app only.
- Matching is rule-based and simple; no fuzzy/ML similarity.
- No load, performance or cross-browser testing was performed.
- Automated tests run on H2, not MySQL; the MySQL deployment was verified through Postman and manual execution.
- Cases not executed: BVA-CE-01/02, BVA-PH-01, DT2-R2 to R5, ST-L-02, ST-C-08, and the pending manual steps.
- Concurrency (two users claiming the same item at the same moment) was not tested, although the application locks the item row to prevent it.
- Future work: add automated tests for the cases above, enforce that a handover cannot be completed before its scheduled time (DEF-004), and add end-to-end UI tests (for example Selenium).