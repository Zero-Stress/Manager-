package com.zerostress.manager;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PerformanceActivity extends AppCompatActivity {

    private RecyclerView rvPerformance;
    private TextView tvSummary;
    private FirebaseFirestore db;
    private String userId;
    private List<DocumentSnapshot> logs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_performance);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();
        rvPerformance = findViewById(R.id.rvPerformance);
        tvSummary = findViewById(R.id.tvPerformanceSummary);

        rvPerformance.setLayoutManager(new LinearLayoutManager(this));
        loadPerformance();
    }

    private void loadPerformance() {
        db.collection("match_logs").whereEqualTo("playerId", userId)
            .orderBy("date").limit(50)
            .addSnapshotListener((snapshots, e) -> {
                if (e != null || snapshots == null) return;
                logs.clear();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    logs.add(doc);
                }
                Collections.reverse(logs);

                long totalKills = 0, totalDamage = 0, totalWins = 0;
                for (DocumentSnapshot doc : logs) {
                    Long kills = doc.getLong("kills");
                    Long damage = doc.getLong("damage");
                    Boolean win = doc.getBoolean("win");
                    if (kills != null) totalKills += kills;
                    if (damage != null) totalDamage += damage;
                    if (win != null && win) totalWins++;
                }

                tvSummary.setText("Last " + logs.size() + " matches: " + totalKills + " kills, " + totalDamage + " damage, " + totalWins + " wins");
                rvPerformance.setAdapter(new PerformanceAdapter());
            });
    }

    class PerformanceAdapter extends RecyclerView.Adapter<PerformanceAdapter.VH> {
        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View v = getLayoutInflater().inflate(R.layout.item_player, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            DocumentSnapshot doc = logs.get(position);
            Long kills = doc.getLong("kills");
            Long damage = doc.getLong("damage");
            Boolean win = doc.getBoolean("win");
            Long date = doc.getLong("date");

            holder.tvName.setText((win != null && win ? "🏆 " : "❌ ") + "K:" + (kills != null ? kills : 0) + " Dmg:" + (damage != null ? damage : 0));
            if (date != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd HH:mm", Locale.getDefault());
                holder.tvRole.setText(sdf.format(new Date(date)));
            }
            holder.tvScore.setText(doc.getString("matchType"));
        }

        @Override
        public int getItemCount() { return logs.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvRole, tvScore;
            VH(android.view.View v) {
                super(v);
                tvName = v.findViewById(R.id.tvName);
                tvRole = v.findViewById(R.id.tvRole);
                tvScore = v.findViewById(R.id.tvScore);
            }
        }
    }
}
