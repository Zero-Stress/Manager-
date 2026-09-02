package com.zerostress.manager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class PlayerDashboardActivity extends AppCompatActivity {

    private TextView tvName, tvScore, tvLevel, tvKills, tvWins, tvMatches, tvWinRate, tvCoins, tvRank;
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
        tvScore = findViewById(R.id.tvPlayerScore);
        tvLevel = findViewById(R.id.tvPlayerLevel);
        tvKills = findViewById(R.id.tvKills);
        tvWins = findViewById(R.id.tvWins);
        tvMatches = findViewById(R.id.tvMatches);
        tvWinRate = findViewById(R.id.tvWinRate);
        tvCoins = findViewById(R.id.tvCoins);
        tvRank = findViewById(R.id.tvRank);

        MaterialButton btnSubmitMatch = findViewById(R.id.btnSubmitMatch);
        MaterialButton btnLeaderboard = findViewById(R.id.btnLeaderboard);
        MaterialButton btnChat = findViewById(R.id.btnChat);
        MaterialButton btnVoice = findViewById(R.id.btnVoice);
        MaterialButton btnProfile = findViewById(R.id.btnProfile);
        MaterialButton btnSchedule = findViewById(R.id.btnSchedule);
        MaterialButton btnFriends = findViewById(R.id.btnFriends);
        MaterialButton btnAchievements = findViewById(R.id.btnAchievements);
        MaterialButton btnSeason = findViewById(R.id.btnSeason);
        MaterialButton btnAnnouncements = findViewById(R.id.btnAnnouncementsPlayer);
        MaterialButton btnLogout = findViewById(R.id.btnLogout);

        btnSubmitMatch.setOnClickListener(v -> startActivity(new Intent(this, SubmitMatchActivity.class)));
        btnLeaderboard.setOnClickListener(v -> startActivity(new Intent(this, LeaderboardActivity.class)));
        btnChat.setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));
        btnVoice.setOnClickListener(v -> startActivity(new Intent(this, VoiceActivity.class)));
        btnProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        btnSchedule.setOnClickListener(v -> startActivity(new Intent(this, ScheduleActivity.class)));
        btnFriends.setOnClickListener(v -> startActivity(new Intent(this, FriendsActivity.class)));
        btnAchievements.setOnClickListener(v -> startActivity(new Intent(this, AchievementsActivity.class)));
        btnSeason.setOnClickListener(v -> startActivity(new Intent(this, SeasonActivity.class)));
        btnAnnouncements.setOnClickListener(v -> startActivity(new Intent(this, AnnouncementsActivity.class)));
        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        loadProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
    }

    private void loadProfile() {
        if (uid == null) return;
        db.collection("players").document(uid).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    tvName.setText(doc.getString("name"));
                    long score = doc.getLong("score") != null ? doc.getLong("score") : 0;
                    long kills = doc.getLong("kills") != null ? doc.getLong("kills") : 0;
                    long wins = doc.getLong("wins") != null ? doc.getLong("wins") : 0;
                    long matches = doc.getLong("matches") != null ? doc.getLong("matches") : 0;
                    long xp = doc.getLong("xp") != null ? doc.getLong("xp") : 0;
                    long level = doc.getLong("level") != null ? doc.getLong("level") : 1;
                    long coins = doc.getLong("coins") != null ? doc.getLong("coins") : 0;
                    String rank = doc.getString("rank") != null ? doc.getString("rank") : "Iron";

                    tvScore.setText("Score: " + score);
                    tvLevel.setText("Level " + level + " (" + xp + " XP)");
                    tvKills.setText("Kills: " + kills);
                    tvWins.setText("Wins: " + wins);
                    tvMatches.setText("Matches: " + matches);
                    double winRate = matches > 0 ? (wins * 100.0 / matches) : 0;
                    tvWinRate.setText(String.format("Win Rate: %.1f%%", winRate));
                    tvCoins.setText("Coins: " + coins);
                    tvRank.setText("Rank: " + rank);
                }
            });
    }
}
