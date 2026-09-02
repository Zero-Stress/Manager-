package com.zerostress.manager;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SubmitMatchActivity extends AppCompatActivity {

    private EditText etKills, etDeaths, etAssists, etDamage;
    private RadioGroup rgResult;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_match);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        etKills = findViewById(R.id.etKills);
        etDeaths = findViewById(R.id.etDeaths);
        etAssists = findViewById(R.id.etAssists);
        etDamage = findViewById(R.id.etDamage);
        rgResult = findViewById(R.id.rgResult);
        progressBar = findViewById(R.id.progressBar);
        MaterialButton btnSubmit = findViewById(R.id.btnSubmit);

        Spinner spinner = findViewById(R.id.spinnerMatchType);
        String[] types = {"Classic", "Ranked", "Tournament", "Custom"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        btnSubmit.setOnClickListener(v -> submitMatch());
    }

    private void submitMatch() {
        String killsStr = etKills.getText().toString().trim();
        String deathsStr = etDeaths.getText().toString().trim();
        String assistsStr = etAssists.getText().toString().trim();
        String damageStr = etDamage.getText().toString().trim();

        if (killsStr.isEmpty()) { etKills.setError("Required"); return; }
        if (damageStr.isEmpty()) { etDamage.setError("Required"); return; }

        int kills = Integer.parseInt(killsStr);
        int deaths = deathsStr.isEmpty() ? 0 : Integer.parseInt(deathsStr);
        int assists = assistsStr.isEmpty() ? 0 : Integer.parseInt(assistsStr);
        long damage = Long.parseLong(damageStr);
        boolean isWin = rgResult.getCheckedRadioButtonId() == R.id.rbWin;
        String matchType = ((Spinner) findViewById(R.id.spinnerMatchType)).getSelectedItem().toString();

        progressBar.setVisibility(View.VISIBLE);

        Map<String, Object> matchLog = new HashMap<>();
        matchLog.put("playerId", userId);
        matchLog.put("kills", kills);
        matchLog.put("deaths", deaths);
        matchLog.put("assists", assists);
        matchLog.put("damage", damage);
        matchLog.put("win", isWin);
        matchLog.put("matchType", matchType);
        matchLog.put("date", System.currentTimeMillis());
        matchLog.put("score", (long)(kills * 10 + damage / 100 + (isWin ? 200 : 0)));

        db.collection("match_logs").add(matchLog)
            .addOnSuccessListener(v -> updatePlayerStats(kills, deaths, assists, damage, isWin))
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void updatePlayerStats(int kills, int deaths, int assists, long damage, boolean isWin) {
        db.collection("players").document(userId).get()
            .addOnSuccessListener(doc -> {
                long currentKills = doc.getLong("kills") != null ? doc.getLong("kills") : 0;
                long currentDamage = doc.getLong("damage") != null ? doc.getLong("damage") : 0;
                long currentWins = doc.getLong("wins") != null ? doc.getLong("wins") : 0;
                long currentMatches = doc.getLong("matches") != null ? doc.getLong("matches") : 0;
                long currentScore = doc.getLong("score") != null ? doc.getLong("score") : 0;
                int currentXp = doc.getLong("xp") != null ? doc.getInt("xp") : 0;
                int currentLevel = doc.getLong("level") != null ? doc.getInt("level") : 1;
                int currentCoins = doc.getLong("coins") != null ? doc.getInt("coins") : 0;

                long newKills = currentKills + kills;
                long newDamage = currentDamage + damage;
                long newWins = currentWins + (isWin ? 1 : 0);
                long newMatches = currentMatches + 1;
                long newScore = (long)(newKills * 10 + newDamage / 100 + newWins * 50);

                int xpGained = kills * 5 + (int)(damage / 50) + (isWin ? 100 : 20);
                int newXp = currentXp + xpGained;
                int newLevel = currentLevel;
                while (newXp >= newLevel * 500) {
                    newXp -= newLevel * 500;
                    newLevel++;
                }

                int coinsGained = kills * 2 + (isWin ? 25 : 5);
                int newCoins = currentCoins + coinsGained;

                String newRank;
                if (newScore >= 5000) newRank = "Mythic";
                else if (newScore >= 4000) newRank = "Diamond";
                else if (newScore >= 3000) newRank = "Platinum";
                else if (newScore >= 2000) newRank = "Gold";
                else if (newScore >= 1200) newRank = "Silver";
                else if (newScore >= 600) newRank = "Bronze";
                else newRank = "Iron";

                Map<String, Object> updates = new HashMap<>();
                updates.put("kills", newKills);
                updates.put("damage", newDamage);
                updates.put("wins", newWins);
                updates.put("matches", newMatches);
                updates.put("score", newScore);
                updates.put("xp", newXp);
                updates.put("level", newLevel);
                updates.put("coins", newCoins);
                updates.put("rank", newRank);

                db.collection("players").document(userId).update(updates)
                    .addOnSuccessListener(v -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "✅ +" + xpGained + " XP, +" + coinsGained + " coins", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            });
    }
}
