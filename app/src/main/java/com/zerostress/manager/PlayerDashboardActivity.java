package com.zerostress.manager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class PlayerDashboardActivity extends AppCompatActivity {

    private TextView tvName, tvScore, tvLevel, tvRank, tvCoins, tvXp;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String uid;
    private ListenerRegistration notificationListener;

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
        findViewById(R.id.btnSubmitMatch).setOnClickListener(v -> startActivity(new Intent(this, SubmitMatchActivity.class)));
        findViewById(R.id.btnLeaderboard).setOnClickListener(v -> startActivity(new Intent(this, LeaderboardActivity.class)));
        findViewById(R.id.btnChat).setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));
        findViewById(R.id.btnVoice).setOnClickListener(v -> startActivity(new Intent(this, VoiceActivity.class)));
        findViewById(R.id.btnProfile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        findViewById(R.id.btnFriends).setOnClickListener(v -> startActivity(new Intent(this, FriendsActivity.class)));
        findViewById(R.id.btnSeasons).setOnClickListener(v -> startActivity(new Intent(this, SeasonActivity.class)));
        findViewById(R.id.btnSchedule).setOnClickListener(v -> startActivity(new Intent(this, ScheduleActivity.class)));
        
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

        loadProfile();
        listenForNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }

    private void listenForNotifications() {
        long lastCheck = System.currentTimeMillis() - (60 * 1000); // Last 1 minute
        notificationListener = db.collection("notifications")
            .whereGreaterThan("timestamp", lastCheck)
            .addSnapshotListener((snapshots, e) -> {
                if (e != null || snapshots == null) return;
                for (var doc : snapshots.getDocuments()) {
                    String title = doc.getString("title");
                    String message = doc.getString("message");
                    if (title != null && message != null) {
                        Toast.makeText(this, "📢 " + title + "\n" + message, Toast.LENGTH_LONG).show();
                    }
                }
            });
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
