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
            }
        }

        stage('Clean') {
            steps {
                echo 'Cleaning previous build artifacts...'
                bat 'gradlew.bat clean'
            }
        }

        stage('Android Lint') {
            steps {
                echo 'Running Android Lint checks...'
                bat 'gradlew.bat lintDebug'
                echo 'Lint checks completed'
            }
            post {
                always {
                    archiveArtifacts(
                        artifacts: 'app/build/reports/lint-results-debug.html',
                        allowEmptyArchive: true
                    )
                }
            }
        }

        stage('Build Variants') {
            parallel {
                stage('Build Debug APK') {
                    steps {
                        echo 'Building Debug APK...'
                        bat 'gradlew.bat assembleDebug'
                        echo 'Debug APK built successfully'
                    }
                }
                stage('Build Release APK') {
                    steps {
                        echo 'Building Release APK...'
                        bat 'gradlew.bat assembleRelease'
                        echo 'Release APK built successfully'
                    }
                }
            }
        }

        stage('Run Unit Tests') {
            steps {
                echo 'Running unit tests...'
                bat 'gradlew.bat testDebugUnitTest'
            }
        }

        stage('Archive Artifacts') {
            steps {
                echo 'Archiving build artifacts...'
                archiveArtifacts(
                    artifacts: 'app/build/outputs/apk/debug/app-debug.apk',
                    fingerprint: true,
                    onlyIfSuccessful: true
                )
                archiveArtifacts(
                    artifacts: 'app/build/outputs/apk/release/app-release-unsigned.apk',
                    fingerprint: true,
                    onlyIfSuccessful: true
                )
                echo "Build Number: ${BUILD_NUMBER}"
                echo "Both APKs archived successfully"
            }
        }
    }

    post {
        success {
            echo "Android Pipeline #${BUILD_NUMBER} completed successfully!"
            mail(
                to: 'akshaanshs@gmail.com',
                subject: "SUCCESS: Android Build #${BUILD_NUMBER}",
                body: "Job: ${JOB_NAME}\nBuild: ${BUILD_NUMBER}\nDebug APK: app-debug.apk\nRelease APK: app-release-unsigned.apk\nStatus: SUCCESS\nURL: ${BUILD_URL}"
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