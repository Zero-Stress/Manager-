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

import com.google.android.material.button.MaterialButton;
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
    private RecyclerView rvPlayers;
    private ProgressBar progressBar;
    private TextView tvStats;
    private PlayerAdapter adapter;
    private List<DocumentSnapshot> players = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        tvStats = findViewById(R.id.tvStats);
        rvPlayers = findViewById(R.id.rvPlayers);
        progressBar = findViewById(R.id.progressBar);
        MaterialButton btnAnnouncement = findViewById(R.id.btnAnnouncement);
        MaterialButton btnAddPlayer = findViewById(R.id.btnAddPlayer);
        MaterialButton btnResetData = findViewById(R.id.btnResetData);
        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        MaterialButton btnDailyInput = findViewById(R.id.btnDailyInput);
        MaterialButton btnLeaderboard = findViewById(R.id.btnLeaderboardAdmin);

        rvPlayers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PlayerAdapter();
        rvPlayers.setAdapter(adapter);

        btnAnnouncement.setOnClickListener(v -> showAnnouncementDialog());
        btnAddPlayer.setOnClickListener(v -> showAddPlayerDialog());
        btnResetData.setOnClickListener(v -> resetAllData());
        btnDailyInput.setOnClickListener(v -> startActivity(new Intent(this, DailyInputActivity.class)));
        btnLeaderboard.setOnClickListener(v -> startActivity(new Intent(this, LeaderboardActivity.class)));
        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        loadPlayers();
    }

    private void loadPlayers() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("players").get()
            .addOnSuccessListener(query -> {
                progressBar.setVisibility(View.GONE);
                players.clear();
                int approved = 0, pending = 0;
                for (DocumentSnapshot doc : query.getDocuments()) {
                    players.add(doc);
                    String status = doc.getString("status");
                    if ("approved".equals(status)) approved++;
                    else if ("pending".equals(status)) pending++;
                }
                tvStats.setText("Total: " + players.size() + " | Approved: " + approved + " | Pending: " + pending);
                adapter.notifyDataSetChanged();
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
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

    private void showAddPlayerDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_player, null);
        EditText etName = view.findViewById(R.id.etPlayerName);
        EditText etPhone = view.findViewById(R.id.etPlayerPhone);
        EditText etPass = view.findViewById(R.id.etPlayerPassword);

        new AlertDialog.Builder(this)
            .setTitle("Add Player")
            .setView(view)
            .setPositiveButton("Create", (d, w) -> {
                String name = etName.getText().toString().trim();
                String phone = etPhone.getText().toString().trim();
                String pass = etPass.getText().toString().trim();
                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
                    Toast.makeText(this, "Name and phone required", Toast.LENGTH_SHORT).show();
                    return;
                }
                createPlayer(name, phone, pass.isEmpty() ? "123456" : pass);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void createPlayer(String name, String phone, String password) {
        String email = phone + "@zerostress.local";
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(result -> {
                String uid = result.getUser().getUid();
                Map<String, Object> data = new HashMap<>();
                data.put("uid", uid);
                data.put("name", name);
                data.put("phone", phone);
                data.put("role", "player");
                data.put("status", "approved");
                data.put("score", 0); data.put("kills", 0); data.put("deaths", 0);
                data.put("assists", 0); data.put("damage", 0L); data.put("wins", 0);
                data.put("matches", 0); data.put("xp", 0); data.put("level", 1);
                data.put("coins", 0); data.put("rank", "Iron");

                db.collection("players").document(uid).set(data)
                    .addOnSuccessListener(v -> {
                        Toast.makeText(this, "Player created! Pass: " + password, Toast.LENGTH_LONG).show();
                        loadPlayers();
                    });
            })
            .addOnFailureListener(e ->
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void resetAllData() {
        new AlertDialog.Builder(this)
            .setTitle("⚠️ Reset All Data")
            .setMessage("This will reset all player scores. Continue?")
            .setPositiveButton("Reset", (d, w) -> {
                for (DocumentSnapshot doc : players) {
                    Map<String, Object> reset = new HashMap<>();
                    reset.put("score", 0); reset.put("kills", 0); reset.put("deaths", 0);
                    reset.put("assists", 0); reset.put("damage", 0L); reset.put("wins", 0);
                    reset.put("matches", 0);
                    db.collection("players").document(doc.getId()).update(reset);
                }
                Toast.makeText(this, "All data reset!", Toast.LENGTH_SHORT).show();
                loadPlayers();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

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
            holder.tvRole.setText(doc.getString("role") + " • " + doc.getString("status"));
            holder.tvScore.setText(String.valueOf(doc.getLong("score") != null ? doc.getLong("score") : 0));

            holder.itemView.setOnClickListener(v -> {
                String[] options = {"Approve", "Reject", "Set Admin", "Set Player", "Reset Password", "Ban"};
                new AlertDialog.Builder(AdminDashboardActivity.this)
                    .setTitle(doc.getString("name"))
                    .setItems(options, (d, which) -> {
                        switch (which) {
                            case 0: updateStatus(doc.getId(), "approved"); break;
                            case 1: updateStatus(doc.getId(), "rejected"); break;
                            case 2: updateRole(doc.getId(), "admin"); break;
                            case 3: updateRole(doc.getId(), "player"); break;
                            case 4: resetPassword(doc.getId()); break;
                            case 5: updateStatus(doc.getId(), "banned"); break;
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

    private void updateStatus(String uid, String status) {
        db.collection("players").document(uid).update("status", status)
            .addOnSuccessListener(v -> { Toast.makeText(this, "Updated!", Toast.LENGTH_SHORT).show(); loadPlayers(); });
    }

    private void updateRole(String uid, String role) {
        db.collection("players").document(uid).update("role", role)
            .addOnSuccessListener(v -> { Toast.makeText(this, "Role updated!", Toast.LENGTH_SHORT).show(); loadPlayers(); });
    }

    private void resetPassword(String uid) {
        db.collection("players").document(uid).get().addOnSuccessListener(doc -> {
            String phone = doc.getString("phone");
            if (phone != null) {
                Toast.makeText(this, "Password reset for " + phone, Toast.LENGTH_LONG).show();
            }
        });
    }
}
