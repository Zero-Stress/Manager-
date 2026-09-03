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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class DailyChallengesActivity extends AppCompatActivity {

    private RecyclerView rvChallenges;
    private ProgressBar progressBar;
    private TextView tvRewards;
    private FirebaseFirestore db;
    private String userId;
    private List<ChallengeItem> challenges = new ArrayList<>();
    private int completedCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_challenges);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();
        rvChallenges = findViewById(R.id.rvChallenges);
        progressBar = findViewById(R.id.progressBar);
        tvRewards = findViewById(R.id.tvRewards);

        rvChallenges.setLayoutManager(new LinearLayoutManager(this));
        loadChallenges();
    }

    private void loadChallenges() {
        progressBar.setVisibility(View.VISIBLE);
        
        db.collection("players").document(userId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    long kills = doc.getLong("kills") != null ? doc.getLong("kills") : 0;
                    long wins = doc.getLong("wins") != null ? doc.getLong("wins") : 0;
                    long matches = doc.getLong("matches") != null ? doc.getLong("matches") : 0;
                    long damage = doc.getLong("damage") != null ? doc.getLong("damage") : 0;
                    long coins = doc.getLong("coins") != null ? doc.getLong("coins") : 0;

                    challenges.clear();
                    completedCount = 0;

                    // Create challenges
                    ChallengeItem c1 = new ChallengeItem("Get 10 kills", "Kill 10 enemies today", 10, (int) kills, "50 XP");
                    ChallengeItem c2 = new ChallengeItem("Win 3 matches", "Win 3 matches today", 3, (int) wins, "100 XP");
                    ChallengeItem c3 = new ChallengeItem("Play 5 matches", "Complete 5 matches", 5, (int) matches, "75 XP");
                    ChallengeItem c4 = new ChallengeItem("Deal 5000 damage", "Deal 5000 total damage", 5000, (int) damage, "150 XP");
                    ChallengeItem c5 = new ChallengeItem("Login today", "Open the app", 1, 1, "25 Coins");

                    challenges.add(c1);
                    challenges.add(c2);
                    challenges.add(c3);
                    challenges.add(c4);
                    challenges.add(c5);

                    for (ChallengeItem c : challenges) {
                        if (c.current >= c.target) completedCount++;
                    }

                    tvRewards.setText(completedCount + "/5 Completed • Bonus: " + (completedCount * 50) + " XP");
                    progressBar.setVisibility(View.GONE);
                    rvChallenges.setAdapter(new ChallengesAdapter());
                }
            });
    }

    class ChallengeItem {
        String title, description, reward;
        int target, current;

        ChallengeItem(String title, String description, int target, int current, String reward) {
            this.title = title;
            this.description = description;
            this.target = target;
            this.current = Math.min(current, target);
            this.reward = reward;
        }

        boolean isCompleted() {
            return current >= target;
        }
    }

    class ChallengesAdapter extends RecyclerView.Adapter<ChallengesAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_challenge, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ChallengeItem challenge = challenges.get(position);
            holder.tvTitle.setText(challenge.title);
            holder.tvDescription.setText(challenge.description);
            holder.tvProgress.setText(challenge.current + "/" + challenge.target);
            holder.tvReward.setText(challenge.reward);
            
            int progress = (int) ((challenge.current * 100) / challenge.target);
            holder.progressBar.setProgress(Math.min(progress, 100));
            
            if (challenge.isCompleted()) {
                holder.tvStatus.setText("✓ COMPLETED");
                holder.tvStatus.setTextColor(0xFF11998e);
                holder.itemView.setAlpha(0.8f);
            } else {
                holder.tvStatus.setText("IN PROGRESS");
                holder.tvStatus.setTextColor(0xFF667eea);
                holder.itemView.setAlpha(1.0f);
            }
        }

        @Override
        public int getItemCount() { return challenges.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDescription, tvProgress, tvReward, tvStatus;
            ProgressBar progressBar;
            VH(View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tvChallengeTitle);
                tvDescription = v.findViewById(R.id.tvChallengeDescription);
                tvProgress = v.findViewById(R.id.tvChallengeProgress);
                tvReward = v.findViewById(R.id.tvChallengeReward);
                tvStatus = v.findViewById(R.id.tvChallengeStatus);
                progressBar = v.findViewById(R.id.progressChallenge);
            }
        }
    }
}
