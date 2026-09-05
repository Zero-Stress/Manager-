package com.zerostress.manager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class PlayerDashboardActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_CODE = 1001;

    private TextView tvName, tvScore, tvLevel, tvRank, tvCoins, tvXp;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_dashboard);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        uid = auth.getUid();

        tvName = findViewById(R.id.tvPlayerName);
        tvScore = findViewById(R.id.tvScore);
        tvLevel = findViewById(R.id.tvLevel);
        tvRank = findViewById(R.id.tvRank);
        tvCoins = findViewById(R.id.tvCoins);
        tvXp = findViewById(R.id.tvXp);

        // Navigation buttons
        findViewById(R.id.btnScheduleTop).setOnClickListener(v -> startActivity(new Intent(this, ScheduleActivity.class)));
        findViewById(R.id.btnLeaderboard).setOnClickListener(v -> startActivity(new Intent(this, LeaderboardActivity.class)));
        findViewById(R.id.btnChat).setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));
        findViewById(R.id.btnVoice).setOnClickListener(v -> startActivity(new Intent(this, VoiceActivity.class)));
        findViewById(R.id.btnProfile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        findViewById(R.id.btnFriends).setOnClickListener(v -> startActivity(new Intent(this, FriendsActivity.class)));
        findViewById(R.id.btnSeasons).setOnClickListener(v -> startActivity(new Intent(this, SeasonActivity.class)));
        findViewById(R.id.btnAchievements).setOnClickListener(v -> startActivity(new Intent(this, AchievementsActivity.class)));
        findViewById(R.id.btnNews).setOnClickListener(v -> startActivity(new Intent(this, AnnouncementsActivity.class)));

        // New features
        findViewById(R.id.btnDailyRewards).setOnClickListener(v -> startActivity(new Intent(this, DailyLoginRewardsActivity.class)));
        findViewById(R.id.btnChallenges).setOnClickListener(v -> startActivity(new Intent(this, DailyChallengesActivity.class)));
        findViewById(R.id.btnBattlePass).setOnClickListener(v -> startActivity(new Intent(this, BattlePassActivity.class)));
        findViewById(R.id.btnTitles).setOnClickListener(v -> startActivity(new Intent(this, PlayerTitlesActivity.class)));
        findViewById(R.id.btnPerformance).setOnClickListener(v -> startActivity(new Intent(this, PerformanceGraphsActivity.class)));

        findViewById(R.id.btnSettings).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        findViewById(R.id.btnNotifications).setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));

        // Request notification permission on Android 13+
        requestNotificationPermission();

        // Save FCM token (retry on resume to ensure it's always fresh)
        com.zerostress.manager.fcm.ZSFCMService.saveTokenToFirestoreWithRetry(this);

        loadProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, re-save FCM token
                com.zerostress.manager.fcm.ZSFCMService.saveTokenToFirestoreWithRetry(this);
            }
        }
    }

    private void loadProfile() {
        if (uid == null) return;
        db.collection("players").document(uid).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    tvName.setText(doc.getString("name"));
                    long score = doc.getLong("score") != null ? doc.getLong("score") : 0;
                    long xp = doc.getLong("xp") != null ? doc.getLong("xp") : 0;
                    long level = doc.getLong("level") != null ? doc.getLong("level") : 1;
                    long coins = doc.getLong("coins") != null ? doc.getLong("coins") : 0;
                    String rank = doc.getString("rank") != null ? doc.getString("rank") : "Iron";

                    tvScore.setText(String.valueOf(score));
                    tvLevel.setText(String.valueOf(level));
                    tvRank.setText(rank);
                    tvCoins.setText(String.valueOf(coins));
                    // Show XP progress: current XP / XP needed for next level
                    long xpForNextLevel = level * 500;
                    tvXp.setText(xp + "/" + xpForNextLevel);
                }
            });
    }
}
