pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
    }

    environment {
        IMAGE_NAME = 'zakariael3/interntrackai-backend:latest'
        SONAR_SERVER = 'sonarqube-local'
        SONAR_PROJECT_KEY = 'interntrackai-backend'
        SONAR_PROJECT_NAME = 'InternTrackAI Backend'
        K8S_NAMESPACE = 'staging'
        DOCKERFILE_PATH = 'docker/Dockerfile.backend'
        TRIVY_VERSION = '0.69.3'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh '''
                echo "Construction du projet..."
                chmod +x mvnw || true
                if [ -f mvnw ]; then
                  ./mvnw -B -DskipTests clean compile
                else
                  mvn -B -DskipTests clean compile
                fi
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                echo "Execution des tests..."
                chmod +x mvnw || true
                if [ -f mvnw ]; then
                  ./mvnw -B test
                else
                  mvn -B test
                fi
                '''
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('SonarQube Scan') {
            steps {
                withSonarQubeEnv("${SONAR_SERVER}") {
                    withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                        sh '''
                        echo "Analyse SonarQube..."
                        chmod +x mvnw || true
                        if [ -f mvnw ]; then
                          ./mvnw -B verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                            -Dsonar.projectKey=$SONAR_PROJECT_KEY \
                            -Dsonar.projectName="$SONAR_PROJECT_NAME" \
                            -Dsonar.token=$SONAR_TOKEN
                        else
                          mvn -B verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                            -Dsonar.projectKey=$SONAR_PROJECT_KEY \
                            -Dsonar.projectName="$SONAR_PROJECT_NAME" \
                            -Dsonar.token=$SONAR_TOKEN
                        fi
                        '''
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Security Tests Auto') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
                    timeout(time: 10, unit: 'MINUTES') {
                        sh '''
                        echo "Analyse OWASP Dependency-Check..."
                        chmod +x mvnw || true
                        if [ -f mvnw ]; then
                          ./mvnw -B org.owasp:dependency-check-maven:check \
                            -Dformat=HTML \
                            -DfailBuildOnCVSS=11
                        else
                          mvn -B org.owasp:dependency-check-maven:check \
                            -Dformat=HTML \
                            -DfailBuildOnCVSS=11
                        fi
                        '''
                    }
                }
            }
            post {
                always {
                    archiveArtifacts allowEmptyArchive: true, artifacts: '**/target/dependency-check-report.*'
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                echo "Build image Docker..."
                docker pull eclipse-temurin:17-jdk-alpine
                docker build --no-cache -t $IMAGE_NAME -f $DOCKERFILE_PATH .
                '''
            }
        }

        stage('Image Security Scan') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
                    sh '''
                    set -o pipefail
                    echo "Scan Trivy de l'image Docker..."
                    docker run --rm \
                      -v /var/run/docker.sock:/var/run/docker.sock \
                      aquasec/trivy:$TRIVY_VERSION image \
                      --severity HIGH,CRITICAL \
                      --ignore-unfixed \
                      --exit-code 1 \
                      $IMAGE_NAME | tee trivy-image-report.txt
                    '''
                }
            }
            post {
                always {
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'trivy-image-report.txt'
                }
            }
        }

        stage('Docker Push') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-creds',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh '''
                    echo "Push image DockerHub..."
                    echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin
                    docker push $IMAGE_NAME
                    '''
                }
            }
        }

        stage('Deploy Staging') {
            steps {
                withCredentials([file(credentialsId: 'kubeconfig-staging', variable: 'KUBECONFIG_FILE')]) {
                    sh '''
                    echo "Deploiement Kubernetes sur staging..."
                    export KUBECONFIG=$KUBECONFIG_FILE

                    kubectl get ns $K8S_NAMESPACE || kubectl create ns $K8S_NAMESPACE

                    kubectl apply -f k8s/postgres.yaml -n $K8S_NAMESPACE
                    kubectl apply -f k8s/deployment.yaml -n $K8S_NAMESPACE
                    kubectl apply -f k8s/service.yaml -n $K8S_NAMESPACE

                    kubectl set image deployment/interntrackai-backend backend=$IMAGE_NAME -n $K8S_NAMESPACE

                    kubectl rollout status deployment/postgres -n $K8S_NAMESPACE --timeout=180s
                    kubectl rollout status deployment/interntrackai-backend -n $K8S_NAMESPACE --timeout=180s

                    kubectl get pods -n $K8S_NAMESPACE
                    kubectl get svc -n $K8S_NAMESPACE
                    '''
                }
            }
        }

        stage('Report') {
            steps {
                echo 'Generation du rapport...'
            }
        }
    }

    post {
        success {
            echo 'Pipeline executee avec succes.'
        }
        unstable {
            echo 'Pipeline terminee avec avertissements.'
        }
        failure {
            echo 'La pipeline a echoue.'
        }
        always {
            sh 'docker logout || true'
        }
    }
}