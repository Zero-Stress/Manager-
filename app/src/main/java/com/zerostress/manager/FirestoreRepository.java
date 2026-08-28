package com.zerostress.manager;

import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.zerostress.manager.models.Announcement;
import com.zerostress.manager.models.LeaderboardEntry;
import com.zerostress.manager.models.MatchRecord;
import com.zerostress.manager.models.Player;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreRepository {
    private static final String TAG = "FirestoreRepo";
    private final FirebaseFirestore db;

    // Collection references
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

    public interface AuthCallback {
        void onSuccess(Player player);
        void onFailure(String error);
    }

    public void login(String phone, String password, AuthCallback callback) {
        // Check admin first
        if (phone.equals("1757261781") && password.equals("adminpassword123")) {
            Player admin = new Player("1757261781", "Admin Master", "", "admin", "confirmed");
            callback.onSuccess(admin);
            return;
        }

        String hashed = hashPassword(password);
        usersRef.document(phone).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Player player = doc.toObject(Player.class);
                    if (player != null && player.getPassword().equals(hashed)) {
                        if (!"confirmed".equals(player.getStatus())) {
                            callback.onFailure("Your account is pending admin approval.");
                        } else {
                            callback.onSuccess(player);
                        }
                    } else {
                        callback.onFailure("Invalid phone number or password!");
                    }
                } else {
                    callback.onFailure("Invalid phone number or password!");
                }
            })
            .addOnFailureListener(e -> callback.onFailure("Login failed: " + e.getMessage()));
    }

    public void register(String name, String phone, String password, AuthCallback callback) {
        String hashed = hashPassword(password);
        Player player = new Player(phone, name, hashed, "player", "pending");
        usersRef.document(phone).set(player)
            .addOnSuccessListener(aVoid -> callback.onSuccess(player))
            .addOnFailureListener(e -> callback.onFailure("Registration failed: " + e.getMessage()));
    }

    // ==================== USERS ====================

    public ListenerRegistration listenUsers(UsersCallback callback) {
        return usersRef.addSnapshotListener((snapshots, error) -> {
            if (error != null) { Log.e(TAG, "Users listener error", error); return; }
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

    public void deletePlayer(String phone) {
        usersRef.document(phone).delete();
    }

    public void addPlayer(String name, String phone, String password) {
        String hashed = hashPassword(password);
        Player player = new Player(phone, name, hashed, "player", "confirmed");
        usersRef.document(phone).set(player);
    }

    public void resetPassword(String phone, String newPassword) {
        String hashed = hashPassword(newPassword);
        usersRef.document(phone).update("password", hashed);
    }

    public void updatePresence(String phone, boolean online) {
        Map<String, Object> data = new HashMap<>();
        data.put("online", online);
        data.put("lastSeen", System.currentTimeMillis());
        usersRef.document(phone).update(data);
    }

    // ==================== DAILY LOGS ====================

    public ListenerRegistration listenDailyLogs(DailyLogsCallback callback) {
        return dailyLogsRef.orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) { Log.e(TAG, "DailyLogs listener error", error); return; }
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
            .addOnSuccessListener(doc -> {
                record.setId(doc.getId());
                callback.onSuccess();
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void deleteDailyRecord(String id, OnResultCallback callback) {
        dailyLogsRef.document(id).delete()
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void resetDailyLogs(OnBulkCallback callback) {
        dailyLogsRef.get().addOnSuccessListener(snapshot -> {
            List<MatchRecord> backup = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snapshot) {
                MatchRecord r = doc.toObject(MatchRecord.class);
                if (r != null) backup.add(r);
            }
            // Delete all
            List<String> ids = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snapshot) ids.add(doc.getId());

            int total = ids.size();
            int[] done = {0};
            if (total == 0) { callback.onComplete(backup); return; }

            for (String id : ids) {
                dailyLogsRef.document(id).delete()
                    .addOnSuccessListener(aVoid -> {
                        done[0]++;
                        if (done[0] == total) callback.onComplete(backup);
                    })
                    .addOnFailureListener(e -> {
                        done[0]++;
                        if (done[0] == total) callback.onComplete(backup);
                    });
            }
        });
    }

    public void restoreDailyLogs(List<MatchRecord> backup, OnResultCallback callback) {
        int total = backup.size();
        int[] done = {0};
        if (total == 0) { callback.onSuccess(); return; }

        for (MatchRecord r : backup) {
            String id = r.getId();
            if (id != null && !id.isEmpty()) {
                dailyLogsRef.document(id).set(r)
                    .addOnSuccessListener(aVoid -> {
                        done[0]++;
                        if (done[0] == total) callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        done[0]++;
                        if (done[0] == total) callback.onSuccess();
                    });
            } else {
                dailyLogsRef.add(r)
                    .addOnSuccessListener(aVoid -> {
                        done[0]++;
                        if (done[0] == total) callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        done[0]++;
                        if (done[0] == total) callback.onSuccess();
                    });
            }
        }
    }

    // ==================== LEADERBOARD ====================

    public static List<LeaderboardEntry> calculateLeaderboard(List<MatchRecord> dailyLogs, List<Player> players) {
        Map<String, LeaderboardEntry> aggregated = new HashMap<>();

        for (MatchRecord record : dailyLogs) {
            String name = record.getPlayerName();
            if (!aggregated.containsKey(name)) {
                aggregated.put(name, new LeaderboardEntry());
                aggregated.get(name).setPlayerName(name);
            }
            LeaderboardEntry entry = aggregated.get(name);
            entry.setMatches(entry.getMatches() + record.getMatches());
            entry.setWins(entry.getWins() + record.getWins());
            entry.setKills(entry.getKills() + record.getKills());
            entry.setAssists(entry.getAssists() + record.getAssists());
            entry.setDamage(entry.getDamage() + record.getDamage());
        }

        // Set online status
        Map<String, Player> playerMap = new HashMap<>();
        for (Player p : players) playerMap.put(p.getName(), p);

        List<LeaderboardEntry> list = new ArrayList<>(aggregated.values());
        for (LeaderboardEntry entry : list) {
            int m = entry.getMatches();
            entry.setAvgDamage(m > 0 ? Math.round((float) entry.getDamage() / m) : 0);
            entry.setWinRate(m > 0 ? Math.round((float) entry.getWins() / m * 1000) / 10.0 : 0);
            entry.setAvgKills(m > 0 ? Math.round((float) entry.getKills() / m * 10) / 10.0 : 0);
            entry.setScorePoints(Math.round((entry.getKills() * 10) + (entry.getDamage() / 100f) + (entry.getWins() * 50)));

            Player p = playerMap.get(entry.getPlayerName());
            entry.setOnline(p != null && p.isCurrentlyOnline());
        }

        // Sort by score descending
        list.sort((a, b) -> b.getScorePoints() - a.getScorePoints());
        for (int i = 0; i < list.size(); i++) list.get(i).setRank(i + 1);

        return list;
    }

    // ==================== WEEKLY DATA ====================

    public interface OnWeeklyDataCallback {
        void onLoaded(Map<String, Map<String, Object>> weeklyData);
    }

    public void loadWeeklyData(OnWeeklyDataCallback callback) {
        weeklyDataRef.get()
            .addOnSuccessListener(doc -> {
                if (doc.exists() && doc.contains("data")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Map<String, Object>> data =
                        (Map<String, Map<String, Object>>) doc.get("data");
                    if (data != null) callback.onLoaded(data);
                    else callback.onLoaded(new HashMap<>());
                } else {
                    callback.onLoaded(new HashMap<>());
                }
            })
            .addOnFailureListener(e -> callback.onLoaded(new HashMap<>()));
    }

    public void saveWeeklyData(Map<String, Map<String, Object>> weeklyData) {
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("data", weeklyData);
        wrapper.put("lastUpdated", System.currentTimeMillis());
        weeklyDataRef.set(wrapper, SetOptions.merge());
    }

    public void resetWeeklyData(OnBulkCallback callback) {
        resetDailyLogs(backup -> {
            weeklyDataRef.delete();
            callback.onComplete(backup);
        });
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
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ==================== CALLBACKS ====================

    public interface OnResultCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface OnBulkCallback {
        void onComplete(List<MatchRecord> backup);
    }
}
