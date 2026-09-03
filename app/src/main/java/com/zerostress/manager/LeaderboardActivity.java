package com.zerostress.manager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {

    private RecyclerView rvLeaderboard;
    private ProgressBar progressBar;
    private TextView tvPeriod;
    private LeaderboardAdapter adapter;
    private List<DocumentSnapshot> players = new ArrayList<>();
    private FirebaseFirestore db;
    private int currentTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        db = FirebaseFirestore.getInstance();
        rvLeaderboard = findViewById(R.id.rvLeaderboard);
        progressBar = findViewById(R.id.progressBar);
        tvPeriod = findViewById(R.id.tvPeriod);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        rvLeaderboard.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LeaderboardAdapter();
        rvLeaderboard.setAdapter(adapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                loadLeaderboard();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        loadLeaderboard();
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
                    // Only show players, not admins
                    String role = doc.getString("role");
                    if (!"admin".equals(role)) {
                        players.add(doc);
                    }
                }
                Collections.sort(players, (a, b) -> {
                    long sa = a.getLong("score") != null ? a.getLong("score") : 0;
                    long sb = b.getLong("score") != null ? b.getLong("score") : 0;
                    return Long.compare(sb, sa);
                });
                String[] periods = {"Daily", "Weekly", "All Time"};
                tvPeriod.setText(periods[currentTab] + " Leaderboard");
                adapter.notifyDataSetChanged();
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Error loading leaderboard", Toast.LENGTH_SHORT).show();
            });
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
            long score = doc.getLong("score") != null ? doc.getLong("score") : 0;
            long kills = doc.getLong("kills") != null ? doc.getLong("kills") : 0;
            long wins = doc.getLong("wins") != null ? doc.getLong("wins") : 0;
            long damage = doc.getLong("damage") != null ? doc.getLong("damage") : 0;

            String medal = "";
            if (position == 0) medal = "👑 ";
            else if (position == 1) medal = "🥈 ";
            else if (position == 2) medal = "🥉 ";

            holder.tvName.setText(medal + name);
            holder.tvRole.setText("K:" + kills + " W:" + wins + " Dmg:" + damage);
            holder.tvScore.setText(score + " pts");
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
