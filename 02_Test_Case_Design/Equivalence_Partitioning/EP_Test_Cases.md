# Equivalence Partitioning — planned test cases
Result columns are intentionally blank until executed. "Auto" = covered by `ValidationServiceTest` / `ApiWorkflowIT`.

| ID | Input | Partition | Sample value | Expected | Auto | Actual | Status |
|---|---|---|---|---|---|---|---|
| EP-EM-01 | Email | Valid | `user.name+tag@campus.edu` | accepted | ✔ | | |
| EP-EM-02 | Email | Missing | (empty) | "Email is required" | ✔ | | |
| EP-EM-03 | Email | No `@` | `plain` | "Email format is invalid" | ✔ | | |
| EP-EM-04 | Email | No domain dot | `a@b` | invalid | ✔ | | |
| EP-EM-05 | Email | Existing account | `student1@campus.edu` | 409 duplicate | ✔ | | |
| EP-PW-01 | Password | Valid, all classes | `Passw0rd!` | accepted | ✔ | | |
| EP-PW-02 | Password | No uppercase | `passw0rd!` | rejected | ✔ | | |
| EP-PW-03 | Password | No lowercase | `PASSW0RD!` | rejected | ✔ | | |
| EP-PW-04 | Password | No digit | `Password!` | rejected | ✔ | | |
| EP-PW-05 | Password | No special char | `Passw0rdd` | rejected | ✔ | | |
| EP-PW-06 | Confirm password | Mismatch | `Other0rd!` | "Passwords do not match" | ✔ | | |
| EP-IN-01 | Item name | Valid | `Black Dell Laptop` | accepted | ✔ | | |
| EP-IN-02 | Item name | Blank/whitespace | `   ` | rejected | ✔ | | |
| EP-IN-03 | Item name | Too long | 101 chars | rejected | ✔ | | |
| EP-DE-01 | Description | Valid | 50 chars | accepted | ✔ | | |
| EP-DE-02 | Description | Too short / missing | `short` / null | rejected | ✔ | | |
| EP-PH-01 | Phone | Valid local | `9876543210` | accepted | ✔ | | |
| EP-PH-02 | Phone | Valid international | `+919876543210` | accepted | ✔ | | |
| EP-PH-03 | Phone | Letters | `98765abcde` | rejected | ✔ | | |
| EP-PH-04 | Phone | Too short | `12345` | rejected | ✔ | | |
| EP-DT-01 | Date | Past (within window) | yesterday | accepted | ✔ | | |
| EP-DT-02 | Date | Future | tomorrow | "cannot be in the future" | ✔ | | |
| EP-DT-03 | Date | Missing | null | "Date is required" | ✔ | | |
| EP-IM-01 | Image type | Supported | `pic.png` image/png | accepted | ✔ | | |
| EP-IM-02 | Image type | Unsupported | `note.txt` text/plain | 400 | ✔ | | |
| EP-IM-03 | Image type | Extension spoof | `virus.exe` as image/png | 400 | ✔ | | |
| EP-CL-01 | Claim | Valid, available item | full form | 201 PENDING | ✔ | | |
| EP-CL-02 | Claim | Missing explanation | empty | 400 | ✔ | | |
| EP-CL-03 | Claim | Item returned | RETURNED item | 422 | ✔ | | |
| EP-CL-04 | Claim | Item not verified | REPORTED item | 422 | ✔ | | |
| EP-CL-05 | Claim | Own found report | claimant = finder | 422 | ✔ | | |
| EP-CL-06 | Claim | Unknown item | id 999999 | 404 | | | |
