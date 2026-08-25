package com.zerostress.manager;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "leaderboard_updates";
    private static final String CHANNEL_NAME = "Leaderboard Updates";
    private static final String CHANNEL_DESCRIPTION = "Notifications when leaderboard changes";

    /**
     * Called when a new FCM token is generated.
     * Store this token in Firestore tied to the user's phone number.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed FCM token: " + token);

        // Save token to Firestore
        saveTokenToFirestore(token);

        // Also save locally for quick access
        getSharedPreferences("fcm_prefs", MODE_PRIVATE)
                .edit()
                .putString("fcm_token", token)
                .apply();
    }

    /**
     * Called when a message is received from FCM.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());

        // Extract data payload
        Map<String, String> data = remoteMessage.getData();
        String title = data.getOrDefault("title", "Zero Stress Manager");
        String body = data.getOrDefault("body", "Leaderboard has been updated!");
        String type = data.getOrDefault("type", "general");
        int score = Integer.parseInt(data.getOrDefault("score", "0"));

        // Show notification
        showNotification(title, body, type, score);
    }

    /**
     * Display a notification on the device.
     */
    private void showNotification(String title, String body, String type, int score) {
        // Create notification channel for Android 8.0+
        createNotificationChannel();

        // Create intent to open app when notification is tapped
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Add extra data based on notification type
        if ("daily_leaderboard".equals(type)) {
            intent.putExtra("navigate_to", "dailyLb");
        } else if ("weekly_leaderboard".equals(type)) {
            intent.putExtra("navigate_to", "leaderboard");
        } else if ("player_rank_change".equals(type)) {
            intent.putExtra("navigate_to", "myProfile");
            intent.putExtra("new_score", score);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // We'll create this
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body));

        // Add category-specific styling
        if ("daily_leaderboard".equals(type)) {
            builder.setColor(0xFF38bdf8); // Blue accent
        } else if ("weekly_leaderboard".equals(type)) {
            builder.setColor(0xFF10b981); // Green accent
        } else if ("player_rank_change".equals(type)) {
            builder.setColor(0xFFf59e0b); // Gold for rank changes
        }

        // Show notification
        try {
            NotificationManagerCompat manager = NotificationManagerCompat.from(this);
            int notificationId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
            manager.notify(notificationId, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission not granted", e);
        }
    }

    /**
     * Create notification channel for Android 8.0 (API 26) and above.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 250, 250, 250});

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Save FCM token to Firestore for the current user.
     */
    private void saveTokenToFirestore(String token) {
        // Get stored user session
        android.content.SharedPreferences prefs = getSharedPreferences("zero_stress_prefs", MODE_PRIVATE);
        String userPhone = prefs.getString("user_phone", null);
        String userName = prefs.getString("user_name", null);

        if (userPhone != null) {
            // Token will be saved when user logs in via MainActivity
            // Store locally for now
            prefs.edit().putString("pending_fcm_token", token).apply();
            Log.d(TAG, "FCM token saved locally, will sync on next login");
        }
    }

    /**
     * Static method to get the current FCM token.
     */
    public static void retrieveToken(Context context) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    Log.d(TAG, "FCM Token: " + token);

                    // Save token locally
                    context.getSharedPreferences("fcm_prefs", MODE_PRIVATE)
                            .edit()
                            .putString("fcm_token", token)
                            .apply();
                });
    }
}
