package com.zs.admin;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.zs.admin.models.Announcement;
import com.zs.admin.models.MatchRecord;
import com.zs.admin.models.Player;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreRepository {
    private final FirebaseFirestore db;
    private final CollectionReference usersRef;
    private final CollectionReference dailyLogsRef;
    private final CollectionReference announcementsRef;
    private final DocumentReference weeklyDataRef;

    public FirestoreRepository() {
        db = FirebaseFirestore.getInstance();
        usersRef = db.collection("users");
        dailyLogsRef = db.collection("dailylogs");
        announcementsRef = db.collection("announcements");
        weeklyDataRef = db.collection("appState").document("weeklyData");
    }

    // ==================== AUTH ====================

    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return password;
        }
    }

    public void loginAdmin(String phone, String password, AuthCallback callback) {
        if ("1757261781".equals(phone) && "adminpassword123".equals(password)) {
            callback.onSuccess("Admin Master");
        } else {
            callback.onFailure("Invalid admin credentials");
        }
    }

    public interface AuthCallback {
        void onSuccess(String adminName);
        void onFailure(String error);
    }

    // ==================== USERS ====================

    public ListenerRegistration listenUsers(UsersCallback callback) {
        return usersRef.addSnapshotListener((snapshots, error) -> {
            if (error != null) return;
            List<Player> players = new ArrayList<>();
            if (snapshots != null) {
                for (QueryDocumentSnapshot doc : snapshots) {
                    Player p = doc.toObject(Player.class);
                    if (p != null) players.add(p);
                }
            }
            callback.onUsersUpdated(players);
        });
    }

    public interface UsersCallback {
        void onUsersUpdated(List<Player> players);
    }

    public void updatePlayerStatus(String phone, String status) {
        usersRef.document(phone).update("status", status);
    }

    public void addPlayer(String name, String phone, String password) {
        String hashed = hashPassword(password);
        Map<String, Object> data = new HashMap<>();
        data.put("phone", phone);
        data.put("name", name);
        data.put("password", hashed);
        data.put("role", "player");
        data.put("status", "confirmed");
        data.put("online", false);
        data.put("lastSeen", 0);
        data.put("playerRole", "fragger");
        data.put("rewardPoints", 0);
        data.put("totalRewardPoints", 0);
        usersRef.document(phone).set(data);
    }

    public void resetPassword(String phone, String newPassword) {
        String hashed = hashPassword(newPassword);
        usersRef.document(phone).update("password", hashed);
    }

    public void deletePlayer(String phone) {
        usersRef.document(phone).delete();
    }

    // ==================== DAILY LOGS ====================

    public ListenerRegistration listenDailyLogs(DailyLogsCallback callback) {
        return dailyLogsRef.orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) return;
                List<MatchRecord> records = new ArrayList<>();
                if (snapshots != null) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        MatchRecord r = doc.toObject(MatchRecord.class);
                        if (r != null) records.add(r);
                    }
                }
                callback.onDailyLogsUpdated(records);
            });
    }

    public interface DailyLogsCallback {
        void onDailyLogsUpdated(List<MatchRecord> records);
    }

    public void addDailyRecord(MatchRecord record, OnResultCallback callback) {
        dailyLogsRef.add(record)
            .addOnSuccessListener(doc -> { record.setId(doc.getId()); callback.onSuccess(); })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void deleteDailyRecord(String id, OnResultCallback callback) {
        dailyLogsRef.document(id).delete()
            .addOnSuccessListener(a -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void resetDailyLogs(OnResultCallback callback) {
        dailyLogsRef.get().addOnSuccessListener(snapshot -> {
            int total = snapshot.size();
            if (total == 0) { callback.onSuccess(); return; }
            int[] done = {0};
            for (QueryDocumentSnapshot doc : snapshot) {
                doc.getReference().delete()
                    .addOnSuccessListener(a -> { done[0]++; if (done[0] == total) callback.onSuccess(); })
                    .addOnFailureListener(e -> { done[0]++; if (done[0] == total) callback.onSuccess(); });
            }
        });
    }

    // ==================== WEEKLY DATA ====================

    public void loadWeeklyData(OnWeeklyDataCallback callback) {
        weeklyDataRef.get()
            .addOnSuccessListener(doc -> {
                if (doc.exists() && doc.contains("data")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Map<String, Object>> data =
                        (Map<String, Map<String, Object>>) doc.get("data");
                    callback.onLoaded(data != null ? data : new HashMap<>());
                } else {
                    callback.onLoaded(new HashMap<>());
                }
            })
            .addOnFailureListener(e -> callback.onLoaded(new HashMap<>()));
    }

    public interface OnWeeklyDataCallback {
        void onLoaded(Map<String, Map<String, Object>> weeklyData);
    }

    public void saveWeeklyData(Map<String, Map<String, Object>> weeklyData) {
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("data", weeklyData);
        wrapper.put("lastUpdated", System.currentTimeMillis());
        weeklyDataRef.set(wrapper, SetOptions.merge());
    }

    // ==================== ANNOUNCEMENTS ====================

    public ListenerRegistration listenAnnouncements(AnnouncementsCallback callback) {
        return announcementsRef.orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) return;
                List<Announcement> list = new ArrayList<>();
                if (snapshots != null) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Announcement a = doc.toObject(Announcement.class);
                        if (a != null) list.add(a);
                    }
                }
                callback.onAnnouncementsUpdated(list);
            });
    }

    public interface AnnouncementsCallback {
        void onAnnouncementsUpdated(List<Announcement> announcements);
    }

    public void postAnnouncement(String message, OnResultCallback callback) {
        Announcement a = new Announcement(message, System.currentTimeMillis());
        announcementsRef.add(a)
            .addOnSuccessListener(doc -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void deleteAnnouncement(String id, OnResultCallback callback) {
        announcementsRef.document(id).delete()
            .addOnSuccessListener(a -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ==================== CALLBACKS ====================

    public interface OnResultCallback {
        void onSuccess();
        void onFailure(String error);
    }
}
