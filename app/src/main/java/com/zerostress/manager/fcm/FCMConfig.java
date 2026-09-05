package com.zerostress.manager.fcm;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class FCMConfig {

    private static final String TAG = "FCMConfig";

    public static void checkFCMConfiguration(Activity activity) {
        Log.d(TAG, "=== FCM Configuration Check ===");

        try {
            FirebaseApp app = FirebaseApp.getInstance();
            Log.d(TAG, "FirebaseApp initialized: " + (app != null));
            Log.d(TAG, "FirebaseApp name: " + (app != null ? app.getName() : "null"));
            Log.d(TAG, "FirebaseApp options: " + (app != null ? app.getOptions() : "null"));
        } catch (Exception e) {
            Log.e(TAG, "FirebaseApp not initialized: " + e.getMessage());
        }

        try {
            String uid = FirebaseAuth.getInstance().getUid();
            Log.d(TAG, "FirebaseAuth current user UID: " + (uid != null ? uid : "null (not logged in)"));
        } catch (Exception e) {
            Log.e(TAG, "FirebaseAuth error: " + e.getMessage());
        }

        try {
            String token = FirebaseMessaging.getInstance().getToken().getResult();
            String tokenPreview = token != null ? token.substring(0, Math.min(20, token.length())) : "null";
            Log.d(TAG, "FCM token: " + (token != null ? "present (" + tokenPreview + "...)" : "null"));
            Log.d(TAG, "FCM token length: " + (token != null ? token.length() : 0));
        } catch (Exception e) {
            Log.e(TAG, "FCM token fetch failed: " + e.getMessage());
        }

        int notificationPermission = activity.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS);
        Log.d(TAG, "POST_NOTIFICATIONS permission: " +
            (notificationPermission == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED"));

        Log.d(TAG, "=== End FCM Configuration Check ===");
    }
}
