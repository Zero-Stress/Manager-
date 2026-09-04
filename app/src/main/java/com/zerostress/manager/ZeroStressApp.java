package com.zerostress.manager;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.AppCheckToken;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

public class ZeroStressApp extends Application {
    private static final String TAG = "ZeroStressApp";
    public static final String CHANNEL_ID = "zs_notifications";
    public static final String CHAT_CHANNEL_ID = "zs_chat";

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        setupAppCheck();
        createNotificationChannels();
    }

    private void setupAppCheck() {
        try {
            FirebaseAppCheck appCheck = FirebaseAppCheck.getInstance();
            appCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
            );
            appCheck.setTokenAutoRefreshEnabled(true);

            appCheck.getToken(false).addOnSuccessListener(result -> {
                String token = result.getToken();
                Log.w(TAG, "APP CHECK DEBUG TOKEN: " + token);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "App Check token failed: " + e.getMessage(), e);
            });
        } catch (Exception e) {
            Log.e(TAG, "App Check setup failed: " + e.getMessage(), e);
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Main notifications channel (match updates, announcements, system)
            NotificationChannel mainChannel = new NotificationChannel(
                    CHANNEL_ID, "ZERO STRESS Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            mainChannel.setDescription("Match updates, announcements, and alerts");
            mainChannel.enableVibration(true);
            mainChannel.setVibrationPattern(new long[]{0, 300, 200, 300});
            mainChannel.setShowBadge(true);

            // Chat notifications channel
            NotificationChannel chatChannel = new NotificationChannel(
                    CHAT_CHANNEL_ID, "Team Chat",
                    NotificationManager.IMPORTANCE_HIGH
            );
            chatChannel.setDescription("Chat messages from your squad");
            chatChannel.enableVibration(true);
            chatChannel.setVibrationPattern(new long[]{0, 100});
            chatChannel.setShowBadge(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(mainChannel);
                manager.createNotificationChannel(chatChannel);
            }
        }
    }
}
