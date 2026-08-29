package com.zerostress.manager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.zerostress.manager.models.MatchSchedule;

import java.util.ArrayList;
import java.util.List;

/**
 * Background service that monitors match schedules and auto-launches games
 * before scheduled match time.
 *
 * Checks every 30 seconds for upcoming matches within the alert window.
 */
public class GameLaunchService extends Service {
    private static final String TAG = "GameLaunchService";
    private static final String CHANNEL_ID = "game_launch";
    private static final int NOTIFICATION_ID = 2001;

    private Handler handler;
    private Runnable checkRunnable;
    private ListenerRegistration scheduleListener;
    private final List<MatchSchedule> currentSchedules = new ArrayList<>();
    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!isRunning) {
            isRunning = true;
            startListening();
            startPeriodicCheck();
            startForeground(NOTIFICATION_ID, createNotification("Monitoring match schedules..."));
            Log.d(TAG, "GameLaunchService started");
        }

        return START_STICKY;
    }

    private void startListening() {
        scheduleListener = FirebaseFirestore.getInstance()
            .collection("schedules")
            .addSnapshotListener((snapshots, error) -> {
                if (error != null || snapshots == null) return;

                currentSchedules.clear();
                for (QueryDocumentSnapshot doc : snapshots) {
                    MatchSchedule s = doc.toObject(MatchSchedule.class);
                    if (s != null && "scheduled".equals(s.getStatus())) {
                        currentSchedules.add(s);
                    }
                }
            });
    }

    private void startPeriodicCheck() {
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                checkSchedules();
                handler.postDelayed(this, 30000); // Check every 30 seconds
            }
        };
        handler.post(checkRunnable);
    }

    private void checkSchedules() {
        long now = System.currentTimeMillis();

        for (MatchSchedule schedule : currentSchedules) {
            if (schedule.isGameLaunched()) continue;
            if (schedule.getGameType() == null || schedule.getGameType().isEmpty()) continue;

            long scheduledTime = schedule.getScheduledTime();
            int minutesBefore = schedule.getMinutesBeforeLaunch();
            if (minutesBefore <= 0) minutesBefore = 10; // Default 10 minutes

            long alertTime = scheduledTime - (minutesBefore * 60 * 1000);

            // Check if it's time to launch (within 30 second window)
            if (now >= alertTime && now < scheduledTime) {
                // Check if game is installed
                if (GameLaunchHelper.isGameInstalled(this, schedule.getGameType())) {
                    // Send notification and mark as launched
                    sendGameLaunchNotification(schedule);

                    // Mark as launched in Firestore
                    markGameLaunched(schedule.getId());

                    // Auto-launch the game
                    GameLaunchHelper.launchGame(this, schedule.getGameType());
                } else {
                    sendGameNotInstalledNotification(schedule);
                }
            }
        }
    }

    private void markGameLaunched(String scheduleId) {
        if (scheduleId == null) return;
        FirebaseFirestore.getInstance()
            .collection("schedules").document(scheduleId)
            .update("gameLaunched", true);
    }

    private void sendGameLaunchNotification(MatchSchedule schedule) {
        String gameName = GameLaunchHelper.getDisplayName(schedule.getGameType());
        String emoji = GameLaunchHelper.getGameEmoji(schedule.getGameType());

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(emoji + " Match Starting Soon!")
            .setContentText("Launching " + gameName + " - " + schedule.getTitle())
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText(emoji + " " + schedule.getTitle() + "\nGame: " + gameName + "\nStarting in " + schedule.getMinutesBeforeLaunch() + " minutes!\n\nOpening " + gameName + "..."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, builder.build());
        }

        // Update service notification
        startForeground(NOTIFICATION_ID, createNotification("🎮 Launching " + gameName + "..."));
    }

    private void sendGameNotInstalledNotification(MatchSchedule schedule) {
        String gameName = GameLaunchHelper.getDisplayName(schedule.getGameType());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Game Not Installed")
            .setContentText(gameName + " is not installed!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true);

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID + 1, builder.build());
        }
    }

    private Notification createNotification(String text) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Zero Stress - Game Monitor")
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Game Launch", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for game launch before matches");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (handler != null && checkRunnable != null) {
            handler.removeCallbacks(checkRunnable);
        }
        if (scheduleListener != null) {
            scheduleListener.remove();
        }
        Log.d(TAG, "GameLaunchService stopped");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
