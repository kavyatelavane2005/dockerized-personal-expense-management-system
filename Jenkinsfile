pipeline {
    agent { label 'windows-edge' }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    parameters {
        choice(
            name: 'DEPLOY_ENV',
            choices: ['development', 'staging'],
            description: 'Select the Tomcat context to deploy to.'
        )
    }

    environment {
        TOMCAT_HOME = 'C:\\DevTools\\Tomcat\\apache-tomcat-11.0.27'
        TOMCAT_PORT = '8090'
        DOCKER_REGISTRY = 'localhost:5000'
        DOCKER_REPOSITORY = 'spendwise-expense-manager'
        DOCKER_CONTAINER = 'spendwise-cd'
        DOCKER_PORT = '8083'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build, test and package') {
            steps {
                bat '''
                    set "JAVA_HOME=C:\\DevTools\\jdk-21"
                    set "PATH=%JAVA_HOME%\\bin;%PATH%"
                    call mvnw.cmd -B clean verify
                '''
            }
        }

        stage('Archive WAR') {
            steps {
                archiveArtifacts artifacts: 'target/*.war', fingerprint: true
            }
        }

        stage('Deploy to Tomcat') {
            steps {
                bat '''
                    if not exist "%TOMCAT_HOME%\\webapps" (
                        echo Tomcat webapps folder was not found: %TOMCAT_HOME%\\webapps
                        exit /b 2
                    )
                    copy /Y "target\\demo-0.0.1-SNAPSHOT.war" "%TOMCAT_HOME%\\webapps\\expense-manager-%DEPLOY_ENV%.war"
                    if errorlevel 1 exit /b 1
                '''
                bat '''
                    powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -Command "$port=%TOMCAT_PORT%; if(-not (Test-NetConnection -ComputerName 127.0.0.1 -Port $port -InformationLevel Quiet)){ $tomcat='%TOMCAT_HOME%'; Start-Process -FilePath 'cmd.exe' -ArgumentList '/c','startup.bat' -WorkingDirectory (Join-Path $tomcat 'bin') -WindowStyle Hidden }"
                '''
                bat '''
                    powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -Command "$uri='http://localhost:%TOMCAT_PORT%/expense-manager-%DEPLOY_ENV%/'; $ready=$false; for($i=0; $i -lt 30; $i++){ try { $response=Invoke-WebRequest -Uri $uri -UseBasicParsing -TimeoutSec 3; if($response.StatusCode -eq 200 -and $response.Content.Contains('Spendwise')) { $ready=$true; break } } catch { }; Start-Sleep -Seconds 2 }; if(-not $ready){ Write-Error ('Health check failed: ' + $uri); exit 1 }"
                '''
            }
        }

        stage('Ensure local Docker registry') {
            steps {
                bat '''
                    docker start spendwise-local-registry >NUL 2>&1
                    if errorlevel 1 (
                        docker volume create spendwise-registry-data
                        if errorlevel 1 exit /b 1
                        docker run -d --name spendwise-local-registry --restart unless-stopped -p 127.0.0.1:5000:5000 -v spendwise-registry-data:/var/lib/registry registry:2
                        if errorlevel 1 exit /b 1
                    )
                    curl.exe --fail --silent --show-error http://127.0.0.1:5000/v2/ >NUL
                    if errorlevel 1 exit /b 1
                '''
            }
        }

        stage('Build and publish Docker image') {
            steps {
                bat '''
                    set "IMAGE_TAG=%DOCKER_REGISTRY%/%DOCKER_REPOSITORY%:v0.1.%BUILD_NUMBER%"
                    docker build -t "%IMAGE_TAG%" .
                    if errorlevel 1 exit /b 1
                    docker push "%IMAGE_TAG%"
                    if errorlevel 1 exit /b 1
                '''
            }
        }

        stage('Deploy fresh Docker container') {
            steps {
                bat '''
                    set "IMAGE_TAG=%DOCKER_REGISTRY%/%DOCKER_REPOSITORY%:v0.1.%BUILD_NUMBER%"
                    docker pull "%IMAGE_TAG%"
                    if errorlevel 1 exit /b 1
                    docker stop "%DOCKER_CONTAINER%" >NUL 2>&1
                    docker rm "%DOCKER_CONTAINER%" >NUL 2>&1
                    docker run --detach --name "%DOCKER_CONTAINER%" --restart unless-stopped --publish 127.0.0.1:%DOCKER_PORT%:8080 "%IMAGE_TAG%"
                    if errorlevel 1 exit /b 1
                '''
                bat '''
                    powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -Command "$uri='http://127.0.0.1:%DOCKER_PORT%/'; $ready=$false; for($i=0; $i -lt 45; $i++){ try { $response=Invoke-WebRequest -Uri $uri -UseBasicParsing -TimeoutSec 3; if($response.StatusCode -eq 200 -and $response.Content.Contains('Spendwise')) { $ready=$true; break } } catch { }; Start-Sleep -Seconds 2 }; if(-not $ready){ Write-Error ('Docker deployment health check failed: ' + $uri); exit 1 }"
                '''
            }
        }
    }

    post {
        always {
            junit testResults: 'target/surefire-reports/TEST-*.xml', allowEmptyResults: true
            archiveArtifacts artifacts: 'target/*.war', fingerprint: true, allowEmptyArchive: true
        }
        unsuccessful {
            archiveArtifacts artifacts: 'Evidence/Task-09/failure-screenshots/**/*.png', fingerprint: true, allowEmptyArchive: true
        }
        success {
            echo 'Tests passed; Tomcat and Docker deployments completed successfully.'
            echo "Tomcat app: http://localhost:8090/expense-manager-${params.DEPLOY_ENV}/"
            echo "Docker app: http://127.0.0.1:${env.DOCKER_PORT}/"
            echo "Published image: ${env.DOCKER_REGISTRY}/${env.DOCKER_REPOSITORY}:v0.1.${env.BUILD_NUMBER}"
        }
    }
}