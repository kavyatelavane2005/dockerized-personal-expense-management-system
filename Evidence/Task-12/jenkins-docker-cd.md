# Task 12 — Jenkins-Docker Continuous Deployment

## Pipeline implementation

The Task 12 Jenkinsfile keeps the `mvnw.cmd -B clean verify` quality gate and the existing Tomcat deployment. After the tests pass, it starts/checks a local registry, builds and pushes a build-numbered image, pulls that image, replaces the `spendwise-cd` container, and checks its HTTP health.

Registry: `localhost:5000` (bound to loopback; persistent Docker volume `spendwise-registry-data`)
Image pattern: `localhost:5000/spendwise-expense-manager:v0.1.<Jenkins BUILD_NUMBER>`
Container: `spendwise-cd`
Docker app URL: `http://127.0.0.1:8083/`

## Local Docker verification

- `docker build -t localhost:5000/spendwise-expense-manager:v0.1.manual .` — completed successfully.
- `docker push localhost:5000/spendwise-expense-manager:v0.1.manual` — pushed successfully.
- `docker pull localhost:5000/spendwise-expense-manager:v0.1.manual` — pulled successfully from the registry.
- Started `spendwise-cd` from the pulled image with host port `8083` mapped to container port `8080`.
- HTTP health check at `http://127.0.0.1:8083/` returned `200`; container status was `running`.

## Jenkins run status

- Jenkins build: `#8` — `SUCCESS`.
- Jenkins reported tests passed and both Tomcat and Docker deployments completed.
- Published image: `localhost:5000/spendwise-expense-manager:v0.1.8`.
- Docker URL: `http://127.0.0.1:8083/`.
- Post-build verification: container `spendwise-cd` is running from tag `v0.1.8`; HTTP health check returned `200`. Local registry API returned `200`.
- Jenkins SCM branch: `feature/task-12-jenkins-docker-cd`.

## Screenshots to add to the Word evidence document

1. Local registry container and published versioned image in Docker Desktop.
2. Jenkins stage view/console showing tests, image push, fresh container deploy, health check, and final `SUCCESS`.
3. Browser showing `http://127.0.0.1:8083/` after the Jenkins deployment.

The local registry is for this machine’s development/demo pipeline; it is not a public registry.