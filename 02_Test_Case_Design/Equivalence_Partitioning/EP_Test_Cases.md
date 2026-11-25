# Equivalence Partitioning — test cases and results

Evidence: automated run `mvn clean test`, 23 Nov 2026, 141 tests, 0 failures (`06_Test_Reports/surefire-report.html`).
"Auto" = covered by `ValidationServiceTest`, `AuthServiceTest`, `ClaimServiceTest` or `ApiWorkflowIT`.
Actual results below are from those automated tests. Manual UI results can be added in the same cell.

| ID | Input | Partition | Sample value | Expected | Auto | Actual | Status |
|---|---|---|---|---|---|---|---|
| EP-EM-01 | Email | Valid | `user.name+tag@campus.edu` | accepted | ✔ | `ValidationServiceTest.email_validPartition_accepted` passed; accepted | Pass |
| EP-EM-02 | Email | Missing | (empty) | "Email is required" | ✔ | `email_invalidPartition_rejected` passed (null, empty and blank values); error returned | Pass |
| EP-EM-03 | Email | No `@` | `plain` | "Email format is invalid" | ✔ | `email_invalidPartition_rejected` passed; rejected | Pass |
| EP-EM-04 | Email | No domain dot | `a@b` | invalid | ✔ | `email_invalidPartition_rejected` passed; rejected | Pass |
| EP-EM-05 | Email | Existing account | `student1@campus.edu` | 409 duplicate | ✔ | `AuthServiceTest.register_duplicateEmail_isRejected` and `ApiWorkflowIT.register_duplicateEmail_returns409` passed; 409, no user saved | Pass |
| EP-PW-01 | Password | Valid, all classes | `Passw0rd!` | accepted | ✔ | `password_lowerBoundary` and `registration_valid_noException` passed; `ApiWorkflowIT.register_validRequest_returns201` returned 201 | Pass |
| EP-PW-02 | Password | No uppercase | `passw0rd!` | rejected | ✔ | `password_complexityPartitions_rejected` passed (`alllowercase1!`); rejected | Pass |
| EP-PW-03 | Password | No lowercase | `PASSW0RD!` | rejected | ✔ | `password_complexityPartitions_rejected` passed (`ALLUPPERCASE1!`); rejected | Pass |
| EP-PW-04 | Password | No digit | `Password!` | rejected | ✔ | `password_complexityPartitions_rejected` passed (`NoDigitsHere!`); rejected | Pass |
| EP-PW-05 | Password | No special char | `Passw0rdd` | rejected | ✔ | `password_complexityPartitions_rejected` passed (`NoSpecial123A`); rejected | Pass |
| EP-PW-06 | Confirm password | Mismatch | `Other0rd!` | "Passwords do not match" | ✔ | `registration_passwordMismatch_rejected` passed; message "Passwords do not match" | Pass |
| EP-IN-01 | Item name | Valid | `Black Dell Laptop` | accepted | ✔ | `itemName_boundaries` passed; `ApiWorkflowIT.lostReport_valid_returns201` created a report with this name (201) | Pass |
| EP-IN-02 | Item name | Blank/whitespace | `   ` | rejected | ✔ | `itemName_boundaries` passed; rejected | Pass |
| EP-IN-03 | Item name | Too long | 101 chars | rejected | ✔ | `itemName_boundaries` passed; rejected | Pass |
| EP-DE-01 | Description | Valid | 50 chars | accepted | ✔ | `description_boundaries` passed; `lostReport_valid_returns201` accepted a valid description (201) | Pass |
| EP-DE-02 | Description | Too short / missing | `short` / null | rejected | ✔ | `description_boundaries` passed (null and 9 chars); `ApiWorkflowIT.lostReport_shortDescription_returns400` returned 400 for `short` | Pass |
| EP-PH-01 | Phone | Valid local | `9876543210` | accepted | ✔ | `phone_valid` passed; accepted | Pass |
| EP-PH-02 | Phone | Valid international | `+919876543210` | accepted | ✔ | `phone_valid` passed; accepted | Pass |
| EP-PH-03 | Phone | Letters | `98765abcde` | rejected | ✔ | `phone_invalid` passed; rejected | Pass |
| EP-PH-04 | Phone | Too short | `12345` | rejected | ✔ | `phone_invalid` passed; rejected | Pass |
| EP-DT-01 | Date | Past (within window) | yesterday | accepted | ✔ | `ApiWorkflowIT.lostReport_valid_returns201` used yesterday's date; 201. `date_boundaries` passed (today accepted) | Pass |
| EP-DT-02 | Date | Future | tomorrow | "cannot be in the future" | ✔ | `date_boundaries` passed with message "Date cannot be in the future"; `lostReport_futureDate_returns400` returned 400 | Pass |
| EP-DT-03 | Date | Missing | null | "Date is required" | ✔ | `date_boundaries` passed (null rejected); `lostReport_missingMandatoryFields_returns400` returned 400 with an error on required fields | Pass |
| EP-IM-01 | Image type | Supported | `pic.png` image/png | accepted | ✔ | `ApiWorkflowIT.imageUpload_unsupportedFormat_returns400_andPngIsAccepted` passed; PNG accepted (200) | Pass |
| EP-IM-02 | Image type | Unsupported | `note.txt` text/plain | 400 | ✔ | Same test passed; `.txt` upload returned 400. `image_unsupportedTypes_rejected` passed | Pass |
| EP-IM-03 | Image type | Extension spoof | `virus.exe` as image/png | 400 | ✔ | `image_extensionMustMatchAllowedList` passed; `.exe` rejected | Pass |
| EP-CL-01 | Claim | Valid, available item | full form | 201 PENDING | ✔ | `ClaimServiceTest.submit_availableItem_createsPendingClaimAndLocksItem` passed; full workflow test returned 201 and PENDING | Pass |
| EP-CL-02 | Claim | Missing explanation | empty | 400 | ✔ | `ClaimServiceTest.submit_missingExplanation_failsValidation` passed; validation error raised | Pass |
| EP-CL-03 | Claim | Item returned | RETURNED item | 422 | ✔ | `submit_alreadyReturnedItem_isRejected` passed; `fullWorkflow_...` returned 422 for a claim on the returned item | Pass |
| EP-CL-04 | Claim | Item not verified | REPORTED item | 422 | ✔ | `submit_unverifiedItem_isRejected` passed; `fullWorkflow_...` returned 422 before verification | Pass |
| EP-CL-05 | Claim | Own found report | claimant = finder | 422 | ✔ | `submit_ownReport_isRejected` passed; rejected | Pass |
| EP-CL-06 | Claim | Unknown item | id 999999 | 404 | ✔ | `submit_unknownItem_isNotFound` passed (item id 99, same partition); not-found error raised | Pass |