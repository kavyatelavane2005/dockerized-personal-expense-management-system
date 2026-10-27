# Dockerized Personal Expense Management System

A Spring Boot MVP for setting up one demo profile, recording expenses, reviewing category totals, and downloading receipts or a CSV summary.

## Run locally

Requires Java 17 or later. In PowerShell, from the project folder:

    .\mvnw.cmd -DskipTests package
    .\mvnw.cmd spring-boot:run

Open http://localhost:8081.

## MVP features

- Single account profile with name/email validation and profile editing
- Expense entry with positive amount, date, category, and description validation
- Responsive dashboard with spending KPIs, category bars, and recent transactions
- Downloadable per-expense text receipt and CSV summary
- HTML escaping for user-entered account and expense text

## Current limitation

This learning MVP stores the account and expenses in memory. Data resets when the application restarts; authentication and persistent database storage are planned for later work.

## Branch naming

Use feature/task-<number>-<short-name> for features, bugfix/<short-name> for fixes, and docs/<short-name> for documentation. Merge reviewed changes into development; merge completed releases into main.

## Project folders

- src/main/java — Spring Boot application and web endpoints
- src/main/resources — application configuration
- src/test/java — automated tests
- Evidence/ — task evidence and build/verification logs


## Task 8 deployment

The Jenkinsfile builds an executable/deployable WAR, archives it, and deploys it to Apache Tomcat 11. The DEPLOY_ENV parameter controls the deployment context (development or staging). The demo Tomcat instance listens on port 8090. See Evidence/Task-08 for deployment notes and evidence.


## Task 9 Selenium tests

Run `.\mvnw.cmd test` to start the Spring Boot test server and execute the Selenium journeys in Microsoft Edge. Test cases cover account setup, valid expense entry and summary totals, negative amount validation, CSV export, and receipt download. Failure screenshots are saved under `Evidence/Task-09/failure-screenshots`; Surefire reports are generated under `target/surefire-reports`.

## Task 10 continuous testing

The Jenkins pipeline runs `mvnw.cmd -B clean verify` before archiving or deployment. Maven test failures stop later stages. Jenkins publishes Surefire XML results and archives failure screenshots from `Evidence/Task-09/failure-screenshots/`. Configure the Jenkins Pipeline job to build the `feature/task-10-continuous-testing` branch for this task.
