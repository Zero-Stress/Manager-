package com.zerostress.manager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeaderboardActivity extends AppCompatActivity {

    private RecyclerView rvLeaderboard;
    private ProgressBar progressBar;
    private TextView tvPeriod;
    private MaterialButton btnReset;
    private LeaderboardAdapter adapter;
    private List<DocumentSnapshot> players = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private int currentTab = 0;
    private boolean isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        rvLeaderboard = findViewById(R.id.rvLeaderboard);
        progressBar = findViewById(R.id.progressBar);
        tvPeriod = findViewById(R.id.tvPeriod);
        btnReset = findViewById(R.id.btnReset);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        rvLeaderboard.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LeaderboardAdapter();
        rvLeaderboard.setAdapter(adapter);

        // Check if admin
        checkAdminStatus();

        // Add tabs
        tabLayout.addTab(tabLayout.newTab().setText("Daily"));
        tabLayout.addTab(tabLayout.newTab().setText("Weekly"));
        tabLayout.addTab(tabLayout.newTab().setText("Monthly"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                loadLeaderboard();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Reset button (admin only)
        btnReset.setOnClickListener(v -> showResetDialog());

        loadLeaderboard();
    }

    private void checkAdminStatus() {
        String userId = auth.getUid();
        if (userId == null) return;

        db.collection("players").document(userId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String role = doc.getString("role");
                    isAdmin = "admin".equals(role);
                    btnReset.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                }
            });
    }

    private void showResetDialog() {
        String[] options = {"Reset Daily Only", "Reset Weekly Only", "Reset Monthly Only", "Reset All"};
        
        new AlertDialog.Builder(this)
            .setTitle("⚠️ Reset Leaderboard")
            .setMessage("Choose which leaderboard to reset:")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: resetLeaderboard("daily"); break;
                    case 1: resetLeaderboard("weekly"); break;
                    case 2: resetLeaderboard("monthly"); break;
                    case 3: resetLeaderboard("all"); break;
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void resetLeaderboard(String type) {
        String confirmMessage = "";
        switch (type) {
            case "daily":
                confirmMessage = "Reset Daily leaderboard only?\nWeekly and Monthly will NOT be affected.";
                break;
            case "weekly":
                confirmMessage = "Reset Weekly leaderboard only?\nDaily and Monthly will NOT be affected.";
                break;
            case "monthly":
                confirmMessage = "Reset Monthly leaderboard only?\nDaily and Weekly will NOT be affected.";
                break;
            case "all":
                confirmMessage = "Reset ALL leaderboards?";
                break;
        }

        new AlertDialog.Builder(this)
            .setTitle("Confirm Reset")
            .setMessage(confirmMessage)
            .setPositiveButton("Reset", (d, w) -> performReset(type))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void performReset(String type) {
        progressBar.setVisibility(View.VISIBLE);
        
        // Get all players
        db.collection("players")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener(query -> {
                int totalPlayers = query.size();
                int[] completed = {0};

                for (DocumentSnapshot doc : query.getDocuments()) {
                    String playerId = doc.getId();
                    Map<String, Object> resetData = new HashMap<>();
                    
                    switch (type) {
                        case "daily":
                            // Only reset daily stats
                            resetData.put("dailyKills", 0);
                            resetData.put("dailyWins", 0);
                            resetData.put("dailyDamage", 0);
                            resetData.put("dailyMatches", 0);
                            resetData.put("dailyScore", 0);
                            resetData.put("lastDailyReset", System.currentTimeMillis());
                            break;
                        case "weekly":
                            // Only reset weekly stats
                            resetData.put("weeklyKills", 0);
                            resetData.put("weeklyWins", 0);
                            resetData.put("weeklyDamage", 0);
                            resetData.put("weeklyMatches", 0);
                            resetData.put("weeklyScore", 0);
                            resetData.put("lastWeeklyReset", System.currentTimeMillis());
                            break;
                        case "monthly":
                            // Only reset monthly stats
                            resetData.put("monthlyKills", 0);
                            resetData.put("monthlyWins", 0);
                            resetData.put("monthlyDamage", 0);
                            resetData.put("monthlyMatches", 0);
                            resetData.put("monthlyScore", 0);
                            resetData.put("lastMonthlyReset", System.currentTimeMillis());
                            break;
                        case "all":
                            // Reset all stats
                            resetData.put("score", 0);
                            resetData.put("kills", 0);
                            resetData.put("wins", 0);
                            resetData.put("damage", 0);
                            resetData.put("matches", 0);
                            resetData.put("dailyKills", 0);
                            resetData.put("dailyWins", 0);
                            resetData.put("dailyDamage", 0);
                            resetData.put("dailyMatches", 0);
                            resetData.put("dailyScore", 0);
                            resetData.put("weeklyKills", 0);
                            resetData.put("weeklyWins", 0);
                            resetData.put("weeklyDamage", 0);
                            resetData.put("weeklyMatches", 0);
                            resetData.put("weeklyScore", 0);
                            resetData.put("monthlyKills", 0);
                            resetData.put("monthlyWins", 0);
                            resetData.put("monthlyDamage", 0);
                            resetData.put("monthlyMatches", 0);
                            resetData.put("monthlyScore", 0);
                            resetData.put("lastDailyReset", System.currentTimeMillis());
                            resetData.put("lastWeeklyReset", System.currentTimeMillis());
                            resetData.put("lastMonthlyReset", System.currentTimeMillis());
                            break;
                    }

                    db.collection("players").document(playerId).update(resetData)
                        .addOnSuccessListener(v -> {
                            completed[0]++;
                            if (completed[0] >= totalPlayers) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, type.toUpperCase() + " leaderboard reset!", Toast.LENGTH_SHORT).show();
                                loadLeaderboard();
                            }
                        });
                }
            });
    }

    private void loadLeaderboard() {
        progressBar.setVisibility(View.VISIBLE);
        
        db.collection("players")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener(query -> {
                progressBar.setVisibility(View.GONE);
                players.clear();
                
                for (DocumentSnapshot doc : query.getDocuments()) {
                    String role = doc.getString("role");
                    if (!"admin".equals(role)) {
                        players.add(doc);
                    }
                }
                
                // Sort based on tab
                Collections.sort(players, (a, b) -> {
                    long scoreA = getScoreForTab(a);
                    long scoreB = getScoreForTab(b);
                    return Long.compare(scoreB, scoreA);
                });
                
                String[] periods = {"Daily", "Weekly", "Monthly"};
                tvPeriod.setText(periods[currentTab] + " Leaderboard");
                adapter.notifyDataSetChanged();
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Error loading leaderboard", Toast.LENGTH_SHORT).show();
            });
    }

    private long getScoreForTab(DocumentSnapshot doc) {
        switch (currentTab) {
            case 0: // Daily
                return doc.getLong("dailyScore") != null ? doc.getLong("dailyScore") : 0;
            case 1: // Weekly
                return doc.getLong("weeklyScore") != null ? doc.getLong("weeklyScore") : 0;
            case 2: // Monthly
                return doc.getLong("monthlyScore") != null ? doc.getLong("monthlyScore") : 0;
            default:
                return doc.getLong("score") != null ? doc.getLong("score") : 0;
        }
    }

    private long getKillsForTab(DocumentSnapshot doc) {
        switch (currentTab) {
            case 0: return doc.getLong("dailyKills") != null ? doc.getLong("dailyKills") : 0;
            case 1: return doc.getLong("weeklyKills") != null ? doc.getLong("weeklyKills") : 0;
            case 2: return doc.getLong("monthlyKills") != null ? doc.getLong("monthlyKills") : 0;
            default: return doc.getLong("kills") != null ? doc.getLong("kills") : 0;
        }
    }

    private long getWinsForTab(DocumentSnapshot doc) {
        switch (currentTab) {
            case 0: return doc.getLong("dailyWins") != null ? doc.getLong("dailyWins") : 0;
            case 1: return doc.getLong("weeklyWins") != null ? doc.getLong("weeklyWins") : 0;
            case 2: return doc.getLong("monthlyWins") != null ? doc.getLong("monthlyWins") : 0;
            default: return doc.getLong("wins") != null ? doc.getLong("wins") : 0;
        }
    }

    class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_player, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            DocumentSnapshot doc = players.get(position);
            String name = doc.getString("name");
            long score = getScoreForTab(doc);
            long kills = getKillsForTab(doc);
            long wins = getWinsForTab(doc);
            long level = doc.getLong("level") != null ? doc.getLong("level") : 1;
            String rank = doc.getString("rank") != null ? doc.getString("rank") : "Iron";

            String medal = "";
            int medalColor = 0;
            if (position == 0) {
                medal = "👑 ";
                medalColor = 0xFFFFD700;
            } else if (position == 1) {
                medal = "🥈 ";
                medalColor = 0xFFC0C0C0;
            } else if (position == 2) {
                medal = "🥉 ";
                medalColor = 0xFFCD7F32;
            }

            holder.tvName.setText(medal + name);
            holder.tvRole.setText("Lv." + level + " • " + rank + " | K:" + kills + " W:" + wins);
            holder.tvScore.setText(score + " pts");
            
            if (medalColor != 0) {
                holder.tvName.setTextColor(medalColor);
            } else {
                holder.tvName.setTextColor(0xFFFFFFFF);
            }
        }

        @Override
        public int getItemCount() { return players.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvRole, tvScore;
            VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvName);
                tvRole = v.findViewById(R.id.tvRole);
                tvScore = v.findViewById(R.id.tvScore);
            }
        }
    }
}
