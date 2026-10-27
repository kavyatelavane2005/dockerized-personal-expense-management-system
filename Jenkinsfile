pipeline {
    agent any

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
                    powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -Command "$uri='http://localhost:%TOMCAT_PORT%/expense-manager-%DEPLOY_ENV%/'; $ready=$false; for($i=0; $i -lt 30; $i++){ try { $response=Invoke-WebRequest -Uri $uri -UseBasicParsing -TimeoutSec 3; if($response.StatusCode -eq 200 -and $response.Content.Contains('Spendwise')) { $ready=$true; break } } catch { }; Start-Sleep -Seconds 2 }; if(-not $ready){ Write-Error ('Health check failed: ' + $uri); exit 1 }"
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
            echo 'Tests passed and deployment completed successfully.'
            echo 'Open the deployed application at http://localhost:8090/expense-manager-<DEPLOY_ENV>/.'
        }
    }
}
