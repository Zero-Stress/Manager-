package com.zerostress.manager;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class PerformanceGraphsActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView tvStats;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_performance_graphs);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();
        progressBar = findViewById(R.id.progressBar);
        tvStats = findViewById(R.id.tvStats);

        loadPerformance();
    }

    private void loadPerformance() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("players").document(userId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    long kills = doc.getLong("kills") != null ? doc.getLong("kills") : 0;
                    long deaths = doc.getLong("deaths") != null ? doc.getLong("deaths") : 0;
                    long wins = doc.getLong("wins") != null ? doc.getLong("wins") : 0;
                    long matches = doc.getLong("matches") != null ? doc.getLong("matches") : 0;
                    long damage = doc.getLong("damage") != null ? doc.getLong("damage") : 0;
                    long xp = doc.getLong("xp") != null ? doc.getLong("xp") : 0;
                    long level = doc.getLong("level") != null ? doc.getLong("level") : 1;
                    long coins = doc.getLong("coins") != null ? doc.getLong("coins") : 0;

                    double kdRatio = deaths > 0 ? (double) kills / deaths : kills;
                    double winRate = matches > 0 ? (wins * 100.0) / matches : 0;
                    double avgDamage = matches > 0 ? damage / matches : 0;
                    double avgKills = matches > 0 ? (double) kills / matches : 0;

                    StringBuilder stats = new StringBuilder();
                    stats.append("📊 PERFORMANCE STATS\n\n");
                    stats.append("K/D Ratio: ").append(String.format("%.2f", kdRatio)).append("\n");
                    stats.append("Win Rate: ").append(String.format("%.1f%%", winRate)).append("\n");
                    stats.append("Avg Damage/Match: ").append(String.format("%.0f", avgDamage)).append("\n");
                    stats.append("Avg Kills/Match: ").append(String.format("%.1f", avgKills)).append("\n\n");
                    stats.append("Total Matches: ").append(matches).append("\n");
                    stats.append("Total Kills: ").append(kills).append("\n");
                    stats.append("Total Deaths: ").append(deaths).append("\n");
                    stats.append("Total Wins: ").append(wins).append("\n");
                    stats.append("Total Damage: ").append(damage).append("\n\n");
                    stats.append("Level: ").append(level).append("\n");
                    stats.append("XP: ").append(xp).append("\n");
                    stats.append("Coins: ").append(coins);

                    tvStats.setText(stats.toString());
                    progressBar.setVisibility(View.GONE);
                }
            });
    }
}
