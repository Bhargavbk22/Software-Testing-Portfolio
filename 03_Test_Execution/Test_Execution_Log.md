# Test Execution Log (template — fill in from real runs)
| Run | Date | Build/commit | Environment | Suite | Executed | Passed | Failed | Blocked | Tester | Evidence |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | | | | `mvn test` | | | | | | `testing/Test_Reports/surefire-report.html` |
| 2 | | | | Postman collection | | | | | | `testing/Test_Execution/postman-run.json` |
| 3 | | | | Manual UI (EP/BVA/DT/ST) | | | | | | `screenshots/` |

## Manual UI checklist (tick during execution)
- [ ] Register with each EP partition; verify inline messages
- [ ] Login valid/invalid; expired token redirects to login
- [ ] Report lost/found with boundary values; image > 2 MB, wrong type, 4 images
- [ ] Search: each filter, sort, pagination, empty result
- [ ] Claim flow as two students; duplicate claim blocked
- [ ] Moderator: review, approve, reject, confirm match, escalate
- [ ] Admin: approve without verification (blocked), approve, schedule, mark returned, claim again (blocked)
- [ ] Role access: student/moderator cannot open admin URLs
- [ ] Concurrency: two browsers claim the same item simultaneously
- [ ] Network failure: stop backend and observe friendly error messages
