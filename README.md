# Zero Stress Manager 🎮

A powerful team management and performance tracking app for competitive gaming squads.

## 📥 Download

### Option 1: Direct Download (Easiest)

**[⬇️ Download ZeroStress-2.0.0.zip](https://github.com/Zero-Stress/Manager-/releases/latest)**

1. Download the ZIP file above
2. Extract it on your phone
3. Open the extracted folder in **AndroidIDE**
4. Copy your `google-services.json` into the `app/` folder
5. Tap **Build** → **Clean** → **Make App** → **Install**

### Option 2: Git Clone (Recommended)

Open **AndroidIDE Terminal** and run:

```bash
cd /storage/emulated/0/CodeOnTheGoProjects
git clone https://github.com/Zero-Stress/Manager-.git ZeroStress-2.0.0
cd ZeroStress-2.0.0
```

Then copy your `google-services.json` into `app/` and build.

### Option 3: Download Source Code

1. Go to this page
2. Click the green **Code** button
3. Click **Download ZIP**

---

## 🔥 Features

### 🎮 Auto Game Launch
- Admin creates match schedule → selects game (PUBG, Free Fire, COD)
- Game auto-launches 5/10/15/30 minutes before match
- Works only for assigned squad members

### 💬 Real-Time Chat
- Team messaging with @mentions
- Admin can clear all chat
- Long-press to delete messages (admin only)

### 🎤 Voice Chat
- Free Jitsi Meet voice rooms (no API key needed)
- Noise cancellation built-in
- Admin controls who can join

### 🏆 Squads & Teams
- Create squads with custom names
- Assign players to squads
- Voice chat permissions per squad

### 📊 Analytics Dashboard
- Performance tracking (kills, wins, damage)
- Top players leaderboard
- Trend graphs

### ⚙️ Admin Customizer
- Change app colors, fonts, theme
- Adjust UI/button sizes with sliders
- Toggle features on/off for all users
- Live preview before saving

### 🔄 In-App Updates
- Push updates to all players
- Force update option
- Download progress bar

### 📅 Match Schedule
- Create match schedules with date/time
- Select game and squad
- Auto-launch game before match

### 🎖️ Achievements & Rewards
- 15+ unlockable badges
- Reward points system
- Daily log tracking

### 👥 Player Management
- Registration with admin approval
- Phone-based authentication
- Online status indicators

---

## 📱 How to Build

### Prerequisites
- **AndroidIDE** on your phone
- **google-services.json** from your Firebase project

### Build Steps

1. Open the project in AndroidIDE
2. Go to **Build** → **Clean**
3. Go to **Build** → **Make App**
4. Wait for **BUILD SUCCESSFUL**
5. Install the APK

### Getting google-services.json

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create a project (or use existing)
3. Add Android app (package: `com.zerostress.manager`)
4. Download `google-services.json`
5. Put it in the `app/` folder


---

## 📂 Project Structure

```
app/src/main/java/com/zerostress/manager/
├── MainActivity.java           # Main navigation
├── LoginActivity.java          # Phone + password auth
├── VoiceChatActivity.java      # Jitsi voice rooms
├── AdminCustomizerActivity.java # Theme/color/UI customizer
├── AppUpdateManager.java       # In-app update system
├── GameLaunchHelper.java       # Game detection & launch
├── GameLaunchService.java      # Background schedule monitor
├── ThemeManager.java           # Dynamic theme singleton
├── FirestoreRepository.java    # All Firestore operations
├── fragments/
│   ├── LeaderboardFragment.java
│   ├── AnalyticsFragment.java
│   ├── ChatFragment.java
│   ├── ScheduleFragment.java
│   ├── SquadFragment.java
│   ├── TournamentFragment.java
│   ├── RegistrationFragment.java
│   ├── AttendanceFragment.java
│   ├── ProfileFragment.java
│   ├── DailyInputFragment.java
│   └── AnnouncementsFragment.java
└── models/
    ├── Player.java
    ├── AppCustomizer.java
    ├── MatchSchedule.java
    ├── MatchRecord.java
    ├── ChatMessage.java
    ├── Tournament.java
    └── Squad.java
```

---

## 🛠️ Tech Stack

- **Language:** Java
- **Backend:** Firebase Firestore
- **Auth:** Phone-based (custom)
- **Voice Chat:** Jitsi Meet (WebView)
- **Notifications:** Firebase Cloud Messaging
- **UI:** Material Design + Custom Themes
- **Build:** Gradle (Android IDE compatible)

---

## 📄 License

Personal use only. Contact the developer for licensing.
