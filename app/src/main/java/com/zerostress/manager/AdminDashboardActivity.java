package com.zerostress.manager;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminDashboardActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ProgressBar progressBar;
    private TextView tvAdminName, tvTotalPlayers, tvActiveMatches;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        tvAdminName = findViewById(R.id.tvAdminName);
        tvTotalPlayers = findViewById(R.id.tvTotalPlayers);
        tvActiveMatches = findViewById(R.id.tvActiveMatches);
        progressBar = findViewById(R.id.progressBar);

        // Navigation buttons
        findViewById(R.id.btnPlayerManagement).setOnClickListener(v -> loadPlayers());
        findViewById(R.id.btnDailyInput).setOnClickListener(v -> startActivity(new Intent(this, DailyInputActivity.class)));
        findViewById(R.id.btnAnnouncements).setOnClickListener(v -> showAnnouncementDialog());
        findViewById(R.id.btnLeaderboard).setOnClickListener(v -> startActivity(new Intent(this, LeaderboardActivity.class)));
        findViewById(R.id.btnChat).setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));
        findViewById(R.id.btnVoice).setOnClickListener(v -> startActivity(new Intent(this, VoiceActivity.class)));
        findViewById(R.id.btnSeasons).setOnClickListener(v -> startActivity(new Intent(this, SeasonActivity.class)));
        findViewById(R.id.btnSchedule).setOnClickListener(v -> startActivity(new Intent(this, ScheduleActivity.class)));
        findViewById(R.id.btnProfile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        
        findViewById(R.id.btnSettings).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        loadAdminInfo();
    }

    private void loadAdminInfo() {
        String userId = auth.getUid();
        db.collection("players").document(userId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    tvAdminName.setText(doc.getString("name"));
                }
            });

        db.collection("players").get()
            .addOnSuccessListener(query -> {
                tvTotalPlayers.setText(String.valueOf(query.size()));
            });

        db.collection("match_logs").get()
            .addOnSuccessListener(query -> {
                tvActiveMatches.setText(String.valueOf(query.size()));
            });
    }

    private void loadPlayers() {
        Toast.makeText(this, "Loading players...", Toast.LENGTH_SHORT).show();
    }

    private void showAnnouncementDialog() {
        EditText input = new EditText(this);
        input.setHint("Type announcement...");
        input.setMinLines(3);

        new AlertDialog.Builder(this)
            .setTitle("📢 Broadcast Announcement")
            .setView(input)
            .setPositiveButton("Send", (d, w) -> {
                String text = input.getText().toString().trim();
                if (!TextUtils.isEmpty(text)) {
                    Map<String, Object> ann = new HashMap<>();
                    ann.put("text", text);
                    ann.put("author", "Admin");
                    ann.put("timestamp", System.currentTimeMillis());
                    db.collection("announcements").add(ann)
                        .addOnSuccessListener(v -> Toast.makeText(this, "Announcement sent!", Toast.LENGTH_SHORT).show());
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
