package com.zerostress.manager;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvName, tvPhone, tvScore, tvLevel, tvRank, tvKills, tvDeaths, tvAssists, tvDamage, tvWins, tvMatches, tvWinRate, tvCoins, tvXp;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();

        tvName = findViewById(R.id.tvProfileName);
        tvPhone = findViewById(R.id.tvProfilePhone);
        tvScore = findViewById(R.id.tvProfileScore);
        tvLevel = findViewById(R.id.tvProfileLevel);
        tvRank = findViewById(R.id.tvProfileRank);
        tvKills = findViewById(R.id.tvProfileKills);
        tvDeaths = findViewById(R.id.tvProfileDeaths);
        tvAssists = findViewById(R.id.tvProfileAssists);
        tvDamage = findViewById(R.id.tvProfileDamage);
        tvWins = findViewById(R.id.tvProfileWins);
        tvMatches = findViewById(R.id.tvProfileMatches);
        tvWinRate = findViewById(R.id.tvProfileWinRate);
        tvCoins = findViewById(R.id.tvProfileCoins);
        tvXp = findViewById(R.id.tvProfileXp);

        loadProfile();
    }

    private void loadProfile() {
        if (uid == null) return;
        db.collection("players").document(uid).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    tvName.setText(doc.getString("name"));
                    tvPhone.setText("+880 " + doc.getString("phone"));
                    long score = doc.getLong("score") != null ? doc.getLong("score") : 0;
                    long kills = doc.getLong("kills") != null ? doc.getLong("kills") : 0;
                    long deaths = doc.getLong("deaths") != null ? doc.getLong("deaths") : 0;
                    long assists = doc.getLong("assists") != null ? doc.getLong("assists") : 0;
                    long damage = doc.getLong("damage") != null ? doc.getLong("damage") : 0;
                    long wins = doc.getLong("wins") != null ? doc.getLong("wins") : 0;
                    long matches = doc.getLong("matches") != null ? doc.getLong("matches") : 0;
                    int xp = doc.getLong("xp") != null ? doc.getLong("xp").intValue() : 0;
                    int level = doc.getLong("level") != null ? doc.getLong("level").intValue() : 1;
                    int coins = doc.getLong("coins") != null ? doc.getLong("coins").intValue() : 0;
                    String rank = doc.getString("rank") != null ? doc.getString("rank") : "Iron";

                    tvScore.setText(score + " pts");
                    tvLevel.setText("Level " + level);
                    tvRank.setText("Rank: " + rank);
                    tvKills.setText("Kills: " + kills);
                    tvDeaths.setText("Deaths: " + deaths);
                    tvAssists.setText("Assists: " + assists);
                    tvDamage.setText("Damage: " + damage);
                    tvWins.setText("Wins: " + wins);
                    tvMatches.setText("Matches: " + matches);
                    double winRate = matches > 0 ? (wins * 100.0 / matches) : 0;
                    tvWinRate.setText(String.format("Win Rate: %.1f%%", winRate));
                    tvCoins.setText("Coins: " + coins);
                    tvXp.setText("XP: " + xp);
                }
            });
    }
}
