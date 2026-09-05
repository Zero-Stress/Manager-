package com.zerostress.manager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
        // Radio-button selection: Daily / Weekly / Monthly / All
        // Default selection = currently viewed tab
        String[] options = {
            "📅 Daily Leaderboard",
            "📆 Weekly Leaderboard",
            "🗓️ Monthly Leaderboard",
            "💣 Everything (All Time)"
        };
        final int[] selected = {currentTab}; // pre-select current tab

        new AlertDialog.Builder(this)
            .setTitle("⚠️ Reset Leaderboard")
            .setSingleChoiceItems(options, currentTab, (dialog, which) -> selected[0] = which)
            .setPositiveButton("Next →", (d, w) -> {
                String type;
                switch (selected[0]) {
                    case 0: type = "daily"; break;
                    case 1: type = "weekly"; break;
                    case 2: type = "monthly"; break;
                    default: type = "all"; break;
                }
                resetLeaderboard(type);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void resetLeaderboard(String type) {
        String confirmMessage;
        String title;
        switch (type) {
            case "daily":
                title = "📅 Reset Daily Leaderboard";
                confirmMessage = "All DAILY stats (kills, wins, score) will be set to 0 for every player.\n\nWeekly and Monthly stats are NOT affected.";
                break;
            case "weekly":
                title = "📆 Reset Weekly Leaderboard";
                confirmMessage = "All WEEKLY stats (kills, wins, score) will be set to 0 for every player.\n\nDaily and Monthly stats are NOT affected.";
                break;
            case "monthly":
                title = "🗓️ Reset Monthly Leaderboard";
                confirmMessage = "All MONTHLY stats (kills, wins, score) will be set to 0 for every player.\n\nDaily and Weekly stats are NOT affected.";
                break;
            default:
                title = "💣 Reset EVERYTHING";
                confirmMessage = "⚠️ DANGER ZONE ⚠️\n\nThis resets ALL stats for every player:\n• Total score, kills, wins, damage\n• XP → Level 1\n• Coins → 0\n• Rank → Iron\n• Daily, Weekly & Monthly stats\n\nThis cannot be undone!";
                break;
        }

        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(confirmMessage)
            .setPositiveButton("RESET NOW", (d, w) -> performReset(type))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void performReset(String type) {
        progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(this, "⏳ Resetting " + type + " leaderboard...", Toast.LENGTH_SHORT).show();

        // Get all players (including admins for reset)
        db.collection("players")
            .get()
            .addOnSuccessListener(query -> {
                int totalPlayers = query.size();
                if (totalPlayers == 0) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "No players to reset", Toast.LENGTH_SHORT).show();
                    return;
                }
                final int total = totalPlayers;
                int[] completed = {0};

                for (DocumentSnapshot doc : query.getDocuments()) {
                    String playerId = doc.getId();
                    Map<String, Object> resetData = new HashMap<>();
                    
                    switch (type) {
                        case "daily":
                            // Only reset daily stats
                            resetData.put("dailyKills", 0);
                            resetData.put("dailyWins", 0);
                            resetData.put("dailyAssists", 0);
                            resetData.put("dailyDamage", 0);
                            resetData.put("dailyMatches", 0);
                            resetData.put("dailyScore", 0);
                            resetData.put("lastDailyReset", System.currentTimeMillis());
                            break;
                        case "weekly":
                            // Only reset weekly stats
                            resetData.put("weeklyKills", 0);
                            resetData.put("weeklyWins", 0);
                            resetData.put("weeklyAssists", 0);
                            resetData.put("weeklyDamage", 0);
                            resetData.put("weeklyMatches", 0);
                            resetData.put("weeklyScore", 0);
                            resetData.put("lastWeeklyReset", System.currentTimeMillis());
                            break;
                        case "monthly":
                            // Only reset monthly stats
                            resetData.put("monthlyKills", 0);
                            resetData.put("monthlyWins", 0);
                            resetData.put("monthlyAssists", 0);
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
                            resetData.put("assists", 0);
                            resetData.put("damage", 0);
                            resetData.put("matches", 0);
                            resetData.put("xp", 0);
                            resetData.put("level", 1);
                            resetData.put("rank", "Iron");
                            resetData.put("coins", 0);
                            resetData.put("dailyKills", 0);
                            resetData.put("dailyWins", 0);
                            resetData.put("dailyAssists", 0);
                            resetData.put("dailyDamage", 0);
                            resetData.put("dailyMatches", 0);
                            resetData.put("dailyScore", 0);
                            resetData.put("weeklyKills", 0);
                            resetData.put("weeklyWins", 0);
                            resetData.put("weeklyAssists", 0);
                            resetData.put("weeklyDamage", 0);
                            resetData.put("weeklyMatches", 0);
                            resetData.put("weeklyScore", 0);
                            resetData.put("monthlyKills", 0);
                            resetData.put("monthlyWins", 0);
                            resetData.put("monthlyAssists", 0);
                            resetData.put("monthlyDamage", 0);
                            resetData.put("monthlyMatches", 0);
                            resetData.put("monthlyScore", 0);
                            resetData.put("lastDailyReset", System.currentTimeMillis());
                            resetData.put("lastWeeklyReset", System.currentTimeMillis());
                            resetData.put("lastMonthlyReset", System.currentTimeMillis());
                            break;
                    }

                    final int[] done = completed;
                    db.collection("players").document(playerId).update(resetData)
                        .addOnSuccessListener(v -> {
                            done[0]++;
                            if (done[0] >= total) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "✅ " + type.toUpperCase() + " leaderboard reset for " + total + " players!", Toast.LENGTH_LONG).show();
                                
                                // Award rewards to top 3 players after reset
                                awardLeaderboardRewards(type);
                                
                                // Delay reload to let Firestore propagate
                                new android.os.Handler().postDelayed(() -> loadLeaderboard(), 1000);
                            }
                        })
                        .addOnFailureListener(e -> {
                            done[0]++;
                            if (done[0] >= total) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Reset completed with some errors", Toast.LENGTH_SHORT).show();
                                loadLeaderboard();
                            }
                        });
                }
            });
        }
    }

    private void awardLeaderboardRewards(String type) {
        // Award coins and titles to top 3 players in the current leaderboard
        // For Daily/Weekly/Monthly: award small amounts
        // For 'all': award larger amounts
        
        long rewardCoins;
        String rewardTitle;
        
        switch (type) {
            case "daily":
                rewardCoins = 50;  // 50 coins for daily top 3
                rewardTitle = "Daily Champion";
                break;
            case "weekly":
                rewardCoins = 200;  // 200 coins for weekly top 3
                rewardTitle = "Weekly Champion";
                break;
            case "monthly":
                rewardCoins = 500;  // 500 coins for monthly top 3
                rewardTitle = "Monthly Champion";
                break;
            case "all":
                rewardCoins = 1000;  // 1000 coins for all-time top 3
                rewardTitle = "All-Time Legend";
                break;
            default:
                rewardCoins = 0;
                rewardTitle = "Champion";
                break;
        }
        
        // Get top players from current leaderboard view
        // We need to re-fetch and get the top 3
        db.collection("players")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener(query -> {
                List<DocumentSnapshot> playersList = new ArrayList<>();
                
                for (DocumentSnapshot doc : query.getDocuments()) {
                    String role = doc.getString("role");
                    if (!"admin".equals(role)) {
                        playersList.add(doc);
                    }
                }
                
                // Sort by current tab score
                Collections.sort(playersList, (a, b) -> {
                    long scoreA = getScoreForTab(a);
                    long scoreB = getScoreForTab(b);
                    return Long.compare(scoreB, scoreA);
                });
                
                // Award top 3
                int maxAward = Math.min(3, playersList.size());
                for (int i = 0; i < maxAward; i++) {
                    DocumentSnapshot player = playersList.get(i);
                    String playerId = player.getId();
                    String playerName = player.getString("name");
                    
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("coins", player.getLong("coins") != null ? player.getLong("coins").intValue() + rewardCoins : rewardCoins);
                    
                    db.collection("players").document(playerId).update(updates)
                        .addOnSuccessListener(v -> {
                            Log.d("Leaderboard", "Awarded " + rewardCoins + " coins to " + playerName);
                        })
                        .addOnFailureListener(e -> {
                            Log.e("Leaderboard", "Failed to award coins to " + playerName, e);
                        });
                    
                    // Also add title if not already awarded
                    if (player.getLong("level") != null && player.getLong("level") >= 5) {
                        db.collection("player_titles").document(playerId + "_" + rewardTitle)
                            .set(new HashMap<String, Object>() {{
                                put("title", rewardTitle);
                                put("awardedAt", System.currentTimeMillis());
                                put("awardedFor", type);
                            }})
                            .addOnSuccessListener(v -> {
                                Log.d("Leaderboard", "Awarded title '" + rewardTitle + "' to " + playerName);
                            });
                    }
                }
                
                Toast.makeText(this, "🏆 Rewards distributed to top 3!", Toast.LENGTH_SHORT).show();
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

            String gameRole = doc.getString("gameRole");
            String gameRoleText = "";
            if (gameRole != null && !gameRole.isEmpty()) {
                switch (gameRole) {
                    case "Rusher": gameRoleText = "⚔️ Rusher"; break;
                    case "Sniper": gameRoleText = "🎯 Sniper"; break;
                    case "IGL": gameRoleText = "👑 IGL"; break;
                    case "Supporter": gameRoleText = "🛡️ Supporter"; break;
                    case "Bomber": gameRoleText = "💣 Bomber"; break;
                    default: gameRoleText = gameRole; break;
                }
            }

            holder.tvName.setText(medal + name);
            String statsText = "Lv." + level + " • " + rank + " | K:" + kills + " W:" + wins;
            if (!gameRoleText.isEmpty()) {
                statsText = gameRoleText + " | " + statsText;
            }
            holder.tvRole.setText(statsText);
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
