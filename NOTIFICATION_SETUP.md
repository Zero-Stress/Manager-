# Zero Stress Manager - Notification System Setup

## Overview
This app now includes a complete push notification system using Firebase Cloud Messaging (FCM). Players will receive notifications when:
- Daily leaderboard is updated
- Weekly leaderboard summary is ready
- New announcements are posted

## Prerequisites

### 1. Enable Firebase Cloud Messaging

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: `zerostress-manager`
3. Navigate to **Project Settings** → **Cloud Messaging**
4. Ensure **Firebase Cloud Messaging API (V1)** is enabled

### 2. Download google-services.json

1. In Firebase Console, go to **Project Settings** → **General**
2. Under **Your apps**, click **Add app** → **Android**
3. Enter package name: `com.zerostress.manager`
4. Download `google-services.json`
5. Place it in: `app/google-services.json`

### 3. Get Firebase Service Account Key (for Cloud Functions)

1. In Firebase Console, go to **Project Settings** → **Service accounts**
2. Click **Generate new private key**
3. Save the file as: `functions/serviceAccountKey.json`

## Android Setup

### Step 1: Add google-services plugin

Update `app/build.gradle` at the top:

```gradle
plugins {
    id 'com.android.application'
    id 'com.google.gms.google-services'  // Add this line
}
```

Update `build.gradle` (project level) dependencies:

```gradle
dependencies {
    classpath 'com.android.tools.build:gradle:8.1.4'
    classpath 'com.google.gms:google-services:4.4.0'  // Add this line
}
```

### Step 2: Build and Test

1. Sync Gradle
2. Build the APK
3. Install on device
4. Grant notification permission when prompted

## Cloud Functions Setup

### Step 1: Install dependencies

```bash
cd functions
npm install
```

### Step 2: Deploy Cloud Functions

```bash
# From project root
firebase deploy --only functions
```

### Step 3: Test notifications

1. Open the app and log in
2. As admin, add a new daily record
3. All players should receive a notification

## How It Works

### Android Side

1. **FCM Token**: When app starts, it retrieves an FCM token from Firebase
2. **Token Sync**: On login, the token is saved to Firestore under the user's document
3. **Receiving**: `MyFirebaseMessagingService` handles incoming messages
4. **Display**: Notifications are shown with app icon and leaderboard colors

### Cloud Functions

1. **onDailyLogWrite**: Triggered when dailylogs collection changes
2. **onAnnouncementWrite**: Triggered when new announcement is created
3. **weeklyLeaderboardSummary**: Scheduled every Sunday at 9 PM (Dhaka time)

### Notification Types

| Type | Color | Trigger |
|------|-------|---------|
| Daily Update | 🔵 Blue | Daily log created/updated |
| Weekly Summary | 🟢 Green | Sunday 9 PM scheduled |
| Announcement | 🟡 Gold | New announcement posted |

## JavaScript Interface

The web app can interact with notifications via `AndroidNotification`:

```javascript
// Show a local notification
AndroidNotification.showNotification("Title", "Body", "daily_leaderboard");

// Sync FCM token with user account
AndroidNotification.syncFCMToken(userPhone);

// Start listening for real-time changes
AndroidNotification.startLeaderboardListener();

// Check if notifications are enabled
var enabled = AndroidNotification.areNotificationsEnabled();
```

## Troubleshooting

### No notifications received?

1. **Check permissions**: Settings → Apps → Zero Stress Manager → Notifications
2. **Check FCM token**: Look for "FCM Token" in logcat
3. **Check Firestore**: Ensure `users/{phone}/fcmToken` is populated
4. **Test Cloud Functions**: Check Firebase Console → Functions → Logs

### Notifications delayed?

- FCM has built-in delay (up to a few minutes)
- For immediate updates, use the JavaScript interface listener

### Cloud Functions not working?

1. Check logs: `firebase functions:log`
2. Ensure service account key is in `functions/`
3. Verify billing is enabled on Firebase project

## File Structure

```
├── app/src/main/java/com/zerostress/manager/
│   ├── MainActivity.java              # Notification permission & JS interface
│   ├── MyFirebaseMessagingService.java # FCM message handler
│   └── NotificationHelper.java        # Notification management
├── functions/
│   ├── src/index.ts                   # Cloud Functions
│   ├── package.json
│   └── serviceAccountKey.json        # (you provide this)
├── firebase.json                      # Firebase config
└── NOTIFICATION_SETUP.md             # This file
```

## Privacy Notes

- FCM tokens are stored in Firestore under each user's document
- Tokens are automatically removed when invalid
- No personal data is included in notification payloads
- Users can revoke notification permission in device settings
