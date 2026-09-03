package com.zerostress.manager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerTitlesActivity extends AppCompatActivity {

    private RecyclerView rvTitles;
    private FirebaseFirestore db;
    private String userId;
    private String currentTitle = "";
    private List<TitleItem> titles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_titles);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();
        rvTitles = findViewById(R.id.rvTitles);

        rvTitles.setLayoutManager(new GridLayoutManager(this, 2));
        loadTitles();
        loadCurrentTitle();
    }

    private void loadTitles() {
        titles.clear();
        
        // Add all available titles
        titles.add(new TitleItem("Iron Warrior", "Begin your journey", "iron", true));
        titles.add(new TitleItem("Bronze Fighter", "Reach 100 kills", "bronze", false));
        titles.add(new TitleItem("Silver Striker", "Reach 500 kills", "silver", false));
        titles.add(new TitleItem("Gold Champion", "Reach 1000 kills", "gold", false));
        titles.add(new TitleItem("Diamond Legend", "Reach 5000 kills", "diamond", false));
        titles.add(new TitleItem("Master Chief", "Reach 10000 kills", "master", false));
        titles.add(new TitleItem("Winning Streak", "Win 5 matches in a row", "streak", false));
        titles.add(new TitleItem("Untouchable", "Win without dying", "untouchable", false));
        titles.add(new TitleItem("Damage King", "Deal 50000 total damage", "damage", false));
        titles.add(new TitleItem("Team Player", "Play 100 matches", "team", false));
        titles.add(new TitleItem("Rising Star", "Reach Level 10", "star", false));
        titles.add(new TitleItem("Legendary", "Reach Mythic Rank", "legendary", false));

        // Check which titles user has unlocked
        db.collection("players").document(userId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    long kills = doc.getLong("kills") != null ? doc.getLong("kills") : 0;
                    long wins = doc.getLong("wins") != null ? doc.getLong("wins") : 0;
                    long damage = doc.getLong("damage") != null ? doc.getLong("damage") : 0;
                    long matches = doc.getLong("matches") != null ? doc.getLong("matches") : 0;
                    long level = doc.getLong("level") != null ? doc.getLong("level") : 1;
                    String rank = doc.getString("rank") != null ? doc.getString("rank") : "Iron";

                    // Unlock titles based on stats
                    for (TitleItem title : titles) {
                        switch (title.id) {
                            case "iron": title.unlocked = true; break;
                            case "bronze": title.unlocked = kills >= 100; break;
                            case "silver": title.unlocked = kills >= 500; break;
                            case "gold": title.unlocked = kills >= 1000; break;
                            case "diamond": title.unlocked = kills >= 5000; break;
                            case "master": title.unlocked = kills >= 10000; break;
                            case "streak": title.unlocked = wins >= 5; break;
                            case "damage": title.unlocked = damage >= 50000; break;
                            case "team": title.unlocked = matches >= 100; break;
                            case "star": title.unlocked = level >= 10; break;
                            case "legendary": title.unlocked = "Mythic".equals(rank); break;
                        }
                    }
                }
                rvTitles.setAdapter(new TitlesAdapter());
            });
    }

    private void loadCurrentTitle() {
        db.collection("players").document(userId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    currentTitle = doc.getString("title") != null ? doc.getString("title") : "";
                }
            });
    }

    private void selectTitle(TitleItem title) {
        if (!title.unlocked) {
            Toast.makeText(this, "Title locked! " + title.requirement, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("Select Title")
            .setMessage("Set \"" + title.name + "\" as your title?")
            .setPositiveButton("Select", (d, w) -> {
                Map<String, Object> update = new HashMap<>();
                update.put("title", title.name);
                db.collection("players").document(userId).update(update)
                    .addOnSuccessListener(v -> {
                        currentTitle = title.name;
                        Toast.makeText(this, "Title set: " + title.name, Toast.LENGTH_SHORT).show();
                        rvTitles.getAdapter().notifyDataSetChanged();
                    });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    class TitleItem {
        String name, requirement, id;
        boolean unlocked;

        TitleItem(String name, String requirement, String id, boolean unlocked) {
            this.name = name;
            this.requirement = requirement;
            this.id = id;
            this.unlocked = unlocked;
        }
    }

    class TitlesAdapter extends RecyclerView.Adapter<TitlesAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_title, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            TitleItem title = titles.get(position);
            holder.tvTitleName.setText(title.name);
            holder.tvTitleRequirement.setText(title.requirement);
            
            if (title.unlocked) {
                holder.itemView.setAlpha(1.0f);
                holder.tvStatus.setText(currentTitle.equals(title.name) ? "✓ EQUIPPED" : "UNLOCKED");
                holder.tvStatus.setTextColor(currentTitle.equals(title.name) ? 0xFF11998e : 0xFF667eea);
            } else {
                holder.itemView.setAlpha(0.5f);
                holder.tvStatus.setText("🔒 LOCKED");
                holder.tvStatus.setTextColor(0xFF718096);
            }

            holder.itemView.setOnClickListener(v -> selectTitle(title));
        }

        @Override
        public int getItemCount() { return titles.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvTitleName, tvTitleRequirement, tvStatus;
            VH(View v) {
                super(v);
                tvTitleName = v.findViewById(R.id.tvTitleName);
                tvTitleRequirement = v.findViewById(R.id.tvTitleRequirement);
                tvStatus = v.findViewById(R.id.tvTitleStatus);
            }
        }
    }
}
