# Task 9 Local Test Report

**Command:** `./mvnw.cmd test`  
**Result:** BUILD SUCCESS  
**Tests:** 5 total (1 Spring context smoke test and 4 Selenium journeys)  
**Failures:** 0  
**Errors:** 0  
**Skipped:** 0  
**Browser:** Microsoft Edge, headless via Selenium WebDriver  
**App server:** Spring Boot test server on a random port

## Selenium journeys passed

1. Create a demo account and verify its success message and profile details.
2. Add an expense and verify the transaction, category summary, and total.
3. Enter a negative amount and verify browser validation blocks submission without adding a transaction.
4. Download the CSV and receipt and verify their contents contain the test expense and account details.

Full run output is in `local-test-run.log`. Surefire text and XML reports are in `reports/`. If a Selenium test fails, its screenshot is saved under `failure-screenshots/`.
## Screenshot evidence

- `screenshots/SJ-01-account-created.png` — account setup confirmation and profile.
- `screenshots/SJ-02-expense-summary.png` — valid expense, dashboard total, and category summary.
- `screenshots/SJ-03-invalid-amount-blocked.png` — browser validation for a negative amount; transaction count remains unchanged.
- `screenshots/SJ-04-csv-and-receipt-download.png` — dashboard after both downloaded files were verified.
