package com.zerostress.manager;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SendNotificationActivity extends AppCompatActivity {

    private EditText etTitle, etMessage;
    private ProgressBar progressBar;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_notification);

        db = FirebaseFirestore.getInstance();
        etTitle = findViewById(R.id.etNotificationTitle);
        etMessage = findViewById(R.id.etNotificationMessage);
        progressBar = findViewById(R.id.progressBar);
        MaterialButton btnSend = findViewById(R.id.btnSendNotification);
        MaterialButton btnQuickAlert = findViewById(R.id.btnQuickAlert);

        btnSend.setOnClickListener(v -> sendNotification());
        btnQuickAlert.setOnClickListener(v -> showQuickAlertDialog());
    }

    private void sendNotification() {
        String title = etTitle.getText().toString().trim();
        String message = etMessage.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Enter title");
            return;
        }
        if (TextUtils.isEmpty(message)) {
            etMessage.setError("Enter message");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        Map<String, Object> notification = new HashMap<>();
        notification.put("title", title);
        notification.put("message", message);
        notification.put("type", "admin");
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("sentBy", "Admin");

        // Save to Firestore (players listening in-dashboard will see it)
        db.collection("notifications").add(notification)
            .addOnSuccessListener(docRef -> {
                // Also save with ID for tracking
                docRef.update("id", docRef.getId());
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "✅ Notification sent to all players!\n" +
                    "Players will see it in-app and via push.", Toast.LENGTH_LONG).show();
                etTitle.setText("");
                etMessage.setText("");
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void showQuickAlertDialog() {
        String[] quickAlerts = {
            "Match starting in 5 minutes!",
            "Tournament begins now!",
            "Server maintenance in 10 minutes",
            "Double XP event active!",
            "New season coming soon!"
        };

        new AlertDialog.Builder(this)
            .setTitle("Quick Alerts")
            .setItems(quickAlerts, (dialog, which) -> {
                etTitle.setText("📢 Quick Alert");
                etMessage.setText(quickAlerts[which]);
                sendNotification();
            })
            .show();
    }
}
