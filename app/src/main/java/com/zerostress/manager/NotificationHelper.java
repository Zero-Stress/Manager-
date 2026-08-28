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
 * Polls Firestore every 30 seconds and shows local notifications when data changes.
 * Monitors: daily logs, announcements, chat messages, schedules, tournaments, new players.
 */
public class NotificationHelper {

    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "leaderboard_updates";
    private static final String CHANNEL_NAME = "Zero Stress Updates";
    private static final long POLL_INTERVAL_MS = 30000;

    private final Context context;
    private final SharedPreferences prefs;
    private final FirebaseFirestore db;

    private Handler pollHandler;
    private Runnable pollRunnable;
    private boolean isPolling = false;
    private boolean isFirstPoll = true;

    // Track last seen timestamps to detect changes
    private long lastDailyLogTimestamp = 0;
    private long lastAnnouncementTimestamp = 0;
    private long lastChatTimestamp = 0;
    private long lastScheduleTimestamp = 0;
    private long lastTournamentTimestamp = 0;
    private long lastUserCount = 0;

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

        lastDailyLogTimestamp = prefs.getLong("last_daily_timestamp", 0);
        lastAnnouncementTimestamp = prefs.getLong("last_announcement_timestamp", 0);
        lastChatTimestamp = prefs.getLong("last_chat_timestamp", 0);
        lastScheduleTimestamp = prefs.getLong("last_schedule_timestamp", 0);
        lastTournamentTimestamp = prefs.getLong("last_tournament_timestamp", 0);
        lastUserCount = prefs.getLong("last_user_count", 0);
    }

    public void setLeaderboardUpdateCallback(LeaderboardUpdateCallback callback) {
        this.callback = callback;
    }

    public void saveUserPhone(String phone) {
        prefs.edit().putString("user_phone", phone).apply();
    }

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
        Log.d(TAG, "Started polling for all updates");
    }

    private void checkForUpdates() {
        // 1. Check daily logs
        db.collection("dailylogs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        Long ts = snapshots.getDocuments().get(0).getLong("timestamp");
                        if (ts != null && !isFirstPoll && ts > lastDailyLogTimestamp) {
                            String player = snapshots.getDocuments().get(0).getString("playerName");
                            String msg = (player != null ? player : "A player") + " added new match data!";
                            showNotification("\uD83C\uDFC6 Daily Leaderboard Updated", msg, "daily_leaderboard");
                            if (callback != null) callback.onLeaderboardChanged("daily", msg);
                        }
                        if (ts != null) {
                            lastDailyLogTimestamp = ts;
                            prefs.edit().putLong("last_daily_timestamp", ts).apply();
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking daily logs", e));

        // 2. Check announcements
        db.collection("announcements")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        Long ts = snapshots.getDocuments().get(0).getLong("timestamp");
                        if (ts != null && !isFirstPoll && ts > lastAnnouncementTimestamp) {
                            String msg = snapshots.getDocuments().get(0).getString("message");
                            showNotification("\uD83D\uDCE2 New Announcement", msg != null ? msg : "New announcement", "announcement");
                            if (callback != null) callback.onLeaderboardChanged("announcement", msg != null ? msg : "New announcement");
                        }
                        if (ts != null) {
                            lastAnnouncementTimestamp = ts;
                            prefs.edit().putLong("last_announcement_timestamp", ts).apply();
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking announcements", e));

        // 3. Check chat messages
        db.collection("chat")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        Long ts = snapshots.getDocuments().get(0).getLong("timestamp");
                        String senderPhone = snapshots.getDocuments().get(0).getString("senderPhone");
                        String myPhone = prefs.getString("user_phone", "");

                        if (ts != null && !isFirstPoll && ts > lastChatTimestamp && !senderPhone.equals(myPhone)) {
                            String sender = snapshots.getDocuments().get(0).getString("senderName");
                            String msg = snapshots.getDocuments().get(0).getString("message");
                            showNotification("\uD83D\uDCAC " + (sender != null ? sender : "Someone") + ":",
                                msg != null ? msg : "New message", "chat");
                        }
                        if (ts != null) {
                            lastChatTimestamp = ts;
                            prefs.edit().putLong("last_chat_timestamp", ts).apply();
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking chat", e));

        // 4. Check schedules
        db.collection("schedules")
                .orderBy("scheduledTime", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        Long ts = snapshots.getDocuments().get(0).getLong("createdAt");
                        if (ts != null && !isFirstPoll && ts > lastScheduleTimestamp) {
                            String title = snapshots.getDocuments().get(0).getString("title");
                            showNotification("\uD83D\uDCC5 New Match Scheduled", title != null ? title : "New match!", "schedule");
                        }
                        if (ts != null) {
                            lastScheduleTimestamp = ts;
                            prefs.edit().putLong("last_schedule_timestamp", ts).apply();
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking schedules", e));

        // 5. Check tournaments
        db.collection("tournaments")
                .orderBy("startDate", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        Long ts = snapshots.getDocuments().get(0).getLong("startDate");
                        if (ts != null && !isFirstPoll && ts > lastTournamentTimestamp) {
                            String name = snapshots.getDocuments().get(0).getString("name");
                            showNotification("\uD83C\uDFC6 New Tournament!", name != null ? name : "New tournament created!", "tournament");
                        }
                        if (ts != null) {
                            lastTournamentTimestamp = ts;
                            prefs.edit().putLong("last_tournament_timestamp", ts).apply();
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking tournaments", e));

        // 6. Check new player registrations (admin only)
        db.collection("users")
                .get()
                .addOnSuccessListener(snapshots -> {
                    long count = snapshots.size();
                    if (!isFirstPoll && count > lastUserCount) {
                        long diff = count - lastUserCount;
                        showNotification("\uD83D\uDC64 New Player(s)!", diff + " new player(s) registered!", "registration");
                        if (callback != null) callback.onLeaderboardChanged("registration", diff + " new player(s) registered!");
                    }
                    lastUserCount = count;
                    prefs.edit().putLong("last_user_count", count).apply();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking users", e));

        if (isFirstPoll) {
            isFirstPoll = false;
        }
    }

    public void stopLeaderboardListener() {
        if (pollHandler != null && pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
        isPolling = false;
    }

    public void showNotification(String title, String body, String type) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, (int) System.currentTimeMillis(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

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
            case "daily_leaderboard": builder.setColor(0xFF38bdf8); break;
            case "announcement": builder.setColor(0xFFf59e0b); break;
            case "chat": builder.setColor(0xFF10b981); break;
            case "schedule": builder.setColor(0xFF8b5cf6); break;
            case "tournament": builder.setColor(0xFFef4444); break;
            case "registration": builder.setColor(0xFFec4899); break;
            default: builder.setColor(0xFF38bdf8); break;
        }

        try {
            NotificationManagerCompat manager = NotificationManagerCompat.from(context);
            int notificationId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
            manager.notify(notificationId, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission not granted", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("All Zero Stress updates - matches, chat, tournaments");
            channel.enableVibration(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    public boolean areNotificationsEnabled() {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public void cleanup() {
        stopLeaderboardListener();
    }
}
