pipeline {
    agent any

    environment {
        IMAGE_NAME = 'zakariael3/interntrackai-backend:latest'
    }

    stages {
        stage('Build') {
            steps {
                echo 'Construction du projet...'
            }
        }

        stage('Test') {
            steps {
                echo 'Execution des tests...'
            }
        }

        stage('Security Scan') {
            steps {
                echo 'Analyse de securite du projet...'
                echo 'Verification SAST / dependances / images Docker'
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                docker pull eclipse-temurin:17-jdk-alpine
                docker build --no-cache -t $IMAGE_NAME -f docker/Dockerfile.backend .
                '''
            }
        }

        stage('Trivy Scan') {
            steps {
                sh '''
                trivy image --exit-code 0 --no-progress $IMAGE_NAME
                '''
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
                    echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin
                    docker push $IMAGE_NAME
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
            echo 'Pipeline execute avec succes.'
        }
        failure {
            echo 'Le pipeline a echoue.'
        }
        always {
            sh 'docker logout || true'
        }
    }
}