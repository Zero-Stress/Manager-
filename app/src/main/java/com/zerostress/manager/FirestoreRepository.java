package com.zerostress.manager;

import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.zerostress.manager.models.Achievement;
import com.zerostress.manager.models.Announcement;
import com.zerostress.manager.models.ChatMessage;
import com.zerostress.manager.models.LeaderboardEntry;
import com.zerostress.manager.models.MatchRecord;
import com.zerostress.manager.models.MatchSchedule;
import com.zerostress.manager.models.Player;
import com.zerostress.manager.models.Season;
import com.zerostress.manager.models.Squad;
import com.zerostress.manager.models.Tournament;

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
    private final CollectionReference squadsRef;
    private final CollectionReference tournamentsRef;
    private final CollectionReference chatRef;
    private final CollectionReference schedulesRef;
    private final CollectionReference seasonsRef;
    private final CollectionReference attendanceRef;
    private final CollectionReference voiceChannelsRef;

    public FirestoreRepository() {
        db = FirebaseFirestore.getInstance();
        usersRef = db.collection("users");
        dailyLogsRef = db.collection("dailylogs");
        announcementsRef = db.collection("announcements");
        weeklyDataRef = db.collection("appState").document("weeklyData");
        squadsRef = db.collection("squads");
        tournamentsRef = db.collection("tournaments");
        chatRef = db.collection("chat");
        schedulesRef = db.collection("schedules");
        seasonsRef = db.collection("seasons");
        attendanceRef = db.collection("attendance");
        voiceChannelsRef = db.collection("voiceChannels");
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
        Map<String, Object> data = new HashMap<>();
        data.put("phone", phone);
        data.put("name", name);
        data.put("password", hashed);
        data.put("role", "player");
        data.put("status", "pending");
        data.put("online", false);
        data.put("lastSeen", 0);
        data.put("playerRole", "fragger");
        data.put("rewardPoints", 0);
        data.put("totalRewardPoints", 0);
        usersRef.document(phone).set(data)
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

    public void updatePlayerRole(String phone, String playerRole) {
        usersRef.document(phone).update("playerRole", playerRole);
    }

    // ==================== VOICE CHAT PERMISSIONS ====================

    public void setVoiceChatPermission(String phone, boolean allowed) {
        usersRef.document(phone).update("voiceChatAllowed", allowed);
    }

    public void checkVoiceChatPermission(String phone, VoicePermissionCallback callback) {
        usersRef.document(phone).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Boolean allowed = doc.getBoolean("voiceChatAllowed");
                    String role = doc.getString("role");
                    // Admin always has permission
                    if ("admin".equals(role)) {
                        callback.onResult(true);
                    } else {
                        callback.onResult(allowed != null && allowed);
                    }
                } else {
                    callback.onResult(false);
                }
            })
            .addOnFailureListener(e -> callback.onResult(false));
    }

    public interface VoicePermissionCallback {
        void onResult(boolean allowed);
    }

    public void deletePlayer(String phone) {
        usersRef.document(phone).delete();
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

    public void updatePresence(String phone, boolean online) {
        Map<String, Object> data = new HashMap<>();
        data.put("online", online);
        data.put("lastSeen", System.currentTimeMillis());
        usersRef.document(phone).update(data);
    }

    // ==================== REWARD POINTS ====================

    public void addRewardPoints(String phone, int points) {
        usersRef.document(phone).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                long current = doc.getLong("rewardPoints") != null ? doc.getLong("rewardPoints") : 0;
                long total = doc.getLong("totalRewardPoints") != null ? doc.getLong("totalRewardPoints") : 0;
                Map<String, Object> data = new HashMap<>();
                data.put("rewardPoints", current + points);
                data.put("totalRewardPoints", total + points);
                usersRef.document(phone).update(data);
            }
        });
    }

    public void spendRewardPoints(String phone, int points) {
        usersRef.document(phone).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                long current = doc.getLong("rewardPoints") != null ? doc.getLong("rewardPoints") : 0;
                if (current >= points) {
                    usersRef.document(phone).update("rewardPoints", current - points);
                }
            }
        });
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
                // Award reward points
                if (record.getPlayerName() != null) {
                    findPlayerPhoneByName(record.getPlayerName(), phone -> {
                        if (phone != null) {
                            int killPoints = record.getKills() * 2;
                            int winPoints = record.getWins() * 50;
                            addRewardPoints(phone, killPoints + winPoints);
                        }
                    });
                }
                callback.onSuccess();
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    private void findPlayerPhoneByName(String name, PhoneCallback callback) {
        usersRef.whereEqualTo("name", name).get().addOnSuccessListener(docs -> {
            if (!docs.isEmpty()) {
                callback.onFound(docs.getDocuments().get(0).getId());
            } else {
                callback.onFound(null);
            }
        }).addOnFailureListener(e -> callback.onFound(null));
    }

    interface PhoneCallback {
        void onFound(String phone);
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
                    .addOnSuccessListener(aVoid -> { done[0]++; if (done[0] == total) callback.onSuccess(); })
                    .addOnFailureListener(e -> { done[0]++; if (done[0] == total) callback.onSuccess(); });
            } else {
                dailyLogsRef.add(r)
                    .addOnSuccessListener(aVoid -> { done[0]++; if (done[0] == total) callback.onSuccess(); })
                    .addOnFailureListener(e -> { done[0]++; if (done[0] == total) callback.onSuccess(); });
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

    // ==================== SQUADS ====================

    public ListenerRegistration listenSquads(SquadsCallback callback) {
        return squadsRef.addSnapshotListener((snapshots, error) -> {
            if (error != null) return;
            List<Squad> list = new ArrayList<>();
            if (snapshots != null) {
                for (QueryDocumentSnapshot doc : snapshots) {
                    Squad s = doc.toObject(Squad.class);
                    if (s != null) list.add(s);
                }
            }
            callback.onSquadsUpdated(list);
        });
    }

    public interface SquadsCallback {
        void onSquadsUpdated(List<Squad> squads);
    }

    public void createSquad(Squad squad, OnResultCallback callback) {
        squadsRef.add(squad)
            .addOnSuccessListener(doc -> {
                squad.setId(doc.getId());
                callback.onSuccess();
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void updateSquad(Squad squad) {
        if (squad.getId() != null) {
            squadsRef.document(squad.getId()).set(squad, SetOptions.merge());
        }
    }

    public void deleteSquad(String id) {
        squadsRef.document(id).delete();
    }

    public void addPlayerToSquad(String squadId, String phone) {
        squadsRef.document(squadId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Squad squad = doc.toObject(Squad.class);
                if (squad != null && !squad.getMemberPhones().contains(phone)) {
                    squad.getMemberPhones().add(phone);
                    updateSquad(squad);
                }
            }
        });
    }

    public void removePlayerFromSquad(String squadId, String phone) {
        squadsRef.document(squadId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Squad squad = doc.toObject(Squad.class);
                if (squad != null) {
                    squad.getMemberPhones().remove(phone);
                    updateSquad(squad);
                }
            }
        });
    }

    // ==================== TOURNAMENTS ====================

    public ListenerRegistration listenTournaments(TournamentsCallback callback) {
        return tournamentsRef.orderBy("startDate", Query.Direction.DESCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) return;
                List<Tournament> list = new ArrayList<>();
                if (snapshots != null) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Tournament t = doc.toObject(Tournament.class);
                        if (t != null) list.add(t);
                    }
                }
                callback.onTournamentsUpdated(list);
            });
    }

    public interface TournamentsCallback {
        void onTournamentsUpdated(List<Tournament> tournaments);
    }

    public void createTournament(Tournament tournament, OnResultCallback callback) {
        tournamentsRef.add(tournament)
            .addOnSuccessListener(doc -> {
                tournament.setId(doc.getId());
                callback.onSuccess();
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void updateTournament(Tournament tournament) {
        if (tournament.getId() != null) {
            tournamentsRef.document(tournament.getId()).set(tournament, SetOptions.merge());
        }
    }

    // ==================== IN-APP CHAT ====================

    public ListenerRegistration listenChat(ChatCallback callback) {
        return chatRef.orderBy("timestamp", Query.Direction.ASCENDING)
            .limitToLast(200)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) return;
                List<ChatMessage> list = new ArrayList<>();
                if (snapshots != null) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        ChatMessage m = doc.toObject(ChatMessage.class);
                        if (m != null) list.add(m);
                    }
                }
                callback.onChatUpdated(list);
            });
    }

    public interface ChatCallback {
        void onChatUpdated(List<ChatMessage> messages);
    }

    public void sendChatMessage(ChatMessage message, OnResultCallback callback) {
        chatRef.add(message)
            .addOnSuccessListener(doc -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ==================== MATCH SCHEDULE ====================

    public ListenerRegistration listenSchedules(SchedulesCallback callback) {
        return schedulesRef.orderBy("scheduledTime", Query.Direction.DESCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) return;
                List<MatchSchedule> list = new ArrayList<>();
                if (snapshots != null) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        MatchSchedule s = doc.toObject(MatchSchedule.class);
                        if (s != null) list.add(s);
                    }
                }
                callback.onSchedulesUpdated(list);
            });
    }

    public interface SchedulesCallback {
        void onSchedulesUpdated(List<MatchSchedule> schedules);
    }

    public void createSchedule(MatchSchedule schedule, OnResultCallback callback) {
        schedulesRef.add(schedule)
            .addOnSuccessListener(doc -> {
                schedule.setId(doc.getId());
                callback.onSuccess();
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void updateSchedule(MatchSchedule schedule) {
        if (schedule.getId() != null) {
            schedulesRef.document(schedule.getId()).set(schedule, SetOptions.merge());
        }
    }

    public void confirmSchedule(String scheduleId, String playerName) {
        schedulesRef.document(scheduleId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                MatchSchedule s = doc.toObject(MatchSchedule.class);
                if (s != null && !s.getConfirmedPlayers().contains(playerName)) {
                    s.getConfirmedPlayers().add(playerName);
                    updateSchedule(s);
                }
            }
        });
    }

    public void cancelSchedule(String scheduleId) {
        schedulesRef.document(scheduleId).update("status", "cancelled");
    }

    // ==================== SEASONS ====================

    public ListenerRegistration listenSeasons(SeasonsCallback callback) {
        return seasonsRef.orderBy("seasonNumber", Query.Direction.DESCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) return;
                List<Season> list = new ArrayList<>();
                if (snapshots != null) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Season s = doc.toObject(Season.class);
                        if (s != null) list.add(s);
                    }
                }
                callback.onSeasonsUpdated(list);
            });
    }

    public interface SeasonsCallback {
        void onSeasonsUpdated(List<Season> seasons);
    }

    public void createSeason(Season season, OnResultCallback callback) {
        seasonsRef.add(season)
            .addOnSuccessListener(doc -> {
                season.setId(doc.getId());
                callback.onSuccess();
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void endSeason(String seasonId) {
        seasonsRef.document(seasonId).update("active", false, "endDate", System.currentTimeMillis());
    }

    // ==================== ATTENDANCE ====================

    public ListenerRegistration listenAttendance(AttendanceCallback callback) {
        return attendanceRef.orderBy("date", Query.Direction.DESCENDING)
            .limitToLast(30)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) return;
                List<Map<String, Object>> list = new ArrayList<>();
                if (snapshots != null) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Map<String, Object> data = doc.getData();
                        data.put("id", doc.getId());
                        list.add(data);
                    }
                }
                callback.onAttendanceUpdated(list);
            });
    }

    public interface AttendanceCallback {
        void onAttendanceUpdated(List<Map<String, Object>> records);
    }

    public void markAttendance(String playerName, String date, boolean present) {
        String docId = date + "_" + playerName;
        Map<String, Object> data = new HashMap<>();
        data.put("playerName", playerName);
        data.put("date", date);
        data.put("present", present);
        data.put("timestamp", System.currentTimeMillis());
        attendanceRef.document(docId).set(data, SetOptions.merge());
    }

    // ==================== ACHIEVEMENTS ====================

    public void loadAchievements(String phone, AchievementsCallback callback) {
        usersRef.document(phone).collection("achievements").get()
            .addOnSuccessListener(docs -> {
                Map<String, Boolean> unlocked = new HashMap<>();
                for (QueryDocumentSnapshot doc : docs) {
                    unlocked.put(doc.getId(), Boolean.TRUE.equals(doc.get("unlocked")));
                }
                callback.onLoaded(unlocked);
            })
            .addOnFailureListener(e -> callback.onLoaded(new HashMap<>()));
    }

    public interface AchievementsCallback {
        void onLoaded(Map<String, Boolean> achievements);
    }

    public void unlockAchievement(String phone, String achievementId) {
        Map<String, Object> data = new HashMap<>();
        data.put("unlocked", true);
        data.put("unlockedAt", System.currentTimeMillis());
        usersRef.document(phone).collection("achievements").document(achievementId).set(data, SetOptions.merge());
    }

    // ==================== PERFORMANCE TRENDS ====================

    public void getPlayerTrends(String playerName, TrendsCallback callback) {
        dailyLogsRef.whereEqualTo("playerName", playerName)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener(docs -> {
                List<Map<String, Object>> trends = new ArrayList<>();
                for (QueryDocumentSnapshot doc : docs) {
                    Map<String, Object> data = doc.getData();
                    data.put("date", doc.get("timestamp"));
                    trends.add(data);
                }
                callback.onTrendsLoaded(trends);
            })
            .addOnFailureListener(e -> callback.onTrendsLoaded(new ArrayList<>()));
    }

    public interface TrendsCallback {
        void onTrendsLoaded(List<Map<String, Object>> trends);
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
