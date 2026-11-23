# Boundary Value Analysis — planned test cases (default limits from `application.yml`)
| ID | Field | Boundary | Value | Expected | Auto | Actual | Status |
|---|---|---|---|---|---|---|---|
| BVA-PW-01 | Password length | min−1 | 7 chars `Abcd12!` | rejected | ✔ | | |
| BVA-PW-02 | Password length | min | 8 chars `Abcd123!` | accepted | ✔ | | |
| BVA-PW-03 | Password length | min+1 | 9 chars | accepted | ✔ | | |
| BVA-PW-04 | Password length | max | 64 chars | accepted | ✔ | | |
| BVA-PW-05 | Password length | max+1 | 65 chars | rejected | ✔ | | |
| BVA-DE-01 | Description | min−1 | 9 chars | rejected | ✔ | | |
| BVA-DE-02 | Description | min | 10 chars | accepted | ✔ | | |
| BVA-DE-03 | Description | max | 1000 chars | accepted | ✔ | | |
| BVA-DE-04 | Description | max+1 | 1001 chars | rejected | ✔ | | |
| BVA-IN-01 | Item name | min−1 / min | 1 / 2 chars | rejected / accepted | ✔ | | |
| BVA-IN-02 | Item name | max / max+1 | 100 / 101 chars | accepted / rejected | ✔ | | |
| BVA-CE-01 | Claim explanation | min−1 / min | 19 / 20 chars | rejected / accepted | | | |
| BVA-CE-02 | Claim explanation | max / max+1 | 1000 / 1001 | accepted / rejected | | | |
| BVA-DT-01 | Date | today | today | accepted | ✔ | | |
| BVA-DT-02 | Date | today+1 | tomorrow | rejected | ✔ | | |
| BVA-DT-03 | Date | oldest allowed | today−730 | accepted | ✔ | | |
| BVA-DT-04 | Date | too old | today−731 | rejected | ✔ | | |
| BVA-IM-01 | Image size | max | 2 097 152 bytes | accepted | ✔ | | |
| BVA-IM-02 | Image size | max+1 | 2 097 153 bytes | rejected | ✔ | | |
| BVA-IM-03 | Image size | empty | 0 bytes | rejected | ✔ | | |
| BVA-IM-04 | Image count | max−1 existing | 2 existing + 1 | accepted | ✔ | | |
| BVA-IM-05 | Image count | max existing | 3 existing + 1 | rejected | ✔ | | |
| BVA-PH-01 | Phone digits | 9 / 10 | | rejected / accepted | | | |
| BVA-PH-02 | Phone digits | 15 / 16 | | accepted / rejected | | | |
| BVA-MT-01 | Match date window | 7 days apart / 8 days apart | | +10 points / +0 | ✔ | | |
| BVA-CF-01 | Configurability | change `password-min-length` to 12 | `Abcd123!` | rejected | ✔ | | |
