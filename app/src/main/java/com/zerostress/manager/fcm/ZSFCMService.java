package com.zerostress.manager.fcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.zerostress.manager.LoginActivity;
import com.zerostress.manager.R;
import com.zerostress.manager.ZeroStressApp;

import java.util.HashMap;
import java.util.Map;

public class ZSFCMService extends FirebaseMessagingService {

    private static final String TAG = "ZSFCMService";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM token refreshed: " + token);
        saveTokenToFirestore(getApplicationContext());
        subscribeToTopics();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        Log.d(TAG, "FCM message received: " + message.getData());
        Log.d(TAG, "From: " + message.getFrom());
        if (message.getNotification() != null) {
            Log.d(TAG, "Notification payload: " + message.getNotification().getTitle() + " - " + message.getNotification().getBody());
        }

        String title = "ZERO STRESS";
        String body = "";
        String type = "general";

        // Check data payload first (more reliable)
        if (message.getData().size() > 0) {
            title = message.getData().getOrDefault("title", title);
            body = message.getData().getOrDefault("body", body);
            type = message.getData().getOrDefault("type", type);
        }

        // Fall back to notification payload
        if (body.isEmpty() && message.getNotification() != null) {
            title = message.getNotification().getTitle() != null ? message.getNotification().getTitle() : title;
            body = message.getNotification().getBody() != null ? message.getNotification().getBody() : "";
        }

        if (!body.isEmpty()) {
            showNotification(title, body, type);
        }
    }

    public static void saveTokenToFirestore(Context context) {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "FCM token fetch failed", task.getException());
                    return;
                }
                String token = task.getResult();
                if (token == null) return;

                FirebaseAuth auth = FirebaseAuth.getInstance();
                String uidValue = null;
                if (auth.getCurrentUser() != null) {
                    uidValue = auth.getCurrentUser().getUid();
                } else {
                    uidValue = auth.getUid();
                }
                final String uid = uidValue;

                if (uid != null) {
                    Map<String, Object> tokenData = new HashMap<>();
                    tokenData.put("fcmToken", token);
                    tokenData.put("tokenUpdated", System.currentTimeMillis());

                    FirebaseFirestore.getInstance()
                        .collection("players").document(uid)
                        .update(tokenData)
                        .addOnSuccessListener(v -> Log.d(TAG, "FCM token saved for user: " + uid))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to save token: " + e.getMessage()));
                } else {
                    Log.w(TAG, "No user logged in, saving token to SharedPreferences for later");
                    context.getSharedPreferences("zs_fcm", Context.MODE_PRIVATE)
                        .edit().putString("pending_token", token).apply();
                }
            });
    }

    public static void saveTokenToFirestoreWithRetry(Context context) {
        // Retry mechanism: if token save failed before, try again
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "FCM token fetch retry failed", task.getException());
                    return;
                }
                String token = task.getResult();
                if (token == null) return;

                FirebaseAuth auth = FirebaseAuth.getInstance();
                String uid = auth.getUid();
                if (uid != null) {
                    // Force set the token (not update) to ensure it's fresh
                    Map<String, Object> tokenData = new HashMap<>();
                    tokenData.put("fcmToken", token);
                    tokenData.put("tokenUpdated", System.currentTimeMillis());

                    FirebaseFirestore.getInstance()
                        .collection("players").document(uid)
                        .set(tokenData, com.google.firebase.firestore.SetOptions.merge())
                        .addOnSuccessListener(v -> Log.d(TAG, "FCM token re-saved for user: " + uid))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to re-save token: " + e.getMessage()));
                }
            });
    }

    private void subscribeToTopics() {
        // Subscribe to broadcast topic for all players
        FirebaseMessaging.getInstance().subscribeToTopic("all_players")
            .addOnSuccessListener(v -> Log.d(TAG, "Subscribed to all_players topic"))
            .addOnFailureListener(e -> Log.e(TAG, "Topic subscription failed: " + e.getMessage()));
        
        // Subscribe to match updates
        FirebaseMessaging.getInstance().subscribeToTopic("match_updates")
            .addOnSuccessListener(v -> Log.d(TAG, "Subscribed to match_updates topic"));
        
        // Subscribe to announcements
        FirebaseMessaging.getInstance().subscribeToTopic("announcements")
            .addOnSuccessListener(v -> Log.d(TAG, "Subscribed to announcements topic"));
    }

    private void showNotification(String title, String body, String type) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        // Choose channel based on type
        String channelId = ZeroStressApp.CHANNEL_ID;
        if ("chat".equals(type) || "mention".equals(type)) {
            channelId = ZeroStressApp.CHAT_CHANNEL_ID;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }
}
