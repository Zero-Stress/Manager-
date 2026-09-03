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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class BattlePassActivity extends AppCompatActivity {

    private RecyclerView rvBattlePass;
    private ProgressBar progressBar;
    private TextView tvSeasonInfo, tvLevelProgress;
    private FirebaseFirestore db;
    private String userId;
    private List<BattlePassTier> tiers = new ArrayList<>();
    private int currentLevel = 1;
    private int currentXP = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battle_pass);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();
        rvBattlePass = findViewById(R.id.rvBattlePass);
        progressBar = findViewById(R.id.progressBar);
        tvSeasonInfo = findViewById(R.id.tvSeasonInfo);
        tvLevelProgress = findViewById(R.id.tvLevelProgress);

        rvBattlePass.setLayoutManager(new LinearLayoutManager(this));
        loadBattlePass();
    }

    private void loadBattlePass() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("players").document(userId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    currentLevel = doc.getLong("level") != null ? doc.getLong("level").intValue() : 1;
                    currentXP = doc.getLong("xp") != null ? doc.getLong("xp").intValue() : 0;
                    
                    tvSeasonInfo.setText("Season 1 • Level " + currentLevel);
                    tvLevelProgress.setText(currentXP + " / " + (currentLevel * 500) + " XP to next level");

                    // Create battle pass tiers
                    tiers.clear();
                    String[] freeRewards = {"50 XP", "100 Coins", "Bronze Title", "200 XP", "500 Coins",
                        "Silver Title", "500 XP", "1000 Coins", "Gold Title", "1000 XP",
                        "2000 Coins", "Diamond Title", "2000 XP", "5000 Coins", "Mythic Title"};
                    
                    String[] premiumRewards = {"100 XP", "200 Coins", "Rare Skin", "400 XP", "1000 Coins",
                        "Epic Skin", "1000 XP", "2000 Coins", "Legendary Skin", "2000 XP",
                        "5000 Coins", "Mythic Skin", "5000 XP", "10000 Coins", "Exclusive Skin"};

                    for (int i = 0; i < 15; i++) {
                        tiers.add(new BattlePassTier(i + 1, freeRewards[i], premiumRewards[i], i + 1 <= currentLevel));
                    }

                    progressBar.setVisibility(View.GONE);
                    rvBattlePass.setAdapter(new BattlePassAdapter());
                }
            });
    }

    class BattlePassTier {
        int level;
        String freeReward, premiumReward;
        boolean unlocked;

        BattlePassTier(int level, String freeReward, String premiumReward, boolean unlocked) {
            this.level = level;
            this.freeReward = freeReward;
            this.premiumReward = premiumReward;
            this.unlocked = unlocked;
        }
    }

    class BattlePassAdapter extends RecyclerView.Adapter<BattlePassAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_battle_pass_tier, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            BattlePassTier tier = tiers.get(position);
            holder.tvLevel.setText("Tier " + tier.level);
            holder.tvFreeReward.setText("FREE: " + tier.freeReward);
            holder.tvPremiumReward.setText("PREMIUM: " + tier.premiumReward);
            
            if (tier.unlocked) {
                holder.tvStatus.setText("✓ UNLOCKED");
                holder.tvStatus.setTextColor(0xFF11998e);
                holder.itemView.setAlpha(1.0f);
            } else {
                holder.tvStatus.setText("LOCKED");
                holder.tvStatus.setTextColor(0xFF718096);
                holder.itemView.setAlpha(0.6f);
            }
        }

        @Override
        public int getItemCount() { return tiers.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvLevel, tvFreeReward, tvPremiumReward, tvStatus;
            VH(View v) {
                super(v);
                tvLevel = v.findViewById(R.id.tvTierLevel);
                tvFreeReward = v.findViewById(R.id.tvFreeReward);
                tvPremiumReward = v.findViewById(R.id.tvPremiumReward);
                tvStatus = v.findViewById(R.id.tvTierStatus);
            }
        }
    }
}
