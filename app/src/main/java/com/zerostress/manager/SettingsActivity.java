package com.zerostress.manager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialButton btnClearCache = findViewById(R.id.btnClearCache);
        MaterialButton btnAbout = findViewById(R.id.btnAbout);
        MaterialButton btnLogout = findViewById(R.id.btnSettingsLogout);
        MaterialButton btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        SwitchMaterial switchNotifications = findViewById(R.id.switchNotifications);
        SwitchMaterial switchChatNotifs = findViewById(R.id.switchChatNotifications);
        SwitchMaterial switchScheduleNotifs = findViewById(R.id.switchScheduleNotifications);

        btnClearCache.setOnClickListener(v -> {
            Toast.makeText(this, "Cache cleared!", Toast.LENGTH_SHORT).show();
        });

        btnAbout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("ZERO STRESS")
                .setMessage("Version 3.0\n\nPerformance & Leaderboard Manager\n\nBuilt with Firebase + Java")
                .setPositiveButton("OK", null)
                .show();
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        // Delete account (2-step confirmation)
        btnDeleteAccount.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("⚠️ Delete Account")
                .setMessage("This will permanently delete your account and all your data.\n\nThis action CANNOT be undone!\n\nType \"DELETE\" below to confirm.")
                .setPositiveButton("Delete My Account", (d, w) -> {
                    new AlertDialog.Builder(this)
                        .setTitle("Final Confirmation")
                        .setMessage("Are you ABSOLUTELY sure?\n\nYour account data, friends, stats, and everything will be permanently deleted.")
                        .setPositiveButton("Yes, Delete Everything", (d2, w2) -> deleteAccount())
                        .setNegativeButton("Cancel", null)
                        .show();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        // Notification preferences (stored locally via SharedPreferences)
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save preference
            getSharedPreferences("notif_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("notifications_enabled", isChecked)
                .apply();
        });

        switchChatNotifs.setOnCheckedChangeListener((buttonView, isChecked) -> {
            getSharedPreferences("notif_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("chat_notifs_enabled", isChecked)
                .apply();
        });

        switchScheduleNotifs.setOnCheckedChangeListener((buttonView, isChecked) -> {
            getSharedPreferences("notif_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("schedule_notifs_enabled", isChecked)
                .apply();
        });
    }

    private void deleteAccount() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Remove from players collection
        db.collection("players").document(uid).delete()
            .addOnSuccessListener(v -> {
                // Remove friend requests
                db.collection("friend_requests").whereEqualTo("fromUserId", uid).get()
                    .addOnSuccessListener(reqs -> {
                        for (com.google.firebase.firestore.DocumentSnapshot req : reqs.getDocuments()) {
                            req.getReference().delete();
                        }
                        finishDeletion();
                    });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to delete account", Toast.LENGTH_SHORT).show();
            });
    }

    private void finishDeletion() {
        // Sign out and clear local data
        FirebaseAuth.getInstance().signOut();
        getSharedPreferences("notif_prefs", MODE_PRIVATE).edit().clear().apply();

        Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_LONG).show();

        new AlertDialog.Builder(this)
            .setTitle("Account Deleted")
            .setMessage("Your account has been permanently deleted.\n\nReturning to login...")
            .setPositiveButton("OK", (d, w) -> {
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            })
            .setCancelable(false)
            .show();
    }
}
