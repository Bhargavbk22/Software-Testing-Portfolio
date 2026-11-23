# Test Automation
Code lives in `backend/src/test/java/com/lostfound/` (kept in the backend module so Maven runs it).
```bash
cd backend && mvn test
mvn test surefire-report:report      # target/site/surefire-report.html
mvn -Dtest=ValidationServiceTest test   # a single class
```
Unit tests use Mockito with no database; integration tests (`*IT`) start the full Spring context on in-memory H2 with the `test` profile
(`application-test.yml`) and drive the REST API through MockMvc with real JWTs and real security rules.
Copy the generated report into `testing/Test_Reports/` after each real run.
