# Defect Reports — Digital Lost and Found Management System

Team: Bhargav B K (1MS23IS028), Chirag B K (1MS23IS035), Ananda K (1MS23IS013)
Blank format: `Defect_Report_Template.md`

## Defect summary

| ID | Title | Module | Severity | Status |
|---|---|---|---|---|
| DEF-001 | Frontend dev server fails: missing exports in `ui.jsx` | Frontend | High | Closed |
| DEF-002 | Integration tests not executed by `mvn test` | Build / automation | Medium | Closed |
| DEF-003 | Postman collection import fails ("Incorrect format") | API test assets | Low | Closed |
| DEF-004 | Handover can be completed before its scheduled time | Recovery | Low | New (improvement) |

Totals: 4 defects. Severity: 1 High, 1 Medium, 2 Low. Status: 3 Closed, 1 New.

---

## DEF-001: Frontend failed to start (missing UI components)

| Field | Value |
|---|---|
| ID | DEF-001 |
| Title | Frontend dev server fails: "No matching export in src/components/ui.jsx" |
| Module | Frontend (shared UI component library) |
| Severity / Priority | High / High (application unusable) |
| Found in build | Initial delivery of the project, 23 Nov 2026 |
| Related test case | Smoke test: start the frontend and open the login page |
| Environment | Windows 10, Node.js, Vite 5.4.21, `npm run dev` |
| Preconditions | Backend running; `npm install` completed |
| Steps to reproduce | 1. In `application/frontend` run `npm run dev`. 2. Observe the terminal output. |
| Expected result | Vite starts and the application opens at http://localhost:5173. |
| Actual result | Vite printed "Failed to scan for dependencies" with errors such as `No matching export in "src/components/ui.jsx" for import "Empty"` (also ErrorState, Modal, PageHeader, Pagination, Skeleton, SkeletonRows, StatusBadge, Confirm). Pages that import these components could not be built. |
| Root cause | A scripted edit to `ui.jsx` removed nine exported components by mistake. |
| Evidence | Terminal output from `npm run dev` (23 Nov 2026) |
| Status | Closed |
| Fix reference | `ui.jsx` restored with all 18 exports and replaced in `application/frontend/src/components/` |
| Retest result | Pass: frontend started and the login page loaded; all pages used in manual testing rendered. |

---

## DEF-002: Integration tests skipped by `mvn test`

| Field | Value |
|---|---|
| ID | DEF-002 |
| Title | Integration tests (`ApiWorkflowIT`) were not executed by `mvn test` |
| Module | Build configuration / test automation |
| Severity / Priority | Medium / High (18 integration tests silently skipped) |
| Found in build | Initial delivery, 23 Nov 2026 |
| Related test case | All integration tests in `ApiWorkflowIT` |
| Environment | Maven 3.9.12, JDK 21.0.9, Surefire 3.2.5 |
| Preconditions | Project compiles |
| Steps to reproduce | 1. In `application/backend` run `mvn clean test`. 2. Read the list of test classes executed. |
| Expected result | All 8 test classes run, including the integration tests (141 tests in total). |
| Actual result | Only the six `*Test` classes ran (123 tests). `ApiWorkflowIT` did not run because Surefire's default includes only class names ending in `Test`. The README wrongly said `mvn test` ran it. |
| Evidence | Maven output of the first run: 6 classes, 123 tests; later output: 7 classes including `ApiWorkflowIT`, 141 tests |
| Status | Closed |
| Fix reference | Added `maven-surefire-plugin` `<includes>` for `**/*Test.java` and `**/*IT.java` in `pom.xml` |
| Retest result | Pass: `mvn clean test` now runs 141 tests, 0 failures (25 Nov 2026). |

---

## DEF-003: Postman collection rejected on import

| Field | Value |
|---|---|
| ID | DEF-003 |
| Title | Postman collection import fails with "Incorrect format" |
| Module | API test assets (Postman collection) |
| Severity / Priority | Low / Medium |
| Found in build | Initial delivery, 24 Nov 2026 |
| Related test case | Postman API run |
| Environment | Postman desktop, collection v2.1 JSON |
| Preconditions | `Lost_And_Found_API_Collection.json` available |
| Steps to reproduce | 1. In Postman click Import. 2. Select the collection file. |
| Expected result | The collection imports with all folders and requests. |
| Actual result | Postman reported "Incorrect format, please input the right format to import". The likely cause was an image-upload request with an empty file field, plus no collection ID. |
| Evidence | Postman import error message |
| Status | Closed |
| Fix reference | Removed the faulty request, added a collection `_postman_id`; collection now has 65 requests |
| Retest result | Pass: import succeeded and the full run passed (68 of 68 test assertions, 0 failed). |

---

## DEF-004: Handover can be completed before its scheduled time

| Field | Value |
|---|---|
| ID | DEF-004 |
| Title | A scheduled handover can be marked returned before its scheduled date and time |
| Module | Recovery and return management |
| Severity / Priority | Low / Low |
| Found in build | Current build, 25 Nov 2026 |
| Related test case | DT4-R4 / WF-11 (manual execution) |
| Environment | Chrome, frontend :5173, backend :8081, MySQL 8 |
| Preconditions | An approved claim (Grey Hoodie) with a handover scheduled for 26 Nov 2026, 11:00 AM |
| Steps to reproduce | 1. As admin1, schedule a handover for tomorrow at 11:00 AM. 2. On 25 Nov at about 11:15 open Admin → Recoveries. 3. Click Mark returned and confirm. |
| Expected result | Open point: the business rules do not say whether completion before the scheduled time is allowed. |
| Actual result | The recovery was completed immediately (status Completed, REC-2026-00005) although the scheduled time had not arrived. |
| Evidence | `07_Screenshots/wf06-recovery-completed.png` |
| Status | New (not fixed; recorded as an improvement suggestion) |
| Fix reference | n/a |
| Retest result | n/a |