# Task 10 Local Gate Verification

**Command:** `mvnw.cmd -B verify` (without `clean`, because the running local Spring Boot process holds files under `target/classes`).
**Result:** `BUILD SUCCESS`.
**Tests:** 5 run, 0 failures, 0 errors, 0 skipped.

The Jenkinsfile now uses `mvnw.cmd -B clean verify`; the Jenkins job must first be pointed at this feature branch. The local Windows file-lock issue did not affect `verify`. Jenkins execution and JUnit publication are still pending.

The full terminal log is `local-test-pass.log`; Surefire reports are in `local-reports/`.

## Deliberate regression check (local)

A temporary defect changed the total-spending reducer from addition to subtraction. The Selenium assertion initially searched the entire metrics section and did not isolate the total card; it was strengthened to assert the total and monthly cards independently. The rerun failed as expected: 5 tests, 1 failure. The failure states `Total spending should show the sum of all expense amounts` and expected `₹123.45`, but received `₹-123.45`.

Evidence: `local-test-failure.log`, `local-failure-reports/`, and `screenshots/SJ-02-total-regression-failure.png`. The defect has been corrected by restoring `BigDecimal::add`. The corrected suite passed locally: 5 tests, 0 failures, 0 errors, 0 skipped. This run used `mvnw.cmd -B verify` because the local app holds files under `target/classes`; the correction commit still needs its Jenkins rerun evidence.


## Jenkins run evidence

The supplied Jenkins screenshot shows build #3 ending in FAILURE (`script returned exit code 1`). The post block reached `junit` and recorded test results; the Checks API line reports no suitable optional checks publisher. Screenshot saved as `screenshots/jenkins-build-3-failure.png`.
