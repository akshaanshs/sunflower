pipeline {
    agent any

    environment {
        ANDROID_HOME = "C:\\Users\\aksha\\AppData\\Local\\Android\\Sdk"
        ANDROID_SDK_ROOT = "C:\\Users\\aksha\\AppData\\Local\\Android\\Sdk"
        JAVA_HOME = "C:\\Program Files\\Eclipse Adoptium\\jdk-17.0.19.10-hotspot"
        PATH = "${JAVA_HOME}\\bin;${ANDROID_HOME}\\platform-tools;${ANDROID_HOME}\\tools;${env.PATH}"
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out Android source code...'
                checkout scm
            }
        }

        stage('Verify Environment') {
            steps {
                echo 'Verifying build environment...'
                bat 'java -version'
                bat 'echo ANDROID_HOME = %ANDROID_HOME%'
                bat 'dir "%ANDROID_HOME%\\build-tools"'
            }
        }

        stage('Clean') {
            steps {
                echo 'Cleaning previous build artifacts...'
                bat 'gradlew.bat clean'
            }
        }

        stage('Build Debug APK') {
            steps {
                echo 'Building Debug APK...'
                bat 'gradlew.bat assembleDebug'
            }
        }

        stage('Run Unit Tests') {
            steps {
                echo 'Running unit tests...'
                bat 'gradlew.bat testDebugUnitTest'
            }
        }

        stage('Archive APK') {
            steps {
                echo 'Archiving APK artifact...'
                archiveArtifacts(
                    artifacts: 'app/build/outputs/apk/debug/app-debug.apk',
                    fingerprint: true,
                    onlyIfSuccessful: true
                )
                echo "APK archived successfully"
                echo "Build Number: ${BUILD_NUMBER}"
            }
        }
    }

    post {
        success {
            echo "Android Pipeline #${BUILD_NUMBER} completed successfully!"
            echo "APK available at: app/build/outputs/apk/debug/app-debug.apk"
            mail(
                to: 'akshaanshs@gmail.com',
                subject: "SUCCESS: Android Build #${BUILD_NUMBER}",
                body: "Job: ${JOB_NAME}\nBuild: ${BUILD_NUMBER}\nAPK: app-debug.apk\nStatus: SUCCESS\nURL: ${BUILD_URL}"
            )
        }
        failure {
            echo "Android Pipeline #${BUILD_NUMBER} failed!"
            mail(
                to: 'akshaanshs@gmail.com',
                subject: "FAILED: Android Build #${BUILD_NUMBER}",
                body: "Job: ${JOB_NAME}\nBuild: ${BUILD_NUMBER}\nStatus: FAILED\nURL: ${BUILD_URL}console"
            )
        }
        always {
            echo 'Android pipeline finished.'
        }
    }
}