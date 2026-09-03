package com.zerostress.manager;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminDashboardActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ProgressBar progressBar;
    private TextView tvAdminName, tvTotalPlayers, tvActiveMatches;
    private RecyclerView rvPlayers;
    private PlayerAdapter adapter;
    private List<DocumentSnapshot> players = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        tvAdminName = findViewById(R.id.tvAdminName);
        tvTotalPlayers = findViewById(R.id.tvTotalPlayers);
        tvActiveMatches = findViewById(R.id.tvActiveMatches);
        progressBar = findViewById(R.id.progressBar);
        rvPlayers = findViewById(R.id.rvPlayers);

        if (rvPlayers != null) {
            rvPlayers.setLayoutManager(new LinearLayoutManager(this));
            adapter = new PlayerAdapter();
            rvPlayers.setAdapter(adapter);
        }

        // Navigation buttons
        findViewById(R.id.btnPlayerManagement).setOnClickListener(v -> showPlayerList());
        findViewById(R.id.btnDailyInput).setOnClickListener(v -> startActivity(new Intent(this, DailyInputActivity.class)));
        findViewById(R.id.btnAnnouncements).setOnClickListener(v -> showAnnouncementDialog());
        findViewById(R.id.btnLeaderboard).setOnClickListener(v -> startActivity(new Intent(this, LeaderboardActivity.class)));
        findViewById(R.id.btnChat).setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));
        findViewById(R.id.btnVoice).setOnClickListener(v -> startActivity(new Intent(this, ManageVoiceChannelsActivity.class)));
        findViewById(R.id.btnSeasons).setOnClickListener(v -> startActivity(new Intent(this, ManageSeasonsActivity.class)));
        findViewById(R.id.btnProfile).setOnClickListener(v -> startActivity(new Intent(this, ViewAllPlayersStatsActivity.class)));
        
        // New admin features
        findViewById(R.id.btnNotifications).setOnClickListener(v -> startActivity(new Intent(this, SendNotificationActivity.class)));
        
        findViewById(R.id.btnSettings).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        loadAdminInfo();
        loadPlayers();
    }

    private void loadAdminInfo() {
        String userId = auth.getUid();
        if (userId == null) return;
        
        db.collection("players").document(userId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    tvAdminName.setText(doc.getString("name"));
                }
            });

        db.collection("players").get()
            .addOnSuccessListener(query -> {
                tvTotalPlayers.setText(String.valueOf(query.size()));
            });

        db.collection("match_logs").get()
            .addOnSuccessListener(query -> {
                tvActiveMatches.setText(String.valueOf(query.size()));
            });
    }

    private void loadPlayers() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        db.collection("players").get()
            .addOnSuccessListener(query -> {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                players.clear();
                for (DocumentSnapshot doc : query.getDocuments()) {
                    players.add(doc);
                }
                if (adapter != null) adapter.notifyDataSetChanged();
            })
            .addOnFailureListener(e -> {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void showPlayerList() {
        if (players.isEmpty()) {
            Toast.makeText(this, "Loading players...", Toast.LENGTH_SHORT).show();
            loadPlayers();
        } else {
            if (rvPlayers != null) {
                rvPlayers.setVisibility(rvPlayers.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        }
    }

    private void showAnnouncementDialog() {
        EditText input = new EditText(this);
        input.setHint("Type announcement...");
        input.setMinLines(3);

        new AlertDialog.Builder(this)
            .setTitle("📢 Broadcast Announcement")
            .setView(input)
            .setPositiveButton("Send", (d, w) -> {
                String text = input.getText().toString().trim();
                if (!TextUtils.isEmpty(text)) {
                    Map<String, Object> ann = new HashMap<>();
                    ann.put("text", text);
                    ann.put("author", "Admin");
                    ann.put("timestamp", System.currentTimeMillis());
                    db.collection("announcements").add(ann)
                        .addOnSuccessListener(v -> Toast.makeText(this, "Announcement sent!", Toast.LENGTH_SHORT).show());
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showEditScheduleDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_player, null);
        EditText etMatchTitle = view.findViewById(R.id.etPlayerName);
        etMatchTitle.setHint("Match Title");
        EditText etMatchTime = view.findViewById(R.id.etPlayerPhone);
        etMatchTime.setHint("Date & Time");
        EditText etMatchType = view.findViewById(R.id.etPlayerPassword);
        etMatchType.setHint("Match Type (Ranked/Custom)");

        new AlertDialog.Builder(this)
            .setTitle("📋 Edit Schedule")
            .setView(view)
            .setPositiveButton("Save", (d, w) -> {
                String title = etMatchTitle.getText().toString().trim();
                String time = etMatchTime.getText().toString().trim();
                String type = etMatchType.getText().toString().trim();
                
                if (!TextUtils.isEmpty(title)) {
                    Map<String, Object> schedule = new HashMap<>();
                    schedule.put("title", title);
                    schedule.put("dateTime", time);
                    schedule.put("type", type.isEmpty() ? "Custom" : type);
                    schedule.put("createdAt", System.currentTimeMillis());
                    schedule.put("createdBy", auth.getUid());
                    
                    db.collection("schedules").add(schedule)
                        .addOnSuccessListener(v -> Toast.makeText(this, "Schedule added!", Toast.LENGTH_SHORT).show());
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    // Player Adapter with Edit Name/Role
    class PlayerAdapter extends RecyclerView.Adapter<PlayerAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_player, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            DocumentSnapshot doc = players.get(position);
            holder.tvName.setText(doc.getString("name"));
            String role = doc.getString("role");
            String status = doc.getString("status");
            holder.tvRole.setText((role != null ? role : "player") + " • " + (status != null ? status : "pending"));
            Long score = doc.getLong("score");
            holder.tvScore.setText(String.valueOf(score != null ? score : 0));

            holder.itemView.setOnClickListener(v -> {
                String[] options = {
                    "✏️ Edit Name",
                    "👑 Change Role",
                    "✅ Approve",
                    "❌ Reject",
                    "🚫 Ban",
                    "🗑️ Delete Player"
                };
                new AlertDialog.Builder(AdminDashboardActivity.this)
                    .setTitle(doc.getString("name"))
                    .setItems(options, (d, which) -> {
                        switch (which) {
                            case 0: showEditNameDialog(doc); break;
                            case 1: showChangeRoleDialog(doc); break;
                            case 2: updateStatus(doc.getId(), "approved"); break;
                            case 3: updateStatus(doc.getId(), "rejected"); break;
                            case 4: updateStatus(doc.getId(), "banned"); break;
                            case 5: deletePlayer(doc.getId(), doc.getString("name")); break;
                        }
                    })
                    .show();
            });
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

    // Edit Player Name Dialog
    private void showEditNameDialog(DocumentSnapshot player) {
        EditText input = new EditText(this);
        input.setText(player.getString("name"));
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(this)
            .setTitle("✏️ Edit Player Name")
            .setView(input)
            .setPositiveButton("Save", (d, w) -> {
                String newName = input.getText().toString().trim();
                if (!TextUtils.isEmpty(newName)) {
                    db.collection("players").document(player.getId())
                        .update("name", newName)
                        .addOnSuccessListener(v -> {
                            Toast.makeText(this, "Name updated to: " + newName, Toast.LENGTH_SHORT).show();
                            loadPlayers();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    // Change Role Dialog
    private void showChangeRoleDialog(DocumentSnapshot player) {
        String currentRole = player.getString("role");
        if (currentRole == null) currentRole = "player";

        String[] roles = {"player", "moderator", "admin"};
        int checkedItem = 0;
        for (int i = 0; i < roles.length; i++) {
            if (roles[i].equals(currentRole)) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
            .setTitle("👑 Change Role for " + player.getString("name"))
            .setSingleChoiceItems(roles, checkedItem, (dialog, which) -> {
                String selectedRole = roles[which];
                db.collection("players").document(player.getId())
                    .update("role", selectedRole)
                    .addOnSuccessListener(v -> {
                        Toast.makeText(this, "Role updated to: " + selectedRole, Toast.LENGTH_SHORT).show();
                        loadPlayers();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void updateStatus(String uid, String status) {
        db.collection("players").document(uid).update("status", status)
            .addOnSuccessListener(v -> {
                Toast.makeText(this, "Status updated!", Toast.LENGTH_SHORT).show();
                loadPlayers();
            });
    }

    private void deletePlayer(String uid, String name) {
        new AlertDialog.Builder(this)
            .setTitle("⚠️ Delete Player")
            .setMessage("Are you sure you want to delete '" + name + "'?\n\nThis action cannot be undone!")
            .setPositiveButton("Delete", (d, w) -> {
                db.collection("players").document(uid).delete()
                    .addOnSuccessListener(v -> {
                        Toast.makeText(this, "Player deleted!", Toast.LENGTH_SHORT).show();
                        loadPlayers();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
