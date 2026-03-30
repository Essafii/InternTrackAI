// ─── InternTrackAI — CI/CD Pipeline ─────────────────────────────────────────
// Branch strategy:  develop → staging image, main → production image
// Required Jenkins credentials:
//   dockerhub-creds  — DockerHub username/password
//   jwt-secret       — Secret text: JWT_SECRET value for integration tests
//
// Required Jenkins plugins: Docker Pipeline, Pipeline Utility Steps,
//   OWASP Dependency-Check, HTML Publisher

pipeline {
    agent any

    environment {
        DOCKER_REGISTRY  = 'messafi2'
        IMAGE_BACKEND    = "${DOCKER_REGISTRY}/interntrackai-backend"
        GIT_COMMIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
        IMAGE_TAG        = "${env.BRANCH_NAME == 'main' ? 'latest' : 'develop'}-${GIT_COMMIT_SHORT}"
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {

        // ── 1. Checkout ─────────────────────────────────────────────────────
        stage('Checkout') {
            steps {
                checkout scm
                echo "Building branch: ${env.BRANCH_NAME} — commit: ${GIT_COMMIT_SHORT}"
            }
        }

        // ── 2. Build & Unit Tests ───────────────────────────────────────────
        stage('Build & Test') {
            steps {
                sh '''
                    mvn clean verify \
                        -B -q \
                        -Dspring.profiles.active=test \
                        -Dmaven.test.failure.ignore=false
                '''
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                    jacoco(
                        execPattern: '**/target/jacoco.exec',
                        classPattern: '**/target/classes',
                        sourcePattern: '**/src/main/java'
                    )
                }
            }
        }

        // ── 3. SAST — Semgrep ───────────────────────────────────────────────
        stage('SAST — Semgrep') {
            steps {
                sh '''
                    if command -v semgrep >/dev/null 2>&1; then
                        semgrep \
                            --config=devsecops/semgrep-rules.yml \
                            --config=p/java \
                            --config=p/spring-boot \
                            --json \
                            --output=target/semgrep-report.json \
                            src/main/java
                    else
                        echo "[WARN] semgrep not installed — skipping SAST scan"
                    fi
                '''
            }
            post {
                always {
                    archiveArtifacts artifacts: 'target/semgrep-report.json', allowEmptyArchive: true
                }
            }
        }

        // ── 4. SCA — OWASP Dependency Check ────────────────────────────────
        stage('SCA — OWASP Dependency Check') {
            steps {
                dependencyCheck(
                    additionalArguments: '''
                        --project "InternTrackAI"
                        --format HTML
                        --format JSON
                        --out target/owasp
                        --failBuildOnCVSS 9
                    ''',
                    odcInstallation: 'OWASP-DC'
                )
            }
            post {
                always {
                    dependencyCheckPublisher pattern: 'target/owasp/dependency-check-report.xml'
                }
            }
        }

        // ── 5. Docker Build ─────────────────────────────────────────────────
        stage('Docker Build') {
            steps {
                sh "docker build -t ${IMAGE_BACKEND}:${IMAGE_TAG} -f docker/Dockerfile.backend ."
                sh "docker tag ${IMAGE_BACKEND}:${IMAGE_TAG} ${IMAGE_BACKEND}:${env.BRANCH_NAME == 'main' ? 'latest' : 'develop'}"
            }
        }

        // ── 6. Image Scan — Trivy ───────────────────────────────────────────
        stage('Image Scan — Trivy') {
            steps {
                sh '''
                    if command -v trivy >/dev/null 2>&1; then
                        trivy image \
                            --exit-code 1 \
                            --severity CRITICAL \
                            --no-progress \
                            --format json \
                            --output target/trivy-report.json \
                            ''' + "${IMAGE_BACKEND}:${IMAGE_TAG}" + '''
                    else
                        echo "[WARN] trivy not installed — skipping image scan"
                    fi
                '''
            }
            post {
                always {
                    archiveArtifacts artifacts: 'target/trivy-report.json', allowEmptyArchive: true
                }
            }
        }

        // ── 7. Push to DockerHub ────────────────────────────────────────────
        stage('Push to DockerHub') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-creds',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'
                    sh "docker push ${IMAGE_BACKEND}:${IMAGE_TAG}"
                    sh "docker push ${IMAGE_BACKEND}:${env.BRANCH_NAME == 'main' ? 'latest' : 'develop'}"
                }
            }
        }

        // ── 8. Deploy to Kubernetes (Minikube) ─────────────────────────────
        stage('Deploy') {
            when {
                branch 'develop'
            }
            steps {
                sh """
                    kubectl set image deployment/interntrackai-backend \
                        backend=${IMAGE_BACKEND}:${IMAGE_TAG} \
                        --record
                    kubectl rollout status deployment/interntrackai-backend \
                        --timeout=120s
                """
            }
        }

    }

    post {
        always {
            sh 'docker logout || true'
            sh "docker rmi ${IMAGE_BACKEND}:${IMAGE_TAG} || true"
            cleanWs()
        }
        success {
            echo "Pipeline completed — image: ${IMAGE_BACKEND}:${IMAGE_TAG}"
        }
        failure {
            echo "Pipeline FAILED on branch ${env.BRANCH_NAME} — commit ${GIT_COMMIT_SHORT}"
        }
    }
}
