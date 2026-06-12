pipeline {
    agent any

    environment {
        ANDROID_HOME = "C:\\Users\\aksha\\AppData\\Local\\Android\\Sdk"
        ANDROID_SDK_ROOT = "C:\\Users\\aksha\\AppData\\Local\\Android\\Sdk"
        JAVA_HOME = "C:\\Program Files\\Eclipse Adoptium\\jdk-17.0.19.10-hotspot"
        PATH = "${JAVA_HOME}\\bin;${ANDROID_HOME}\\platform-tools;${ANDROID_HOME}\\tools;${env.PATH}"
        FIREBASE_APP_ID = "1:460964823455:android:c47b12e5c3b6351ebd3daa"
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

        stage('Sign Release APK') {
            steps {
                echo 'Signing Release APK...'
                withCredentials([
                    file(credentialsId: 'android-keystore', variable: 'KEYSTORE_FILE'),
                    string(credentialsId: 'android-keystore-password', variable: 'KEYSTORE_PASSWORD')
                ]) {
                    bat "\"${JAVA_HOME}\\bin\\jarsigner.exe\" -verbose -sigalg SHA256withRSA -digestalg SHA-256 -keystore \"%KEYSTORE_FILE%\" -storepass %KEYSTORE_PASSWORD% -keypass %KEYSTORE_PASSWORD% app\\build\\outputs\\apk\\release\\app-release-unsigned.apk sunflower"
                    echo 'APK signed successfully'
                    bat "\"%ANDROID_HOME%\\build-tools\\34.0.0\\zipalign.exe\" -v 4 app\\build\\outputs\\apk\\release\\app-release-unsigned.apk app\\build\\outputs\\apk\\release\\app-release-signed.apk"
                    echo 'APK aligned successfully'
                }
            }
        }

        stage('DAST Security Scan') {
    steps {
        echo 'Starting DAST Security Scan with MobSF...'
        bat 'docker stop mobsf-jenkins 2>nul & docker rm mobsf-jenkins 2>nul & exit 0'
        bat 'docker run -d --name mobsf-jenkins -p 8010:8000 opensecurity/mobile-security-framework-mobsf:latest'
        bat 'ping -n 60 127.0.0.1 > nul'
        bat '''
	@echo off
	docker logs mobsf-jenkins > mobsf_raw.txt 2>&1
	powershell -Command "Get-Content mobsf_raw.txt | Where-Object { $_ -match 'REST API Key:' } | Select-Object -Last 1 | ForEach-	Object { $_ -replace '.*REST API Key: ', '' } | ForEach-Object { $_ -replace '[^a-fA-F0-9]', '' } | ForEach-Object 	{ $_.Substring(0, [Math]::Min(64, $_.Length)) } | Set-Content mobsf_api_key.txt"
	type mobsf_api_key.txt
	'''
        bat '''
@echo off
set /p MOBSF_KEY=<mobsf_api_key.txt
echo Using key: %MOBSF_KEY%
curl -s -F "file=@app/build/outputs/apk/debug/app-debug.apk" http://localhost:8010/api/v1/upload -H "X-Mobsf-Api-Key: %MOBSF_KEY%" -o mobsf_upload.json
echo Upload done
type mobsf_upload.json
'''
        bat '''
@echo off
set /p MOBSF_KEY=<mobsf_api_key.txt
for /f "tokens=2 delims=:," %%a in ('findstr "hash" mobsf_upload.json') do set HASH=%%~a
set HASH=%HASH: =%
set HASH=%HASH:"=%
echo Scan hash: %HASH%
curl -s -X POST http://localhost:8010/api/v1/scan -H "X-Mobsf-Api-Key: %MOBSF_KEY%" -d "scan_type=apk&file_name=app-debug.apk&hash=%HASH%"
echo Scan triggered
'''
        bat 'ping -n 120 127.0.0.1 > nul'
        bat '''
@echo off
set /p MOBSF_KEY=<mobsf_api_key.txt
for /f "tokens=2 delims=:," %%a in ('findstr "hash" mobsf_upload.json') do set HASH=%%~a
set HASH=%HASH: =%
set HASH=%HASH:"=%
echo Downloading PDF for hash: %HASH%
curl -s -X POST http://localhost:8010/api/v1/download_pdf -H "X-Mobsf-Api-Key: %MOBSF_KEY%" -d "hash=%HASH%" -o mobsf-security-report.pdf
echo PDF done
'''
        echo 'DAST Security Scan completed'
    }
    post {
        always {
            archiveArtifacts(
                artifacts: 'mobsf-security-report.pdf',
                allowEmptyArchive: true
            )
            bat 'docker stop mobsf-jenkins 2>nul & docker rm mobsf-jenkins 2>nul & exit 0'
        }
    }
}

        stage('Run Unit Tests') {
            steps {
                echo 'Running unit tests...'
                bat 'gradlew.bat testDebugUnitTest'
            }
        }

        stage('Firebase Test Lab') {
            steps {
                echo 'Running tests on Firebase Test Lab...'
                withCredentials([
                    file(credentialsId: 'firebase-service-account', variable: 'GCLOUD_KEY')
                ]) {
                    bat '"C:\\Users\\aksha\\AppData\\Local\\Google\\Cloud SDK\\google-cloud-sdk\\bin\\gcloud.cmd" auth activate-service-account --key-file="%GCLOUD_KEY%" --project=sunflower-cicd'
                    bat '"C:\\Users\\aksha\\AppData\\Local\\Google\\Cloud SDK\\google-cloud-sdk\\bin\\gcloud.cmd" firebase test android run --type robo --app app\\build\\outputs\\apk\\debug\\app-debug.apk --device model=MediumPhone.arm,version=34,locale=en,orientation=portrait --timeout 3m --project sunflower-cicd'
                }
                echo 'Firebase Test Lab completed successfully'
            }
        }

        stage('Distribute to Firebase') {
            steps {
                echo 'Distributing APK to Firebase App Distribution...'
                withCredentials([
                    file(credentialsId: 'firebase-service-account', variable: 'GOOGLE_APPLICATION_CREDENTIALS')
                ]) {
                    bat '"C:\\Users\\aksha\\AppData\\Roaming\\npm\\firebase.cmd" appdistribution:distribute app\\build\\outputs\\apk\\debug\\app-debug.apk --app %FIREBASE_APP_ID% --release-notes "Build ${BUILD_NUMBER} - Automated build from Jenkins"'
                }
                echo 'APK distributed to Firebase successfully'
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
                    artifacts: 'app/build/outputs/apk/release/app-release-signed.apk',
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
            echo "Android Pipeline ${BUILD_NUMBER} completed successfully!"
            mail(
                to: 'akshaanshs@gmail.com',
                subject: "SUCCESS: Android Build ${BUILD_NUMBER}",
                body: "Job: ${JOB_NAME}\nBuild: ${BUILD_NUMBER}\nAPKs archived\nFirebase Test Lab: Passed\nDAST Security Scan: Completed\nStatus: SUCCESS\nURL: ${BUILD_URL}"
            )
        }
        failure {
            echo "Android Pipeline ${BUILD_NUMBER} failed!"
            mail(
                to: 'akshaanshs@gmail.com',
                subject: "FAILED: Android Build ${BUILD_NUMBER}",
                body: "Job: ${JOB_NAME}\nBuild: ${BUILD_NUMBER}\nStatus: FAILED\nURL: ${BUILD_URL}console"
            )
        }
        always {
            echo 'Android pipeline finished.'
        }
    }
}