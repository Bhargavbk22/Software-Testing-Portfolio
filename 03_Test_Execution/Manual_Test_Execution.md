# Manual Test Execution — Claim Workflow

| | |
|---|---|
| Date | 25 Nov 2026 |
| Environment | Chrome (normal + incognito), frontend :5173, backend :8081, MySQL 8 |
| Tester | Bhargav B K |
| Test item | **Seeded item Grey Hoodie (found item #7)**, loaded as already verified and Available. It was not freshly reported. |
| Accounts | student3 (Priya Nair) = claimant, student1 = finder, admin1 (Aarav Admin) = administrator |

| Step | Related case | Action | Expected | Actual | Status | Screenshot |
|---|---|---|---|---|---|---|
| WF-4 | EP-CL-01, ST-F-02 | student3 submits a valid claim for Grey Hoodie | Claim created, status Pending, item becomes Claim pending | Toast "Claim submitted. You will be notified about the decision." My claims lists Grey Hoodie, submitted Nov 25 2026, status Pending, with Add evidence and Cancel buttons. The item's own status was not captured in this screenshot | Pass | wf03-claim-pending.png |
| WF-5 | Duplicate claim (R13) | student3 submits a second claim for the same item | Error "You already have an active claim for this item"; no second claim | | | |
| WF-6 | EP-CL-05 | student1 (finder) opens Grey Hoodie | No "Claim this item" button | | | ep-cl-05-no-claim-button.png |
| WF-7 | ST-C-01, DT1-R5 | admin1 views the Pending claim | Only "Start review" offered; no Approve button | | | st-c-01-pending-only-review.png |
| WF-7b | ST-C-01 | admin1 clicks Start review | Status Under review; Approve, Reject, Request evidence appear | Claim card shows status Under review with Approve and Reject buttons visible (seen behind the approval dialog). Request evidence is hidden behind the dialog in this screenshot | Pass | wf04-approve-blocked.png |
| WF-8 | DT1-R2 | admin1 clicks Approve with "ownership verified" unticked | Blocked with an error message; claim stays Under review | Approve dialog open with the checkbox unticked and an empty note. Red toast: "Ownership must be verified before a claim can be approved". The claim card behind the dialog still shows Under review | Pass | wf04-approve-blocked.png |
| WF-9 | DT1-R1 | admin1 approves with "ownership verified" ticked | Status Approved | Claims page shows Grey Hoodie, Priya Nair (student3@campus.edu), status Approved, "Decision: Details match the report", and a Schedule handover button | Pass | wf05-claim-approved.png |
| WF-10 | DT4-R1 | admin1 schedules a handover | Status Handover scheduled | A recovery record for Grey Hoodie now exists, scheduled Nov 26, 11:00 AM at "Security Office, Main Block" (visible on the Recoveries page). The intermediate "Handover scheduled" claim status itself was not captured | Pass (partial evidence) | wf06-recovery-completed.png |
| WF-11 | DT4-R4, ST-F-04 | admin1 marks the recovery returned | Recovery Completed; confirmation code starting REC- | Recoveries page shows Grey Hoodie, claimant Priya Nair, responsible admin Aarav Admin, status Completed, confirmation REC-2026-00005 | Pass | wf06-recovery-completed.png |
| WF-12a | ST-F-04 | student3 opens My claims | Claim shows Returned and the confirmation code | | | |
| WF-12b | ST-F-05, EP-CL-03 | student3 and student4 open the returned item | Status Returned; no "Claim this item" button | | | |

## Observations
- **Handover time not enforced.** The handover was scheduled for Nov 26, 11:00 AM, yet "Mark returned" worked on Nov 25 at about 11:15. The business rules do not require the scheduled time to have passed, so this is an observation, not a failed case. Mention it in the summary as a possible improvement.
- **Other recovery rows are not from this run.** The Recoveries page also lists seeded demo records (for example Brown Leather Wallet, still Scheduled) and records created by the earlier Postman run (Black Dell Laptop v2, REC-2026-00004).