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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DailyLoginRewardsActivity extends AppCompatActivity {

    private RecyclerView rvRewards;
    private ProgressBar progressBar;
    private TextView tvStreak, tvClaimStatus;
    private FirebaseFirestore db;
    private String userId;
    private List<RewardDay> rewards = new ArrayList<>();
    private int currentStreak = 0;
    private boolean claimedToday = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_login_rewards);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();
        rvRewards = findViewById(R.id.rvRewards);
        progressBar = findViewById(R.id.progressBar);
        tvStreak = findViewById(R.id.tvStreak);
        tvClaimStatus = findViewById(R.id.tvClaimStatus);

        rvRewards.setLayoutManager(new GridLayoutManager(this, 4));
        loadRewards();
    }

    private void loadRewards() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("players").document(userId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    currentStreak = doc.getLong("loginStreak") != null ? doc.getLong("loginStreak").intValue() : 0;
                    Long lastLogin = doc.getLong("lastLoginDate");
                    
                    // Check if claimed today
                    Calendar today = Calendar.getInstance();
                    today.set(Calendar.HOUR_OF_DAY, 0);
                    today.set(Calendar.MINUTE, 0);
                    today.set(Calendar.SECOND, 0);
                    today.set(Calendar.MILLISECOND, 0);
                    
                    if (lastLogin != null && lastLogin >= today.getTimeInMillis()) {
                        claimedToday = true;
                    }

                    tvStreak.setText("🔥 Streak: " + currentStreak + " days");
                    tvClaimStatus.setText(claimedToday ? "✓ Claimed today" : "Tap to claim!");

                    // Create 7-day reward cycle
                    rewards.clear();
                    rewards.add(new RewardDay(1, "50 Coins", "50", false));
                    rewards.add(new RewardDay(2, "100 Coins", "100", false));
                    rewards.add(new RewardDay(3, "200 Coins", "200", false));
                    rewards.add(new RewardDay(4, "500 Coins", "500", false));
                    rewards.add(new RewardDay(5, "1000 Coins", "1000", false));
                    rewards.add(new RewardDay(6, "2000 Coins", "2000", false));
                    rewards.add(new RewardDay(7, "5000 Coins + Title", "5000", false));

                    // Mark unlocked days
                    for (int i = 0; i < Math.min(currentStreak, 7); i++) {
                        rewards.get(i).unlocked = true;
                    }

                    progressBar.setVisibility(View.GONE);
                    rvRewards.setAdapter(new RewardsAdapter());
                }
            });
    }

    private void claimReward() {
        if (claimedToday) {
            Toast.makeText(this, "Already claimed today!", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("🎁 Claim Daily Reward")
            .setMessage("Claim your Day " + ((currentStreak % 7) + 1) + " reward?")
            .setPositiveButton("Claim", (d, w) -> {
                int dayIndex = currentStreak % 7;
                int rewardCoins = Integer.parseInt(rewards.get(dayIndex).coins);
                
                // Update streak
                int newStreak = currentStreak + 1;
                if (newStreak > 7) newStreak = 1;

                Map<String, Object> update = new HashMap<>();
                update.put("loginStreak", newStreak);
                update.put("lastLoginDate", System.currentTimeMillis());
                
                // Add coins
                db.collection("players").document(userId).get()
                    .addOnSuccessListener(doc -> {
                        long currentCoins = doc.getLong("coins") != null ? doc.getLong("coins") : 0;
                        update.put("coins", currentCoins + rewardCoins);
                        
                        db.collection("players").document(userId).update(update)
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, "+" + rewardCoins + " Coins!", Toast.LENGTH_SHORT).show();
                                claimedToday = true;
                                tvClaimStatus.setText("✓ Claimed today");
                                loadRewards();
                            });
                    });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    class RewardDay {
        int day;
        String reward, coins;
        boolean unlocked;

        RewardDay(int day, String reward, String coins, boolean unlocked) {
            this.day = day;
            this.reward = reward;
            this.coins = coins;
            this.unlocked = unlocked;
        }
    }

    class RewardsAdapter extends RecyclerView.Adapter<RewardsAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_login_reward, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            RewardDay reward = rewards.get(position);
            holder.tvDay.setText("Day " + reward.day);
            holder.tvReward.setText(reward.reward);
            
            if (reward.unlocked) {
                holder.tvStatus.setText("✓");
                holder.tvStatus.setTextColor(0xFF11998e);
                holder.itemView.setAlpha(1.0f);
            } else {
                holder.tvStatus.setText("🔒");
                holder.tvStatus.setTextColor(0xFF718096);
                holder.itemView.setAlpha(0.5f);
            }

            // Make today's reward clickable
            if (position == currentStreak % 7 && !claimedToday) {
                holder.itemView.setOnClickListener(v -> claimReward());
                holder.itemView.setBackgroundResource(R.drawable.bg_fancy_button_primary);
            } else {
                holder.itemView.setOnClickListener(null);
            }
        }

        @Override
        public int getItemCount() { return rewards.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvDay, tvReward, tvStatus;
            VH(View v) {
                super(v);
                tvDay = v.findViewById(R.id.tvRewardDay);
                tvReward = v.findViewById(R.id.tvRewardAmount);
                tvStatus = v.findViewById(R.id.tvRewardStatus);
            }
        }
    }
}
