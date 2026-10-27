# Dockerized Personal Expense Management System

A Spring Boot application being developed as an academic DevOps project. This branch adds the first expense-entry workflow. Expense records are currently held in memory and are cleared when the application restarts; persistent storage will be added in a later task.

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

Open [http://localhost:8081](http://localhost:8081), enter an amount, date, category, and description, then select **Add expense**. The app validates required values and positive amounts before showing the saved entry.

## Branch naming

Use `feature/task-<number>-<short-name>` for features, `bugfix/<short-name>` for fixes, and `docs/<short-name>` for documentation. Keep branch names lowercase and use hyphens between words. Merge reviewed changes into `development`; merge completed releases into `main`.

## Project structure

- `src/main/java` — application source code
- `src/main/resources` — application configuration
- `src/test/java` — automated tests
- `Evidence/` — task evidence and build logs
