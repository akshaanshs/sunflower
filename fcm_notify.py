import sys
import google.auth.transport.requests
import google.oauth2.service_account
import urllib.request
import json

SCOPES = ['https://www.googleapis.com/auth/firebase.messaging']
SERVICE_ACCOUNT_FILE = sys.argv[1]
PROJECT_ID = 'sunflower-cicd'
FCM_TOKEN = 'f9zolC_QQJ-kVSW4rz30g7:APA91bGs6YeJK2Ivpe5skRlkk6HWD4PyTT5yNfqjEa6APRAparHg5aFoyUIDoPT88KvCqbEvr8XbfkmuM4-xtiSnYUbMbUNGiq1mo7JyaT3G3eo7IROpJBE'
BUILD_NUMBER = sys.argv[2]

credentials = google.oauth2.service_account.Credentials.from_service_account_file(SERVICE_ACCOUNT_FILE, scopes=SCOPES)
credentials.refresh(google.auth.transport.requests.Request())
access_token = credentials.token

url = 'https://fcm.googleapis.com/v1/projects/' + PROJECT_ID + '/messages:send'
message = {
    'message': {
        'token': FCM_TOKEN,
        'notification': {
            'title': 'Build ' + BUILD_NUMBER + ' Successful!',
            'body': 'New Sunflower APK is ready for testing!'
        },
        'data': {
            'build_number': BUILD_NUMBER
        }
    }
}

data = json.dumps(message).encode('utf-8')
req = urllib.request.Request(url, data=data, headers={
    'Authorization': 'Bearer ' + access_token,
    'Content-Type': 'application/json'
})
response = urllib.request.urlopen(req)
print('FCM Response:', response.read().decode('utf-8'))
print('Notification sent to device successfully!')