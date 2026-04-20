pipeline {
    agent none
    stages {
        stage('Wrong Jenkinsfile') {
            steps {
                error("""
Ce Jenkinsfile est obsolete.
Configurez votre job Jenkins avec :
  Script Path : ci/Jenkinsfile
                """)
            }
        }
    }
}
