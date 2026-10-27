# Task 9: Selenium Test Design and Local Execution

## Test environment

- Application: Spendwise personal expense manager
- Test framework: JUnit 5 with Selenium WebDriver for Java
- Browser: Microsoft Edge in headless mode; Selenium Manager locates the driver
- App server: Spring Boot test server on a random local port
- Run command: `.\mvnw.cmd test`

## Test journeys and expected results

| ID | Journey | Test data | Assertions |
| --- | --- | --- | --- |
| SJ-01 | Create a demo account | Selenium Journey User / selenium.journey@example.test | Success message appears and the profile name/email are shown. |
| SJ-02 | Record a valid expense | INR 123.45, category Selenium Test, description selenium-journey-expense | Success message appears; recent transaction, category, and dashboard total contain the new expense. |
| SJ-03 | Reject an invalid amount | Amount -5 | Browser reports a range-underflow validation error; no form submission or new transaction occurs. |
| SJ-04 | Download expense records | CSV and receipt for the test expense | CSV header and expense row are present; downloaded receipt contains the description, account name, and amount. |

## Failure evidence

If a Selenium test fails, the suite captures a browser screenshot in `Evidence/Task-09/failure-screenshots/` using the failing test method name. Maven Surefire writes the local test report to `target/surefire-reports/`.

The invalid amount journey checks the browser's native HTML `min="0.01"` validation. The application also validates amounts server-side; Task 10 will add deployment quality-gate behavior.
Successful browser states are captured as PNGs in `Evidence/Task-09/screenshots/` (SJ-01 through SJ-04). Failed tests save a separate diagnostic screenshot in `Evidence/Task-09/failure-screenshots/`.
