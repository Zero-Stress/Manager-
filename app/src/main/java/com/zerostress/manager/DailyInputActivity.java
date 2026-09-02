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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DailyInputActivity extends AppCompatActivity {

    private EditText etPlayerName, etKills, etAssists, etDamage, etMinutes, etSeconds;
    private Spinner spinnerMatchType;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_input);

        db = FirebaseFirestore.getInstance();

        etPlayerName = findViewById(R.id.etDailyPlayerName);
        etKills = findViewById(R.id.etDailyKills);
        etAssists = findViewById(R.id.etDailyAssists);
        etDamage = findViewById(R.id.etDailyDamage);
        etMinutes = findViewById(R.id.etDailyMinutes);
        etSeconds = findViewById(R.id.etDailySeconds);
        spinnerMatchType = findViewById(R.id.spinnerDailyMatchType);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvDailyStatus);
        MaterialButton btnSubmit = findViewById(R.id.btnDailySubmit);

        String[] types = {"Classic", "Ranked", "Tournament", "Custom"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMatchType.setAdapter(adapter);

        btnSubmit.setOnClickListener(v -> submitDailyInput());
        loadRecentEntries();
    }

    private void submitDailyInput() {
        String playerName = etPlayerName.getText().toString().trim();
        String killsStr = etKills.getText().toString().trim();
        String assistsStr = etAssists.getText().toString().trim();
        String damageStr = etDamage.getText().toString().trim();

        if (playerName.isEmpty()) { etPlayerName.setError("Required"); return; }
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
        dailyLog.put("kills", kills);
        dailyLog.put("assists", assists);
        dailyLog.put("damage", damage);
        dailyLog.put("survivalSeconds", survivalSeconds);
        dailyLog.put("matchType", matchType);
        dailyLog.put("timestamp", System.currentTimeMillis());

        db.collection("daily_logs").add(dailyLog)
            .addOnSuccessListener(v -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Daily entry saved!", Toast.LENGTH_SHORT).show();
                etPlayerName.setText("");
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
