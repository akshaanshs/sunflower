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
        bat 'powershell -Command "docker logs mobsf-jenkins 2>&1 | Where-Object { $_ -match \'REST API Key:\' } | Select-Object -First 1 | ForEach-Object { ($_ -split \'REST API Key:\')[1].Trim() -replace \'[^a-fA-F0-9]\',\'\' } | Set-Content mobsf_api_key.txt; Write-Host (\'Key saved: \' + (Get-Content mobsf_api_key.txt))"'
        bat '@echo $key = (Get-Content mobsf_api_key.txt).Trim() > mobsf_scan.ps1'
        bat '@echo $r = (& curl -s -F "file=@app\build\outputs\apk\debug\app-debug.apk" http://localhost:8010/api/v1/upload -H ("X-Mobsf-Api-Key: "+$key)) >> mobsf_scan.ps1'
        bat '@echo Set-Content mobsf_upload.json $r >> mobsf_scan.ps1'
        bat '@echo $h = ($r | ConvertFrom-Json).hash >> mobsf_scan.ps1'
        bat '@echo Write-Host ("Hash: "+$h) >> mobsf_scan.ps1'
        bat '@echo & curl -s -X POST http://localhost:8010/api/v1/scan -H ("X-Mobsf-Api-Key: "+$key) -d ("scan_type=apk&file_name=app-debug.apk&hash="+$h) >> mobsf_scan.ps1'
        bat '@echo Start-Sleep -Seconds 90 >> mobsf_scan.ps1'
        bat '@echo & curl -s -X POST http://localhost:8010/api/v1/download_pdf -H ("X-Mobsf-Api-Key: "+$key) -d ("hash="+$h) -o mobsf-security-report.pdf >> mobsf_scan.ps1'
        bat '@echo Write-Host ("PDF bytes: "+((Get-Item mobsf-security-report.pdf).Length)) >> mobsf_scan.ps1'
        bat 'powershell -ExecutionPolicy Bypass -File mobsf_scan.ps1'
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