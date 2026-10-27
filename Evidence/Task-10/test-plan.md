# Task 10: Continuous Testing in Jenkins

## Pipeline quality gate

The Jenkinsfile runs `mvnw.cmd -B clean verify` before the Archive WAR and Deploy to Tomcat stages. A failing Maven test returns a non-zero status, so Declarative Pipeline skips deployment. The `post { always }` block publishes `target/surefire-reports/TEST-*.xml` as Jenkins JUnit results and archives any Selenium failure screenshots.

## Test suite

Four headless Microsoft Edge Selenium journeys cover account setup, expense entry and totals, negative amount validation, and CSV/receipt downloads. Maven Surefire is configured to fail if it discovers no tests.

## Deliberate defect demonstration

A temporary incorrect expense-total calculation will be committed and run to demonstrate that the Selenium suite fails before deployment. The correction commit restores the correct total calculation. Local failure/pass logs and screenshots are saved here; Jenkins build screenshots and published test results should be added after running the branch in Jenkins.

## Jenkins branch

Configure the existing Pipeline job's SCM branch to `feature/task-10-continuous-testing`, then build once with the deliberate defect and again after the correction commit. The successful run should show a passed Test stage, JUnit results, archived WAR, and deployment.

## Deliberate failure expected

The overall spending card is asserted independently from monthly spending. A deliberately changed total reducer should fail this exact-value assertion. Jenkins must show the Test stage failed, publish JUnit results, archive the failure screenshot, and skip Archive WAR / Deploy to Tomcat. After the reducer correction commit, rerun and verify those later stages succeed.
