package com.zerostress.manager;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DailyInputActivity extends AppCompatActivity {

    private Spinner spinnerPlayerName, spinnerMatchType;
    private EditText etKills, etAssists, etDamage, etWins, etMinutes, etSeconds;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private FirebaseFirestore db;

    private List<String> playerNames = new ArrayList<>();
    private List<String> playerIds = new ArrayList<>();

    // Score formula constants
    private static final int KILL_POINTS = 10;
    private static final int WIN_POINTS = 25;
    private static final int ASSIST_POINTS = 5;
    private static final int DAMAGE_PER_POINT = 100;
    private static final int SURVIVAL_POINTS_PER_SEC = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_input);

        db = FirebaseFirestore.getInstance();

        spinnerPlayerName = findViewById(R.id.spinnerDailyPlayerName);
        spinnerMatchType = findViewById(R.id.spinnerDailyMatchType);
        etKills = findViewById(R.id.etDailyKills);
        etAssists = findViewById(R.id.etDailyAssists);
        etDamage = findViewById(R.id.etDailyDamage);
        etWins = findViewById(R.id.etDailyWins);
        etMinutes = findViewById(R.id.etDailyMinutes);
        etSeconds = findViewById(R.id.etDailySeconds);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvDailyStatus);
        MaterialButton btnSubmit = findViewById(R.id.btnDailySubmit);

        loadRegisteredPlayers();

        String[] types = {"Classic", "Ranked", "Tournament", "Custom"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMatchType.setAdapter(typeAdapter);

        btnSubmit.setOnClickListener(v -> submitDailyInput());
        loadRecentEntries();
    }

    private void loadRegisteredPlayers() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("players")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener(query -> {
                progressBar.setVisibility(View.GONE);
                playerNames.clear();
                playerIds.clear();

                for (DocumentSnapshot doc : query.getDocuments()) {
                    String name = doc.getString("name");
                    if (name != null && !name.isEmpty()) {
                        playerNames.add(name);
                        playerIds.add(doc.getId());
                    }
                }

                if (playerNames.isEmpty()) {
                    playerNames.add("No players registered");
                }

                playerNames.add(0, "-- Select Player --");
                playerIds.add(0, "");

                ArrayAdapter<String> playerAdapter = new ArrayAdapter<>(
                    DailyInputActivity.this,
                    android.R.layout.simple_spinner_item,
                    playerNames
                );
                playerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerPlayerName.setAdapter(playerAdapter);
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Error loading players: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private int calculateScore(int kills, int wins, int assists, long damage, int survivalSeconds) {
        int score = 0;
        score += kills * KILL_POINTS;
        score += wins * WIN_POINTS;
        score += assists * ASSIST_POINTS;
        score += (int) (damage / DAMAGE_PER_POINT);
        score += survivalSeconds * SURVIVAL_POINTS_PER_SEC;
        return score;
    }

    private void submitDailyInput() {
        int selectedPosition = spinnerPlayerName.getSelectedItemPosition();

        if (selectedPosition <= 0) {
            Toast.makeText(this, "Please select a player", Toast.LENGTH_SHORT).show();
            return;
        }

        String playerName = playerNames.get(selectedPosition);
        String playerId = playerIds.get(selectedPosition);
        String killsStr = etKills.getText().toString().trim();
        String assistsStr = etAssists.getText().toString().trim();
        String damageStr = etDamage.getText().toString().trim();
        String winsStr = etWins.getText().toString().trim();

        if (killsStr.isEmpty()) { etKills.setError("Required"); return; }
        if (damageStr.isEmpty()) { etDamage.setError("Required"); return; }

        int kills = Integer.parseInt(killsStr);
        int wins = winsStr.isEmpty() ? 0 : Integer.parseInt(winsStr);
        int assists = assistsStr.isEmpty() ? 0 : Integer.parseInt(assistsStr);
        long damage = Long.parseLong(damageStr);
        int minutes = etMinutes.getText().toString().isEmpty() ? 0 : Integer.parseInt(etMinutes.getText().toString());
        int seconds = etSeconds.getText().toString().isEmpty() ? 0 : Integer.parseInt(etSeconds.getText().toString());
        int survivalSeconds = (minutes * 60) + seconds;
        String matchType = spinnerMatchType.getSelectedItem().toString();

        // Calculate the score for this entry
        int entryScore = calculateScore(kills, wins, assists, damage, survivalSeconds);

        progressBar.setVisibility(View.VISIBLE);

        // 1. Save to daily_logs
        Map<String, Object> dailyLog = new HashMap<>();
        dailyLog.put("playerName", playerName);
        dailyLog.put("playerId", playerId);
        dailyLog.put("kills", kills);
        dailyLog.put("wins", wins);
        dailyLog.put("assists", assists);
        dailyLog.put("damage", damage);
        dailyLog.put("survivalSeconds", survivalSeconds);
        dailyLog.put("score", entryScore);
        dailyLog.put("matchType", matchType);
        dailyLog.put("timestamp", System.currentTimeMillis());

        db.collection("daily_logs").add(dailyLog)
            .addOnSuccessListener(v -> {
                // 2. Update player stats in players collection
                updatePlayerStats(playerId, kills, wins, assists, damage, survivalSeconds, entryScore, playerName);
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void updatePlayerStats(String playerId, int kills, int wins, int assists,
                                    long damage, int survivalSeconds, int score, String playerName) {
        // Use FieldValue.increment to add to existing stats
        Map<String, Object> updates = new HashMap<>();

        // Total stats
        updates.put("kills", FieldValue.increment(kills));
        updates.put("wins", FieldValue.increment(wins));
        updates.put("assists", FieldValue.increment(assists));
        updates.put("damage", FieldValue.increment(damage));
        updates.put("survivalSeconds", FieldValue.increment(survivalSeconds));
        updates.put("matches", FieldValue.increment(1));
        updates.put("score", FieldValue.increment(score));

        // Daily stats
        updates.put("dailyKills", FieldValue.increment(kills));
        updates.put("dailyWins", FieldValue.increment(wins));
        updates.put("dailyAssists", FieldValue.increment(assists));
        updates.put("dailyDamage", FieldValue.increment(damage));
        updates.put("dailyMatches", FieldValue.increment(1));
        updates.put("dailyScore", FieldValue.increment(score));

        // Weekly stats
        updates.put("weeklyKills", FieldValue.increment(kills));
        updates.put("weeklyWins", FieldValue.increment(wins));
        updates.put("weeklyAssists", FieldValue.increment(assists));
        updates.put("weeklyDamage", FieldValue.increment(damage));
        updates.put("weeklyMatches", FieldValue.increment(1));
        updates.put("weeklyScore", FieldValue.increment(score));

        // Monthly stats
        updates.put("monthlyKills", FieldValue.increment(kills));
        updates.put("monthlyWins", FieldValue.increment(wins));
        updates.put("monthlyAssists", FieldValue.increment(assists));
        updates.put("monthlyDamage", FieldValue.increment(damage));
        updates.put("monthlyMatches", FieldValue.increment(1));
        updates.put("monthlyScore", FieldValue.increment(score));

        // Calculate rank based on total score
        db.collection("players").document(playerId).get()
            .addOnSuccessListener(doc -> {
                long currentScore = doc.getLong("score") != null ? doc.getLong("score") : 0;
                long newTotalScore = currentScore + score;
                String newRank = calculateRank(newTotalScore);
                updates.put("rank", newRank);

        // XP: 1 XP per point of score
        updates.put("xp", FieldValue.increment(score));

        // Coins reward: 5 coins per kill, 10 per win
        int coinsEarned = (kills * 5) + (wins * 10);
        updates.put("coins", FieldValue.increment(coinsEarned));

        // Level based on total XP (level up every 500 XP)
        long currentXP = doc.getLong("xp") != null ? doc.getLong("xp") : 0;
        long newTotalXP = currentXP + score;
        int newLevel = (int) (newTotalXP / 500) + 1;
        updates.put("level", newLevel);

        db.collection("players").document(playerId).update(updates)
                    .addOnSuccessListener(v -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this,
                            "✅ " + playerName + ": +" + score + " pts, +" + coinsEarned + " coins\n" +
                            "Level: " + newLevel + " | Rank: " + newRank,
                            Toast.LENGTH_LONG).show();

                        // Reset form
                        spinnerPlayerName.setSelection(0);
                        etKills.setText("");
                        etAssists.setText("");
                        etDamage.setText("");
                        etWins.setText("");
                        etMinutes.setText("");
                        etSeconds.setText("");
                        loadRecentEntries();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Error updating stats: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            });
    }

    private String calculateRank(long totalScore) {
        if (totalScore >= 50000) return "Predator";
        if (totalScore >= 30000) return "Master";
        if (totalScore >= 20000) return "Diamond";
        if (totalScore >= 10000) return "Platinum";
        if (totalScore >= 5000) return "Gold";
        if (totalScore >= 2000) return "Silver";
        return "Iron";
    }

    private void loadRecentEntries() {
        db.collection("daily_logs").orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(10)
            .get()
            .addOnSuccessListener(query -> {
                StringBuilder sb = new StringBuilder();
                for (DocumentSnapshot doc : query.getDocuments()) {
                    String name = doc.getString("playerName");
                    Long kills = doc.getLong("kills");
                    Long wins = doc.getLong("wins");
                    Long damage = doc.getLong("damage");
                    Long score = doc.getLong("score");
                    sb.append(name).append(": ");
                    sb.append(kills != null ? kills : 0).append("K ");
                    sb.append(wins != null ? wins : 0).append("W ");
                    sb.append(damage != null ? damage : 0).append("DMG ");
                    sb.append(score != null ? score : 0).append("pts\n");
                }
                tvStatus.setText(sb.length() > 0 ? sb.toString() : "No entries yet");
            });
    }
}
