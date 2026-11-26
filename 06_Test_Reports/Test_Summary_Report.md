# Test Execution Summary Report

| | |
|---|---|
| Application | Digital Lost and Found Management System |
| Team | Bhargav B K (1MS23IS028), Chirag B K (1MS23IS035), Ananda K (1MS23IS013) |
| Test period | 23 Nov 2026 – 25 Nov 2026 |
| Environment | JDK 21.0.9, Maven 3.9.12, Windows 10, Chrome, MySQL 8 (manual and Postman), H2 in-memory (automated) |

## 1. Results by test activity

| Activity | Executed | Passed | Failed | Evidence |
|---|---|---|---|---|
| Automated unit tests (Mockito) | 123 | 123 | 0 | `surefire-report.html` |
| Automated integration tests (Spring context, MockMvc, H2) | 18 | 18 | 0 | `surefire-report.html` |
| **Automated total (`mvn clean test`, 25 Nov 2026)** | **141** | **141** | **0** | `06_Test_Reports/`, `mvn-test-output-25Nov2026.txt` |
| Postman API run (24 Nov 2026), 65 requests | 68 assertions | 68 | 0 | `postman-run-summary.png`, `postman-run.json` |
| Manual UI execution, claim workflow | ___ | ___ | ___ | `Manual_Test_Execution.md`, `07_Screenshots/` |

## 2. Results by technique (automated evidence)

| Technique | Design file | Result |
|---|---|---|
| Equivalence Partitioning | `EP_Test_Cases.md` | All 31 cases traced to passing automated tests |
| Boundary Value Analysis | `BVA_Test_Cases.md` | 22 of 25 rows traced to passing automated tests; 3 rows (claim explanation 19/20 and 1000/1001, 9-digit phone) rely on manual execution |
| Decision Table | `Decision_Tables.md` | 17 of 22 rules traced to passing automated tests; DT2-R2 to R5 rely on manual execution |
| State Transition | `State_Transition_Tests.md` | 18 of 20 cases traced to passing automated tests; ST-L-02 and ST-C-08 rely on manual execution |

## 3. Manual execution results (claim workflow, seeded Grey Hoodie item)
Rows recorded as Pass: ___ of ___. Rows not executed: ___. See `Manual_Test_Execution.md`.

## 4. Defects
| Severity | Count |
|---|---|
| Critical / High | ___ |
| Medium | ___ |
| Low | ___ |

No test in the automated suite or the Postman run failed. Defects raised during manual execution, if any, are listed in `04_Defect_Reports/`.

**Observation (not a failed case):** a handover scheduled for a future time can be marked returned before that time. The business rules do not require the scheduled time to have passed, so this is recorded as a possible improvement.

## 5. Limitations
- Automated tests use H2, not MySQL. The application itself was verified against MySQL through the Postman run and the manual run.
- Image validation checks file type, extension and size, not file content.
- No load, performance or cross-browser testing was performed.
- The Postman collection does not include an image-upload request; image upload is covered by `ApiWorkflowIT`.

## 6. Conclusion
___ (Two or three sentences based on your final results.)