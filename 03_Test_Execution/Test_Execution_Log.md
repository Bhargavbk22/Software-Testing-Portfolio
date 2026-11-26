# Test Execution Log

| Run | Date | Build/commit | Environment | Suite | Executed | Passed | Failed | Blocked | Tester | Evidence |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 25 Nov 2026 | `<paste output of: git log -1 --format=%h>` | JDK 21.0.9, Maven 3.9.12, H2 in-memory, Windows 10 | `mvn clean test` (123 unit + 18 integration) | 141 | 141 | 0 | 0 | Bhargav B K | `06_Test_Reports/surefire-report.html`, `06_Test_Reports/surefire-report-summary.png`, `03_Test_Execution/mvn-test-output-25Nov2026.txt` |
| 2 | 24 Nov 2026 | same build | MySQL 8, backend on port 8081, Postman Runner | Postman collection (65 requests) | 68 test assertions | 68 | 0 | 0 | Bhargav B K | `03_Test_Execution/postman-run-summary.png`, `03_Test_Execution/postman-run.json` |
| 3 | 25 Nov 2026 | same build | Chrome (normal + incognito), frontend :5173, backend :8081, MySQL 8 | Manual UI: claim workflow on seeded Grey Hoodie (11 steps) | 6 recorded so far | 6 | 0 | 0 | Bhargav B K | `03_Test_Execution/Manual_Test_Execution.md`, `07_Screenshots/` |

## Notes
- Run 1 was first executed on 23 Nov 2026 and also passed 141 of 141. It was repeated on 25 Nov 2026 to regenerate the report, with identical results.
- The first Maven run on 23 Nov 2026 executed only 123 tests, because the integration tests were skipped by default (see DEF-002 in `04_Defect_Reports/Defect_Reports.md`). After the fix, 141 tests ran.
- Run 3: 6 of the 11 steps are recorded as Pass (WF-4, WF-7b, WF-8, WF-9, WF-10 with partial evidence, WF-11). WF-5, WF-6, WF-7, WF-12a and WF-12b are still Pending. Update the Executed and Passed numbers when you finish them, and write `Not executed` for any you skip.
- No test failed in runs 1 and 2. Defects found during the project are listed in `04_Defect_Reports/Defect_Reports.md`.

## Manual UI checklist
Tick only what you have actually done and recorded.

- [ ] Register with each EP partition; verify inline messages
- [ ] Login valid/invalid; expired token redirects to login
- [ ] Report lost/found with boundary values; image > 2 MB, wrong type, 4 images
- [ ] Search: each filter, sort, pagination, empty result (empty-state message seen once: search for a found item under "Lost reports")
- [ ] Claim flow as two students; duplicate claim blocked (claim submitted and returned; duplicate attempt not yet recorded)
- [ ] Moderator: review, approve, reject, confirm match, escalate
- [x] Admin: approve without verification (blocked), approve, schedule, mark returned
- [ ] Claim again after return (blocked)
- [ ] Role access: student/moderator cannot open admin URLs
- [ ] Concurrency: two browsers claim the same item simultaneously
- [ ] Network failure: stop backend and observe friendly error messages