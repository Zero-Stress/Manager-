package com.zerostress.manager;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to manage notifications.
 * Polls Firestore every 30 seconds for changes and shows local notifications.
 * No Cloud Functions needed!
 */
public class NotificationHelper {

    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "leaderboard_updates";
    private static final String CHANNEL_NAME = "Leaderboard Updates";
    private static final long POLL_INTERVAL_MS = 30000; // 30 seconds

    private final Context context;
    private final SharedPreferences prefs;
    private final FirebaseFirestore db;

    // Polling
    private Handler pollHandler;
    private Runnable pollRunnable;
    private boolean isPolling = false;

    // Track last seen timestamps to detect changes
    private long lastDailyLogTimestamp = 0;
    private long lastAnnouncementTimestamp = 0;
    private boolean isFirstPoll = true;

    // Callback
    public interface LeaderboardUpdateCallback {
        void onLeaderboardChanged(String type, String message);
    }

    private LeaderboardUpdateCallback callback;

    public NotificationHelper(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences("zero_stress_prefs", Context.MODE_PRIVATE);
        this.db = FirebaseFirestore.getInstance();
        this.pollHandler = new Handler(Looper.getMainLooper());
        createNotificationChannel();

        // Restore last known timestamps
        lastDailyLogTimestamp = prefs.getLong("last_daily_timestamp", 0);
        lastAnnouncementTimestamp = prefs.getLong("last_announcement_timestamp", 0);
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
     * Start polling Firestore for changes.
     * Shows notifications when leaderboard or announcements change.
     */
    public void startLeaderboardListener() {
        if (isPolling) return;

        isPolling = true;
        isFirstPoll = true;

        pollRunnable = new Runnable() {
            @Override
            public void run() {
                checkForUpdates();
                pollHandler.postDelayed(this, POLL_INTERVAL_MS);
            }
        };

        pollHandler.post(pollRunnable);
        Log.d(TAG, "Started polling for leaderboard updates");
    }

    /**
     * Check Firestore for new updates and show notifications.
     */
    private void checkForUpdates() {
        // Check daily logs for changes
        db.collection("dailylogs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Long timestamp = queryDocumentSnapshots.getDocuments().get(0)
                                .getLong("timestamp");

                        if (timestamp != null) {
                            if (!isFirstPoll && timestamp > lastDailyLogTimestamp) {
                                // New data added!
                                String playerName = queryDocumentSnapshots.getDocuments().get(0)
                                        .getString("playerName");

                                String message = (playerName != null ? playerName : "A player")
                                        + " added new match data!";

                                showNotification(
                                        "🏆 Daily Leaderboard Updated",
                                        message,
                                        "daily_leaderboard"
                                );

                                if (callback != null) {
                                    callback.onLeaderboardChanged("daily", message);
                                }
                            }
                            lastDailyLogTimestamp = timestamp;
                            prefs.edit().putLong("last_daily_timestamp", timestamp).apply();
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking daily logs", e));

        // Check announcements for new ones
        db.collection("announcements")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Long timestamp = queryDocumentSnapshots.getDocuments().get(0)
                                .getLong("timestamp");

                        if (timestamp != null) {
                            if (!isFirstPoll && timestamp > lastAnnouncementTimestamp) {
                                // New announcement!
                                String message = queryDocumentSnapshots.getDocuments().get(0)
                                        .getString("message");

                                showNotification(
                                        "📢 New Announcement",
                                        message != null ? message : "New announcement available",
                                        "announcement"
                                );

                                if (callback != null) {
                                    callback.onLeaderboardChanged("announcement",
                                            message != null ? message : "New announcement");
                                }
                            }
                            lastAnnouncementTimestamp = timestamp;
                            prefs.edit().putLong("last_announcement_timestamp", timestamp).apply();
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking announcements", e));

        // After first poll, disable "first poll" flag
        if (isFirstPoll) {
            isFirstPoll = false;
        }
    }

    /**
     * Stop listening for updates.
     */
    public void stopLeaderboardListener() {
        if (pollHandler != null && pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
        isPolling = false;
        Log.d(TAG, "Stopped polling for leaderboard updates");
    }

    /**
     * Show a local notification.
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
            Log.d(TAG, "Notification shown: " + title);
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
