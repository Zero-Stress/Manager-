package com.zerostress.manager;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
            NotificationChannel mainChannel = new NotificationChannel(
                    CHANNEL_ID, "ZERO STRESS Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            mainChannel.setDescription("Match updates, announcements, and alerts");

            NotificationChannel chatChannel = new NotificationChannel(
                    CHAT_CHANNEL_ID, "Team Chat",
                    NotificationManager.IMPORTANCE_HIGH
            );
            chatChannel.setDescription("Chat messages from your squad");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(mainChannel);
                manager.createNotificationChannel(chatChannel);
            }
        }
    }
}
