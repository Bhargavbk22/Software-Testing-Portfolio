# Software Testing Portfolio

## Team Details

| Name | USN |
|---|---|
| Bhargav B K | 1MS23IS028 |
| Chirag B K | 1MS23IS035 |
| Ananda K | 1MS23IS013 |

| | |
|---|---|
| **Subject** | Software Testing |
| **Department** | Information Science and Engineering |
| **Semester / Section** | _VII_ |
| **Institution** | _Ramaiah Institute of Technology_ |
| **Faculty** | _Kavya_ |
| **Academic Year** | _2026 _ |

---

## Objectives

1. Select a real application and identify its modules and functionalities to be tested.
2. Prepare a test plan covering scope, strategy, environment, risks and entry/exit criteria.
3. Design test cases using **Equivalence Partitioning, Boundary Value Analysis, Decision Table Testing and State Transition Testing**.
4. Execute test cases and record actual results honestly.
5. Identify, document and track defects.
6. Automate testing using **JUnit 5 / Mockito / Spring Boot Test** and **Postman**.
7. Prepare a test execution summary and maintain the work in GitHub with meaningful commits.

---

## Application Under Test: Digital Lost and Found Management System

A web application for a campus where students report lost items, report found items, search for items,
submit ownership claims and track their status. Moderators verify reports and potential matches, and
administrators review claims and complete the return of items, with a full audit trail.

### Roles
| Role | Main capabilities |
|---|---|
| Student | Register/login, report lost and found items, search, submit and track claims, notifications, profile |
| Moderator | Review, approve or reject reports, confirm potential matches, escalate claims |
| Administrator | Approve/reject claims, schedule and complete recoveries, manage users and categories, dashboard, activity logs |

### Modules tested
1. Authentication and user management
2. Lost item reporting
3. Found item reporting
4. Item search and discovery
5. Item matching (rule-based score)
6. Ownership claim management
7. Item recovery and return
8. Notifications
9. Admin dashboard
10. Moderator dashboard

### Technology stack
| Layer | Technology |
|---|---|
| Frontend | React 18 (Vite), Tailwind CSS, React Router, Axios, Recharts |
| Backend | Java 21, Spring Boot 3.3, Spring Security (JWT), Spring Data JPA |
| Database | MySQL 8 (H2 in-memory for automated tests) |
| Testing tools | JUnit 5, Mockito, Spring Boot Test / MockMvc, Postman, Maven Surefire |

---

## Testing Activities Performed

| # | Activity | Location |
|---|---|---|
| 1 | Test plan | `01_Test_Plan/` |
| 2 | Test case design: Equivalence Partitioning, Boundary Value Analysis, Decision Table, State Transition | `02_Test_Case_Design/` |
| 3 | Test execution with actual results | `03_Test_Execution/` |
| 4 | Defect reports | `04_Defect_Reports/` |
| 5 | Test automation: JUnit 5, Mockito, MockMvc and Postman | `05_Test_Automation/` |
| 6 | Test execution summary and reports | `06_Test_Reports/` |
| 7 | Screenshots | `07_Screenshots/` |
| 8 | Conclusion | `Conclusion.md` |

### Business rules used as the test basis
The application enforces 14 explicit rules (for example: no future dates, an item cannot be returned before its
claim is approved, a returned item cannot be claimed again, a rejected claim cannot be approved directly,
duplicate active claims are blocked). All validation limits are configurable, which makes boundary values
testable. See `application/documentation/Business_Rules.md`.

### Automated test summary
| Suite | Tests | Result |
|---|---|---|
| Unit tests (Mockito): validation EP/BVA, state machine, claims, recovery, matching, auth | 123 | 123 passed |
| Integration tests (Spring context, security, H2): full report, claim and recovery workflow | 18 | 18 passed |
| **Total (`mvn clean test`)** | **141** | **141 passed, 0 failed** |

Postman API test results and the manual test execution results are recorded in `03_Test_Execution/` and
`06_Test_Reports/`.

---

## Repository Structure

```
Software-Testing-Portfolio/
├── README.md
├── 01_Test_Plan/
├── 02_Test_Case_Design/
│   ├── Equivalence_Partitioning/
│   ├── Boundary_Value_Analysis/
│   ├── Decision_Table/
│   └── State_Transition/
├── 03_Test_Execution/
├── 04_Defect_Reports/
├── 05_Test_Automation/
├── 06_Test_Reports/
├── 07_Screenshots/
├── Conclusion.md
└── application/              <- source code of the application under test
    ├── backend/              (Spring Boot)
    ├── frontend/             (React)
    ├── database/schema.sql
    ├── postman/
    └── documentation/
```

---

## How to Run the Application

**Prerequisites:** JDK 21, Maven 3.9+, Node.js 20+, MySQL 8.

### 1. Database
```sql
CREATE DATABASE IF NOT EXISTS lost_and_found CHARACTER SET utf8mb4;
```
Tables are created automatically on first start.

### 2. Backend (default port 8080)
```powershell
cd application/backend
$env:DB_USERNAME="root"
$env:DB_PASSWORD="<your mysql password>"
$env:JWT_SECRET="<any random string of at least 32 characters>"
mvn spring-boot:run
```
If port 8080 is busy, set `$env:SERVER_PORT="8081"` and use the same port in `frontend/vite.config.js`.
Swagger UI: `http://localhost:8080/swagger-ui.html`

### 3. Frontend (http://localhost:5173)
```powershell
cd application/frontend
npm install
npm run dev
```

### 4. Demo accounts (local development only)
Password for all accounts: `Demo@12345`

| Role | Email |
|---|---|
| Administrator | `admin1@campus.edu` |
| Moderator | `mod1@campus.edu` |
| Student | `student1@campus.edu` to `student10@campus.edu` |

### 5. Run the automated tests
```powershell
cd application/backend
mvn clean test                          # 141 tests
mvn test surefire-report:report         # HTML report in target/site/surefire-report.html
```

### 6. Postman
Import `application/postman/Lost_And_Found_API_Collection.json`, set the `baseUrl` variable to the backend
URL, and run the collection with the backend running.

---

## Team Contributions

| Member | Contribution |
|---|---|
| Bhargav B K | _<fill in>_ |
| Chirag B K | _<fill in>_ |
| Ananda K | _<fill in>_ |

---

## Conclusion
See [`Conclusion.md`](Conclusion.md) for the final results, defects found and lessons learned.
