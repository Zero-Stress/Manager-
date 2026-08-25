# Zero Stress Manager - Android App

## Overview
This is the Android version of the ZERO STRESS Performance & Leaderboard Manager app, built for AndroidIDE.

## Features
- Full native Android WebView wrapper
- Firebase/Firestore real-time sync
- Camera integration for screenshot/OCR
- Dark theme optimized for mobile
- Portrait-locked for consistent UX

## Building in AndroidIDE

### Prerequisites
1. Install [AndroidIDE](https://androidide.com/) on your Android device
2. Ensure you have JDK 17+ installed in AndroidIDE

### Build Steps
1. Open this project folder in AndroidIDE
2. Wait for Gradle sync to complete
3. Tap the **Build** button (hammer icon) or use:
   ```
   ./gradlew assembleDebug
   ```
4. The APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

### Install & Run
1. Transfer the APK to your device
2. Open and install the APK
3. Launch "Zero Stress Manager" from your app drawer

## Project Structure
```
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml        # App permissions & config
│   │   ├── java/com/zerostress/manager/
│   │   │   └── MainActivity.java      # WebView activity
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml
│   │   │   ├── values/strings.xml
│   │   │   ├── values/themes.xml
│   │   │   └── xml/file_paths.xml     # FileProvider config
│   │   └── assets/
│   │       └── index.html             # Web app
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## Permissions
- **Internet** - For Firebase connectivity
- **Camera** - For screenshot/OCR match data feature
- **Storage** - For saving exported scorecards

## Notes
- The app uses a WebView to render the existing web app
- Firebase SDK loads from CDN (requires internet)
- Camera permission is requested when using OCR feature
- Back button navigates within the WebView history

## Troubleshooting

### Firebase not connecting?
- Ensure you have internet connection
- Check that Firebase project is configured correctly

### Camera not working?
- Grant camera permission when prompted
- The app will fallback to file picker if camera is denied

### Build fails?
- Ensure JDK 17+ is configured in AndroidIDE
- Try: `./gradlew clean` then rebuild
