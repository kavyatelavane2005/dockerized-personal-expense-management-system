# Task 11 — Docker Image and Container Lifecycle

## Image build

Command: `docker build -t spendwise-expense-manager:v0.1.0 .`

Result: BuildKit completed successfully. Maven packaged the Spring Boot WAR with `-DskipTests`; the Selenium suite is run by the Jenkins test gate.

Image: `spendwise-expense-manager:v0.1.0`
Image ID: `sha256:7f96ac3331d29dd8c0f362e0e55970c29948b7049932834c14c1067964b0e411`
Image size: `465,133,778` bytes (about 444 MiB)

## Container run and app verification

Command: `docker run -d --name spendwise-task11 -p 8082:8080 spendwise-expense-manager:v0.1.0`

Final container ID: `54b4df5d28064e44282776b97cd0722f5e123fb0fc93876395e1b0299fc991a4`
Status: `running`
Port mapping: host `8082` to container `8080`
Container user: `app` (non-root)
Health check: `curl.exe --silent --show-error --output NUL --write-out 'HTTP %{http_code}\n' http://127.0.0.1:8082/` returned `HTTP 200`.
Startup log: Spring Boot started and embedded Tomcat listened on port `8080`.

App URL: http://localhost:8082/

## Lifecycle demonstration

- `docker stop spendwise-task11` → container status `exited`.
- `docker start spendwise-task11` → container status `running`; after startup, the app returned `HTTP 200` again.
- `docker stop spendwise-task11` then `docker rm spendwise-task11` → disposable container removed.
- Started a fresh `spendwise-task11` container from the same image; it is left running for the demo at `http://localhost:8082/`.

## Screenshots to add to the Word evidence document

1. Docker Desktop → Images showing `spendwise-expense-manager:v0.1.0`.
2. Docker Desktop → Containers showing `spendwise-task11` running and port `8082:8080`.
3. Browser showing the app at `http://localhost:8082/`.
4. PowerShell output showing the image build, HTTP 200 check, and stop/start/remove commands.

The app keeps its account and expenses in memory; they reset when the container restarts.