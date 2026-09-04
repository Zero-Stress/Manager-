package com.zerostress.manager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ViewAllPlayersStatsActivity extends AppCompatActivity {

    private RecyclerView rvPlayers;
    private ProgressBar progressBar;
    private TextView tvSummary;
    private FirebaseFirestore db;
    private List<DocumentSnapshot> players = new ArrayList<>();
    private StatsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_all_players_stats);

        db = FirebaseFirestore.getInstance();
        rvPlayers = findViewById(R.id.rvPlayersStats);
        progressBar = findViewById(R.id.progressBar);
        tvSummary = findViewById(R.id.tvSummary);

        rvPlayers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StatsAdapter();
        rvPlayers.setAdapter(adapter);

        loadPlayersStats();
    }

    private void loadPlayersStats() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("players")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener(query -> {
                progressBar.setVisibility(View.GONE);
                players.clear();
                for (DocumentSnapshot doc : query.getDocuments()) {
                    players.add(doc);
                }

                // Sort by score
                Collections.sort(players, (a, b) -> {
                    long sa = a.getLong("score") != null ? a.getLong("score") : 0;
                    long sb = b.getLong("score") != null ? b.getLong("score") : 0;
                    return Long.compare(sb, sa);
                });

                // Calculate summary
                int totalPlayers = players.size();
                long totalKills = 0, totalWins = 0, totalMatches = 0;
                for (DocumentSnapshot doc : players) {
                    totalKills += doc.getLong("kills") != null ? doc.getLong("kills") : 0;
                    totalWins += doc.getLong("wins") != null ? doc.getLong("wins") : 0;
                    totalMatches += doc.getLong("matches") != null ? doc.getLong("matches") : 0;
                }

                tvSummary.setText("👥 " + totalPlayers + " Players | ⚔️ " + totalKills + " Kills | 🏆 " + totalWins + " Wins | 🎮 " + totalMatches + " Matches");
                rvPlayers.setAdapter(adapter);
            });
    }

    class StatsAdapter extends RecyclerView.Adapter<StatsAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_player_stats, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            DocumentSnapshot doc = players.get(position);
            
            holder.tvRank.setText("#" + (position + 1));
            holder.tvName.setText(doc.getString("name"));
            
            String role = doc.getString("role") != null ? doc.getString("role") : "player";
            String gameRole = doc.getString("gameRole");
            String displayRole = role.toUpperCase();
            int roleColor = 0xFF11998e;
            if ("admin".equals(role)) {
                displayRole = "👑 ADMIN";
                roleColor = 0xFF667eea;
            } else if ("moderator".equals(role)) {
                displayRole = "🛡️ MOD";
                roleColor = 0xFFa855f7;
            }
            if (gameRole != null && !gameRole.isEmpty()) {
                String emoji = "";
                switch (gameRole) {
                    case "Rusher": emoji = "⚔️ "; break;
                    case "Sniper": emoji = "🎯 "; break;
                    case "IGL": emoji = "👑 "; break;
                    case "Supporter": emoji = "🛡️ "; break;
                    case "Bomber": emoji = "💣 "; break;
                }
                displayRole = emoji + gameRole + " • " + displayRole;
            }
            holder.tvRole.setText(displayRole);
            holder.tvRole.setTextColor(roleColor);

            long score = doc.getLong("score") != null ? doc.getLong("score") : 0;
            long kills = doc.getLong("kills") != null ? doc.getLong("kills") : 0;
            long wins = doc.getLong("wins") != null ? doc.getLong("wins") : 0;
            long matches = doc.getLong("matches") != null ? doc.getLong("matches") : 0;
            long level = doc.getLong("level") != null ? doc.getLong("level") : 1;
            String playerRank = doc.getString("rank") != null ? doc.getString("rank") : "Iron";

            holder.tvScore.setText(score + " pts");
            holder.tvStats.setText("Lv." + level + " " + playerRank + " | K:" + kills + " W:" + wins + " M:" + matches);

            // Medal for top 3
            if (position == 0) holder.tvRank.setTextColor(0xFFFFD700);
            else if (position == 1) holder.tvRank.setTextColor(0xFFC0C0C0);
            else if (position == 2) holder.tvRank.setTextColor(0xFFCD7F32);
            else holder.tvRank.setTextColor(0xFF718096);
        }

        @Override
        public int getItemCount() { return players.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvRank, tvName, tvRole, tvScore, tvStats;
            VH(View v) {
                super(v);
                tvRank = v.findViewById(R.id.tvPlayerRank);
                tvName = v.findViewById(R.id.tvPlayerName);
                tvRole = v.findViewById(R.id.tvPlayerRole);
                tvScore = v.findViewById(R.id.tvPlayerScore);
                tvStats = v.findViewById(R.id.tvPlayerStats);
            }
        }
    }
}
