package com.zerostress.manager;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AchievementsActivity extends AppCompatActivity {

    private RecyclerView rvAchievements;
    private TextView tvEmpty;
    private FirebaseFirestore db;
    private String userId;
    private List<String> unlockedIds = new ArrayList<>();

    // All available achievements
    private final String[][] ACHIEVEMENTS = {
        {"first_blood", "First Blood", "Get your first kill", "10", "50"},
        {"kill_100", "Century Killer", "Get 100 total kills", "50", "200"},
        {"kill_500", "Rampage", "Get 500 total kills", "100", "500"},
        {"win_10", "Winner", "Win 10 matches", "50", "250"},
        {"win_50", "Champion", "Win 50 matches", "200", "1000"},
        {"damage_10000", "Damage Dealer", "Deal 10,000 total damage", "100", "300"},
        {"level_5", "Rising Star", "Reach Level 5", "50", "150"},
        {"level_10", "Veteran", "Reach Level 10", "150", "500"},
        {"level_25", "Legend", "Reach Level 25", "500", "2000"},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();
        rvAchievements = findViewById(R.id.rvAchievements);
        tvEmpty = findViewById(R.id.tvEmpty);

        rvAchievements.setLayoutManager(new LinearLayoutManager(this));
        loadAchievements();
    }

    private void loadAchievements() {
        db.collection("player_achievements")
            .whereEqualTo("playerId", userId)
            .get()
            .addOnSuccessListener(query -> {
                unlockedIds.clear();
                for (DocumentSnapshot doc : query.getDocuments()) {
                    String achId = doc.getString("achievementId");
                    if (achId != null) unlockedIds.add(achId);
                }
                rvAchievements.setAdapter(new AchievementAdapter());
            });
    }

    class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.VH> {
        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View v = getLayoutInflater().inflate(R.layout.item_achievement, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            String[] ach = ACHIEVEMENTS[position];
            boolean unlocked = unlockedIds.contains(ach[0]);
            holder.tvName.setText(ach[1]);
            holder.tvDesc.setText(ach[2]);
            holder.tvReward.setText("+" + ach[3] + " XP, +" + ach[4] + " Coins");
            holder.tvStatus.setText(unlocked ? "✅ Unlocked" : "🔒 Locked");
            holder.itemView.setAlpha(unlocked ? 1.0f : 0.5f);
        }

        @Override
        public int getItemCount() { return ACHIEVEMENTS.length; }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvDesc, tvReward, tvStatus;
            VH(android.view.View v) {
                super(v);
                tvName = v.findViewById(R.id.tvAchName);
                tvDesc = v.findViewById(R.id.tvAchDesc);
                tvReward = v.findViewById(R.id.tvAchReward);
                tvStatus = v.findViewById(R.id.tvAchStatus);
            }
        }
    }
}
