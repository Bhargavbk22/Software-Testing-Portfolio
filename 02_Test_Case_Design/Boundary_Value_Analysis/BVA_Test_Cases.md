# Boundary Value Analysis — test cases and results (default limits from `application.yml`)

Evidence: automated run `mvn clean test`, 23 Nov 2026, 141 tests, 0 failures (`06_Test_Reports/surefire-report.html`).
Rows with a blank Actual/Status have not been executed yet.

| ID | Field | Boundary | Value | Expected | Auto | Actual | Status |
|---|---|---|---|---|---|---|---|
| BVA-PW-01 | Password length | min−1 | 7 chars `Abcd12!` | rejected | ✔ | Automated: `ValidationServiceTest.password_lowerBoundary` passed; password rejected | Pass |
| BVA-PW-02 | Password length | min | 8 chars `Abcd123!` | accepted | ✔ | Automated: `password_lowerBoundary` passed; accepted | Pass |
| BVA-PW-03 | Password length | min+1 | 9 chars | accepted | ✔ | Automated: `password_lowerBoundary` passed; accepted | Pass |
| BVA-PW-04 | Password length | max | 64 chars | accepted | ✔ | Automated: `password_upperBoundary` passed; accepted | Pass |
| BVA-PW-05 | Password length | max+1 | 65 chars | rejected | ✔ | Automated: `password_upperBoundary` passed; rejected | Pass |
| BVA-DE-01 | Description | min−1 | 9 chars | rejected | ✔ | Automated: `description_boundaries` passed; rejected | Pass |
| BVA-DE-02 | Description | min | 10 chars | accepted | ✔ | Automated: `description_boundaries` passed; accepted | Pass |
| BVA-DE-03 | Description | max | 1000 chars | accepted | ✔ | Automated: `description_boundaries` passed; accepted | Pass |
| BVA-DE-04 | Description | max+1 | 1001 chars | rejected | ✔ | Automated: `description_boundaries` passed; rejected | Pass |
| BVA-IN-01 | Item name | min−1 / min | 1 / 2 chars | rejected / accepted | ✔ | Automated: `itemName_boundaries` passed; 1 char rejected, 2 chars accepted | Pass |
| BVA-IN-02 | Item name | max / max+1 | 100 / 101 chars | accepted / rejected | ✔ | Automated: `itemName_boundaries` passed; 100 accepted, 101 rejected | Pass |
| BVA-CE-01 | Claim explanation | min−1 / min | 19 / 20 chars | rejected / accepted | | | |
| BVA-CE-02 | Claim explanation | max / max+1 | 1000 / 1001 | accepted / rejected | | | |
| BVA-DT-01 | Date | today | today | accepted | ✔ | Automated: `date_boundaries` passed; accepted | Pass |
| BVA-DT-02 | Date | today+1 | tomorrow | rejected | ✔ | Automated: `date_boundaries` passed; "Date cannot be in the future" | Pass |
| BVA-DT-03 | Date | oldest allowed | today−730 | accepted | ✔ | Automated: `date_boundaries` passed; accepted | Pass |
| BVA-DT-04 | Date | too old | today−731 | rejected | ✔ | Automated: `date_boundaries` passed; rejected | Pass |
| BVA-IM-01 | Image size | max | 2 097 152 bytes | accepted | ✔ | Automated: `image_sizeBoundary` passed; accepted | Pass |
| BVA-IM-02 | Image size | max+1 | 2 097 153 bytes | rejected | ✔ | Automated: `image_sizeBoundary` passed; rejected | Pass |
| BVA-IM-03 | Image size | empty | 0 bytes | rejected | ✔ | Automated: `image_sizeBoundary` passed; rejected | Pass |
| BVA-IM-04 | Image count | max−1 existing | 2 existing + 1 | accepted | ✔ | Automated: `image_maxCountBoundary` passed; accepted | Pass |
| BVA-IM-05 | Image count | max existing | 3 existing + 1 | rejected | ✔ | Automated: `image_maxCountBoundary` passed; rejected | Pass |
| BVA-PH-01 | Phone digits | 9 / 10 | 9 digits / 10 digits | rejected / accepted | | | |
| BVA-PH-02 | Phone digits | 15 / 16 | 15 digits / 16 digits | accepted / rejected | ✔ | Automated: `phone_valid` (15 digits) and `phone_invalid` (16 digits) passed | Pass |
| BVA-MT-01 | Match date window | 7 days apart / 8 days apart | | +10 points / +0 | ✔ | Automated: `MatchServiceTest.dateBoundary_sevenDaysCounts_eightDoesNot` passed | Pass |
| BVA-CF-01 | Configurability | change `password-min-length` to 12 | `Abcd123!` | rejected | ✔ | Automated: `ValidationServiceTest.password_limitsAreConfigurable` passed | Pass |