pipeline {
    agent any
    tools {
        // Specify the JDK version
        jdk 'openjdk-17.0.2'
    }
    parameters {
     choice(
                 choices: ['t5', 'p1', 't1', 't3', 't4', 't6', 'perf', 'perf1', 'prod'],
                 description: 'Select a target environment.',
                 name: 'ENVIRONMENT')

     string (name:"RELEASE", defaultValue: '', description: 'Please enter release build number.')
     string (name:"FAILED_BUILD", defaultValue: '', description: 'Please enter failed jenkins build number to retest failed test cases only.')
     string (name:"THREAD_COUNT", defaultValue: '', description: 'Please enter thread count to execute test cases as per provided count.')

    }
    environment {
        USERS = '10'
        RAMP_DURATION = '10'
        MAVEN_OPTS = '-DskipTests'
        AUTOMATION_ENVIRONMENT = "${params.ENVIRONMENT}"
        JENKINS_BUILD = "${currentBuild.number}"
        WORKSPACE_PATH = "${WORKSPACE}"
        RELEASE = "${params.RELEASE}"
    }

    stages {
        stage('Checkout') {
            steps {
                git 'https://your-repo-url.git' // Replace with your repository URL
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package'
            }
        }

        stage('Run Simulation') {
            steps {
                sh 'mvn gatling:test'
            }
        }

        stage('Generate Allure Report') {
            steps {
                // Generate Allure report
                sh 'mvn allure:report'
                // Archive the Allure report
                allure([
                    reportPath: 'target/allure-results'
                ])
            }
        }

        stage('Publish Reports') {
            steps {
                archiveArtifacts artifacts: 'target/gatling/**/*.html', allowEmptyArchive: true
                publishHTML(target: [
                    reportName: 'Gatling Report',
                    reportDir: 'target/gatling',
                    reportFiles: '*.html',
                    keepAll: true,
                    alwaysLinkToLastBuild: true
                ])
            }
        }
    }

    post {
        always {
            gatlingArchive(),
            cleanWs()
        }
        success {
            echo 'Simulation run was successful!'
        }
        failure {
            echo 'Simulation run failed!'
        }
    }
}
