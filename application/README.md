# Digital Lost and Found Management System

A full-stack campus lost-and-found platform built as a **Software Testing portfolio project**. It is designed so that
Equivalence Partitioning, Boundary Value Analysis, Decision Table Testing, State Transition Testing, API testing and
JUnit/Mockito automation can all be demonstrated against real, enforced business rules.

| Layer | Technology |
|---|---|
| Frontend | React 18 (Vite), Tailwind CSS 3, React Router 6, Axios, Lucide icons, Recharts |
| Backend | Java 21, Spring Boot 3.3, Spring Security (JWT), Spring Data JPA, springdoc OpenAPI |
| Database | MySQL 8 (H2 in-memory for automated tests) |
| Testing | JUnit 5, Mockito, Spring Boot Test / MockMvc, Postman |

> **Status of verification.** This code base was written without access to a build toolchain or network, so it has **not
> yet been compiled or run**. Follow "First run checklist" below; fix any compile error you meet (they are expected to be
> small) and record real results under `testing/`. No test results in this repository are fabricated.

## Prerequisites
- JDK 21, Maven 3.9+, Node.js 20+, MySQL 8 running locally (or use the H2 option below).

## 1. Database
```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS lost_and_found CHARACTER SET utf8mb4;"
```
Tables are created automatically on first start (`ddl-auto=update`). `database/schema.sql` is the reference DDL if you prefer
to create the schema manually (then start with `DDL_AUTO=validate`).

## 2. Backend (http://localhost:8080)
```bash
cd backend
export DB_USERNAME=root DB_PASSWORD=your_mysql_password          # Windows PowerShell: $env:DB_PASSWORD="..."
export JWT_SECRET="replace-with-a-random-string-of-32+-characters"
mvn spring-boot:run
```
Swagger UI: http://localhost:8080/swagger-ui.html · OpenAPI JSON: http://localhost:8080/v3/api-docs

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | local MySQL, root, empty | database connection |
| `JWT_SECRET` | dev-only value | HMAC signing key (min 32 chars). **Always override outside local dev.** |
| `JWT_EXPIRATION_MINUTES` | 120 | token lifetime |
| `SEED_DEMO_DATA` | true | load demo data on an empty database. Set `false` for any shared environment |
| `SEED_PASSWORD` | `Demo@12345` | password for all demo accounts |
| `UPLOAD_DIR` | `uploads` | where item images are stored |
| `CORS_ORIGINS` | `http://localhost:5173` | allowed browser origins |

All validation limits (password length, description length, image size/count, date window...) are configurable under
`app.validation.*` in `backend/src/main/resources/application.yml` — see `documentation/Business_Rules.md`.

## 3. Frontend (http://localhost:5173)
```bash
cd frontend
npm install
npm run dev
```
Vite proxies `/api` and `/uploads` to port 8080. For a production build: `npm run build`.

## 4. Demo accounts (local development only)
All use the password **`Demo@12345`** (override with `SEED_PASSWORD`). They exist only when `SEED_DEMO_DATA=true`.

| Role | Email |
|---|---|
| Administrator | `admin1@campus.edu`, `admin2@campus.edu` |
| Moderator | `mod1@campus.edu`, `mod2@campus.edu` |
| Student | `student1@campus.edu` … `student10@campus.edu` |

Seed data: 14 accounts, 15 lost and 15 found reports across all categories and statuses, claims in every status, two
completed recoveries and one scheduled handover, notifications, matches and activity logs.

## 5. Automated tests
```bash
cd backend
mvn test                          # unit tests (Mockito) + integration tests (H2, MockMvc)
mvn test surefire-report:report   # HTML report: backend/target/site/surefire-report.html
```
| Test class | Technique |
|---|---|
| `unit/ValidationServiceTest` | Equivalence Partitioning + Boundary Value Analysis |
| `unit/StateMachineTest` | State Transition Testing (all four lifecycles) |
| `unit/ClaimServiceTest`, `RecoveryServiceTest` | Decision tables + business rules with Mockito |
| `unit/AuthServiceTest` | Registration / login unit tests |
| `unit/MatchServiceTest` | Scoring rules and date/name boundaries |
| `integration/ApiWorkflowIT` | Real Spring context: security, validation, full report→claim→recovery workflow |

Postman: import `postman/Lost_And_Found_API_Collection.json`, then run the collection (Runner) in order with the backend
running and demo data loaded. It contains ~65 requests with assertions, including negative and security cases.

## 6. First run checklist (do this before demonstrating)
1. `mvn test` — fix compile/test failures if any; keep the surefire report as evidence in `testing/Test_Reports/`.
2. Start MySQL + backend; confirm Swagger loads and demo data was seeded.
3. Run the Postman collection; export the run summary into `testing/Test_Execution/`.
4. Start the frontend, log in as each role and capture screenshots into `screenshots/` (suggested list in `screenshots/README.md`).
5. Fill in the execution columns of the test design documents and write `Conclusion.md` from the real results.

## Documentation map
`documentation/` (overview, modules, database design, API, **business rules**) · `testing/` (plan, EP/BVA/decision-table/
state-transition designs, execution + defect templates) · `postman/` · `database/schema.sql`.

## Engineering decisions (where the brief left room)
- **Withdraw = close.** `DELETE /api/lost-items/{id}` closes the report (status `CLOSED`) instead of deleting rows, preserving history.
- **Roles.** Moderators may review reports, confirm/dismiss matches and *escalate* claims; only administrators approve/reject/reconsider claims and manage recoveries.
- **Found-item privacy.** `identifyingCharacteristics` (and reporter identity) are returned only to staff and the report's owner.
- **Auto-rejection.** Approving one claim auto-rejects other open claims on the same item.
- **Forgot password** is an interface only (generic response); e-mail delivery is out of scope.
- **Image storage** is the local `uploads/` folder, served at `/uploads/**`.
- **Concurrency.** Claim submission/approval and recovery lock the found-item row (`PESSIMISTIC_WRITE`).
