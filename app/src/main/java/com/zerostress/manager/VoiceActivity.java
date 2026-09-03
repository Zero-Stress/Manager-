package com.zerostress.manager;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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

public class VoiceActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private TextView tvStatus, tvAdminPanel;
    private ProgressBar progressBar;
    private MaterialButton btnJoinLeave, btnMute, btnManagePermissions;
    private FirebaseFirestore db;
    private String userId, userName;
    private boolean isInVoice = false;
    private boolean isMuted = false;
    private boolean isAdmin = false;
    private boolean hasPermission = false;
    private String currentChannelId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        tvStatus = findViewById(R.id.tvVoiceStatus);
        tvAdminPanel = findViewById(R.id.tvAdminPanel);
        progressBar = findViewById(R.id.progressBar);
        btnJoinLeave = findViewById(R.id.btnJoinLeave);
        btnMute = findViewById(R.id.btnMute);
        btnManagePermissions = findViewById(R.id.btnManagePermissions);

        // Check if user is admin
        checkUserRole();

        btnJoinLeave.setOnClickListener(v -> {
            if (isInVoice) {
                leaveVoice();
            } else {
                checkPermissionsAndJoin();
            }
        });

        btnMute.setOnClickListener(v -> toggleMute());

        btnManagePermissions.setOnClickListener(v -> showManagePermissionsDialog());
    }

    private void checkUserRole() {
        db.collection("players").document(userId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    userName = doc.getString("name");
                    if (userName == null) userName = "Unknown";
                    
                    String role = doc.getString("role");
                    isAdmin = "admin".equals(role);
                    
                    if (isAdmin) {
                        tvAdminPanel.setVisibility(View.VISIBLE);
                        btnManagePermissions.setVisibility(View.VISIBLE);
                    } else {
                        tvAdminPanel.setVisibility(View.GONE);
                        btnManagePermissions.setVisibility(View.GONE);
                        checkPlayerPermission();
                    }
                }
            });
    }

    private void checkPlayerPermission() {
        db.collection("voice_permissions").document(userId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Boolean allowed = doc.getBoolean("allowed");
                    hasPermission = allowed != null && allowed;
                } else {
                    hasPermission = false;
                }
                updateUIBasedOnPermission();
            });
    }

    private void updateUIBasedOnPermission() {
        if (!isAdmin && !hasPermission) {
            btnJoinLeave.setEnabled(false);
            btnJoinLeave.setText("🔒 NO PERMISSION");
            btnJoinLeave.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.text_muted));
            tvStatus.setText("🔴 Ask admin to grant voice access");
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        } else {
            btnJoinLeave.setEnabled(true);
            btnJoinLeave.setText("🎙️ Join Voice");
            btnJoinLeave.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.success));
        }
    }

    private void checkPermissionsAndJoin() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
        } else {
            joinVoiceChannel();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            joinVoiceChannel();
        } else {
            Toast.makeText(this, "Microphone permission required for voice chat", Toast.LENGTH_SHORT).show();
        }
    }

    private void joinVoiceChannel() {
        String channelId = "general_voice";
        currentChannelId = channelId;

        Map<String, Object> participant = new HashMap<>();
        participant.put("userId", userId);
        participant.put("userName", userName);
        participant.put("joinedAt", System.currentTimeMillis());
        participant.put("muted", false);
        participant.put("isAdmin", isAdmin);

        db.collection("voice_channels").document(channelId)
            .collection("participants").document(userId)
            .set(participant)
            .addOnSuccessListener(v -> {
                isInVoice = true;
                isMuted = false;
                btnJoinLeave.setText("🔴 Leave Voice");
                btnJoinLeave.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.danger));
                btnMute.setVisibility(View.VISIBLE);
                btnMute.setText("🔇 Mute");
                tvStatus.setText("🟢 Connected to Voice Channel");
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.success));
                Toast.makeText(this, "Joined voice channel!", Toast.LENGTH_SHORT).show();

                listenToParticipants(channelId);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to join: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void listenToParticipants(String channelId) {
        db.collection("voice_channels").document(channelId)
            .collection("participants")
            .addSnapshotListener((snapshots, e) -> {
                if (e != null || snapshots == null) return;
                int count = snapshots.size();
                tvStatus.setText("🟢 Connected — " + count + " participant" + (count != 1 ? "s" : ""));
            });
    }

    private void toggleMute() {
        isMuted = !isMuted;
        btnMute.setText(isMuted ? "🔊 Unmute" : "🔇 Mute");
        
        if (currentChannelId != null) {
            db.collection("voice_channels").document(currentChannelId)
                .collection("participants").document(userId)
                .update("muted", isMuted);
        }
    }

    private void showManagePermissionsDialog() {
        if (!isAdmin) {
            Toast.makeText(this, "Only admins can manage permissions", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get all players
        db.collection("players").get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<DocumentSnapshot> players = new ArrayList<>();
                List<String> playerNames = new ArrayList<>();
                List<Boolean> playerPermissions = new ArrayList<>();
                
                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    String playerId = doc.getId();
                    String playerName = doc.getString("name");
                    String role = doc.getString("role");
                    
                    // Skip admins
                    if ("admin".equals(role)) continue;
                    
                    players.add(doc);
                    playerNames.add(playerName != null ? playerName : "Unknown");
                    
                    // Check current permission
                    String finalPlayerId = playerId;
                    db.collection("voice_permissions").document(playerId).get()
                        .addOnSuccessListener(permDoc -> {
                            Boolean allowed = permDoc.getBoolean("allowed");
                            playerPermissions.add(allowed != null && allowed);
                            
                            if (playerPermissions.size() == players.size()) {
                                showPermissionSelectionDialog(playerNames, players, playerPermissions);
                            }
                        });
                }
                
                if (players.isEmpty()) {
                    Toast.makeText(this, "No players found", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void showPermissionSelectionDialog(List<String> playerNames, List<DocumentSnapshot> players, List<Boolean> permissions) {
        boolean[] checkedItems = new boolean[permissions.size()];
        for (int i = 0; i < permissions.size(); i++) {
            checkedItems[i] = permissions.get(i);
        }

        String[] nameArray = playerNames.toArray(new String[0]);

        new AlertDialog.Builder(this)
            .setTitle("Voice Chat Permissions")
            .setMessage("Select players who can join voice chat:")
            .setMultiChoiceItems(nameArray, checkedItems, (dialog, which, isChecked) -> {
                checkedItems[which] = isChecked;
            })
            .setPositiveButton("Save", (dialog, which) -> {
                for (int i = 0; i < players.size(); i++) {
                    String playerId = players.get(i).getId();
                    boolean allowed = checkedItems[i];
                    
                    Map<String, Object> permission = new HashMap<>();
                    permission.put("allowed", allowed);
                    permission.put("playerId", playerId);
                    permission.put("updatedAt", System.currentTimeMillis());
                    permission.put("grantedBy", userId);
                    
                    db.collection("voice_permissions").document(playerId)
                        .set(permission);
                }
                Toast.makeText(this, "Permissions updated!", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void leaveVoice() {
        if (currentChannelId != null) {
            db.collection("voice_channels").document(currentChannelId)
                .collection("participants").document(userId)
                .delete();
        }

        isInVoice = false;
        isMuted = false;
        currentChannelId = null;
        btnJoinLeave.setText("🎙️ Join Voice");
        btnJoinLeave.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.accent));
        btnMute.setVisibility(View.GONE);
        tvStatus.setText("🔴 Not connected");
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isInVoice) leaveVoice();
    }
}
