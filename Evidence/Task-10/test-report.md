# Task 10: Continuous Testing in Jenkins

## Pipeline quality gate

The Jenkinsfile runs `mvnw.cmd -B clean verify` before WAR archiving and Tomcat deployment. Maven test failures return a nonzero status and skip the later stages. Jenkins publishes Surefire XML results in the `always` post block and archives failure screenshots on unsuccessful runs. Surefire is configured to fail if it discovers no tests.

## Deliberate regression and correction

A temporary defect changed the total-spending calculation from addition to subtraction. The initial Selenium assertion was too broad because it searched both total and monthly metrics. It now checks the total and monthly cards independently. The regression was caught locally: 5 tests, 1 failure, expected `₹123.45`, received `₹-123.45`. The application fix restores `BigDecimal::add` in correction commit `587e410`; the corrected local suite passed all 5 tests.

Local failure details are in `local-test-failure.log`, reports are in `local-failure-reports/`, and the browser screenshot is `screenshots/SJ-02-total-regression-failure.png`.

## Jenkins build #3 diagnosis

Jenkins checked out correction commit `587e410`, but build #3 failed before the Selenium journeys could run. All four browser sessions returned `SessionNotCreatedException` because Edge crashed under the Jenkins service account (`DevToolsActivePort file doesn't exist`). JUnit recorded 4 Selenium errors and 0 assertion failures. The Archive WAR and Deploy stages were skipped. The Checks API notice only reports that no optional checks publisher was available. The screenshot is `screenshots/jenkins-build-3-failure.png`; the full console output is `jenkins-build-3-console.log`.

The Selenium setup now gives each headless Edge session a unique profile directory and disables first-run checks and extensions. Run Jenkins again to verify Edge startup, JUnit publication, artifact archiving, and deployment.

## Jenkins job

The existing Pipeline job should build branch `feature/task-10-continuous-testing` with Script Path `Jenkinsfile`. Use `DEPLOY_ENV=development` for the deployment verification.

## Edge launch adjustment verification

Added a per-test `--user-data-dir` beneath the unique Selenium download folder and disabled first-run checks/extensions. Local `mvnw.cmd -B verify` passed again: 5 tests, 0 failures, 0 errors, 0 skipped. Log: `local-edge-profile-test.log`; Surefire reports: `local-edge-profile-reports/`. Jenkins build must be rerun to confirm behavior under its service account.
