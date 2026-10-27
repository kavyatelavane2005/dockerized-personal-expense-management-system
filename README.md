# Dockerized Personal Expense Management System

A Spring Boot application being developed as an academic DevOps project. The current starter endpoint confirms that the web application starts; the expense-management MVP features are planned for subsequent tasks.

## Technology

- Java 17 or later
- Spring Boot and Maven Wrapper
- Embedded Tomcat
- Docker, Jenkins, Selenium, and Ansible are planned DevOps tools

## Run locally

In PowerShell, from this folder:

```powershell
.\mvnw.cmd package
.\mvnw.cmd spring-boot:run
```

The application listens on [http://localhost:8081](http://localhost:8081). The starter endpoint currently displays: `Personal Expense Management System is running`.

## Planned MVP

- Account setup
- Expense entry and input validation
- Expense list and summary
- Downloadable transaction status or receipt

## Branch naming

Use `feature/task-<number>-<short-name>` for features, `bugfix/<short-name>` for fixes, and `docs/<short-name>` for documentation. Keep branch names lowercase and use hyphens between words. Merge reviewed changes into `main`.

## Project structure

- `src/main/java` — application source code
- `src/main/resources` — application configuration
- `src/test/java` — automated tests
- `Evidence/` — task evidence and build logs

## Status

The starter project builds successfully and the root endpoint has been verified locally. Feature implementation is in progress.
