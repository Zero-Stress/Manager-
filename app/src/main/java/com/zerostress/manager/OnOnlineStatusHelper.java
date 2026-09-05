package com.zerostress.manager;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class OnOnlineStatusHelper {

    private static final String TAG = "OnOnlineStatusHelper";

    public static void updateOnlineStatus(boolean isOnline) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> updates = new HashMap<>();
        updates.put("isOnline", isOnline);
        if (isOnline) {
            updates.put("lastSeen", System.currentTimeMillis());
        }

        db.collection("players").document(uid).update(updates)
                .addOnSuccessListener(v -> Log.d(TAG, "Online status updated: " + isOnline))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update online status", e));
    }
}
