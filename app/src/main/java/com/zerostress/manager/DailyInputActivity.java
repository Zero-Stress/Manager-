package com.zerostress.manager;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DailyInputActivity extends AppCompatActivity {

    private Spinner spinnerPlayerName, spinnerMatchType;
    private EditText etKills, etAssists, etDamage, etMinutes, etSeconds;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private FirebaseFirestore db;

    private List<String> playerNames = new ArrayList<>();
    private List<String> playerIds = new ArrayList<>();

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
        etMinutes = findViewById(R.id.etDailyMinutes);
        etSeconds = findViewById(R.id.etDailySeconds);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvDailyStatus);
        MaterialButton btnSubmit = findViewById(R.id.btnDailySubmit);

        // Load registered players
        loadRegisteredPlayers();

        // Match types
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

                // Add "Select Player" as first item
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

        if (killsStr.isEmpty()) { etKills.setError("Required"); return; }
        if (damageStr.isEmpty()) { etDamage.setError("Required"); return; }

        int kills = Integer.parseInt(killsStr);
        int assists = assistsStr.isEmpty() ? 0 : Integer.parseInt(assistsStr);
        long damage = Long.parseLong(damageStr);
        int minutes = etMinutes.getText().toString().isEmpty() ? 0 : Integer.parseInt(etMinutes.getText().toString());
        int seconds = etSeconds.getText().toString().isEmpty() ? 0 : Integer.parseInt(etSeconds.getText().toString());
        int survivalSeconds = (minutes * 60) + seconds;
        String matchType = spinnerMatchType.getSelectedItem().toString();

        progressBar.setVisibility(View.VISIBLE);

        Map<String, Object> dailyLog = new HashMap<>();
        dailyLog.put("playerName", playerName);
        dailyLog.put("playerId", playerId);
        dailyLog.put("kills", kills);
        dailyLog.put("assists", assists);
        dailyLog.put("damage", damage);
        dailyLog.put("survivalSeconds", survivalSeconds);
        dailyLog.put("matchType", matchType);
        dailyLog.put("timestamp", System.currentTimeMillis());

        db.collection("daily_logs").add(dailyLog)
            .addOnSuccessListener(v -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Daily entry saved for " + playerName + "!", Toast.LENGTH_SHORT).show();
                // Reset spinner to default
                spinnerPlayerName.setSelection(0);
                etKills.setText("");
                etAssists.setText("");
                etDamage.setText("");
                etMinutes.setText("");
                etSeconds.setText("");
                loadRecentEntries();
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void loadRecentEntries() {
        db.collection("daily_logs").orderBy("timestamp").limit(20)
            .get()
            .addOnSuccessListener(query -> {
                StringBuilder sb = new StringBuilder();
                for (DocumentSnapshot doc : query.getDocuments()) {
                    String name = doc.getString("playerName");
                    Long kills = doc.getLong("kills");
                    Long damage = doc.getLong("damage");
                    sb.append(name).append(": ").append(kills != null ? kills : 0).append("K, ");
                    sb.append(damage != null ? damage : 0).append(" DMG\n");
                }
                tvStatus.setText(sb.length() > 0 ? sb.toString() : "No entries today");
            });
    }
}
