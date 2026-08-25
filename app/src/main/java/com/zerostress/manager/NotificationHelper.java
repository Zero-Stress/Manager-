package com.zerostress.manager;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to manage notifications and sync FCM tokens with Firestore.
 * Also provides a Firestore listener for real-time leaderboard updates.
 */
public class NotificationHelper {

    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "leaderboard_updates";
    private static final String CHANNEL_NAME = "Leaderboard Updates";

    private final Context context;
    private final SharedPreferences prefs;
    private final FirebaseFirestore db;
    private ListenerRegistration dailyLogListener;
    private ListenerRegistration announcementListener;

    // Callback to notify when leaderboard changes
    public interface LeaderboardUpdateCallback {
        void onLeaderboardChanged(String type, String message);
    }

    private LeaderboardUpdateCallback callback;

    public NotificationHelper(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences("zero_stress_prefs", Context.MODE_PRIVATE);
        this.db = FirebaseFirestore.getInstance();
        createNotificationChannel();
    }

    /**
     * Set callback for leaderboard changes.
     */
    public void setLeaderboardUpdateCallback(LeaderboardUpdateCallback callback) {
        this.callback = callback;
    }

    /**
     * Save FCM token to Firestore under the user's document.
     */
    public void saveTokenToUser(String userPhone, String fcmToken) {
        if (userPhone == null || fcmToken == null) return;

        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("fcmToken", fcmToken);
        tokenData.put("lastTokenUpdate", System.currentTimeMillis());
        tokenData.put("platform", "android");

        db.collection("users").document(userPhone)
                .update(tokenData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token saved to Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save FCM token", e));
    }

    /**
     * Start listening for daily log changes (leaderboard updates).
     */
    public void startLeaderboardListener() {
        // Listen for changes in dailylogs collection
        dailyLogListener = db.collection("dailylogs")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Daily log listener error", error);
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        Log.d(TAG, "Daily logs updated: " + snapshots.size() + " entries");

                        // Notify callback if set
                        if (callback != null) {
                            callback.onLeaderboardChanged(
                                    "daily",
                                    "Daily leaderboard has been updated with " + snapshots.size() + " entries"
                            );
                        }
                    }
                });

        // Listen for announcements
        announcementListener = db.collection("announcements")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Announcement listener error", error);
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        Map<String, Object> latestAnnouncement = snapshots.getDocuments().get(0).getData();
                        if (latestAnnouncement != null) {
                            String message = (String) latestAnnouncement.get("message");
                            if (callback != null) {
                                callback.onLeaderboardChanged(
                                        "announcement",
                                        message != null ? message : "New announcement available"
                                );
                            }
                        }
                    }
                });
    }

    /**
     * Stop listening for updates.
     */
    public void stopLeaderboardListener() {
        if (dailyLogListener != null) {
            dailyLogListener.remove();
            dailyLogListener = null;
        }
        if (announcementListener != null) {
            announcementListener.remove();
            announcementListener = null;
        }
    }

    /**
     * Show a local notification (for testing or in-app triggers).
     */
    public void showNotification(String title, String body, String type) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        if ("daily_leaderboard".equals(type)) {
            intent.putExtra("navigate_to", "dailyLb");
        } else if ("weekly_leaderboard".equals(type)) {
            intent.putExtra("navigate_to", "leaderboard");
        } else if ("announcement".equals(type)) {
            intent.putExtra("navigate_to", "announcements");
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body));

        // Color based on type
        switch (type) {
            case "daily_leaderboard":
                builder.setColor(0xFF38bdf8);
                break;
            case "weekly_leaderboard":
                builder.setColor(0xFF10b981);
                break;
            case "announcement":
                builder.setColor(0xFFf59e0b);
                break;
            default:
                builder.setColor(0xFF38bdf8);
        }

        try {
            NotificationManagerCompat manager = NotificationManagerCompat.from(context);
            int notificationId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
            manager.notify(notificationId, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission not granted", e);
        }
    }

    /**
     * Create notification channel for Android 8.0+.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications when leaderboard changes");
            channel.enableVibration(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Check if notifications are enabled.
     */
    public boolean areNotificationsEnabled() {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /**
     * Get stored FCM token.
     */
    public String getStoredToken() {
        return prefs.getString("fcm_token", null);
    }

    /**
     * Save user phone for token sync.
     */
    public void saveUserPhone(String phone) {
        prefs.edit().putString("user_phone", phone).apply();
    }

    /**
     * Cleanup resources.
     */
    public void cleanup() {
        stopLeaderboardListener();
    }
}
