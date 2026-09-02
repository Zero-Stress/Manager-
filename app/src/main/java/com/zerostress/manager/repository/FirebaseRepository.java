package com.zerostress.manager.repository;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.messaging.FirebaseMessaging;
import com.zerostress.manager.models.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseRepository {
    private static final String TAG = "FirebaseRepo";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private final com.google.firebase.firestore.CollectionReference playersRef = db.collection("players");
    private final com.google.firebase.firestore.CollectionReference matchLogsRef = db.collection("match_logs");
    private final com.google.firebase.firestore.CollectionReference announcementsRef = db.collection("announcements");
    private final com.google.firebase.firestore.CollectionReference chatRef = db.collection("chat_messages");
    private final com.google.firebase.firestore.CollectionReference schedulesRef = db.collection("match_schedules");
    private final com.google.firebase.firestore.CollectionReference friendsRef = db.collection("friendships");
    private final com.google.firebase.firestore.CollectionReference friendRequestsRef = db.collection("friend_requests");
    private final com.google.firebase.firestore.CollectionReference achievementsRef = db.collection("player_achievements");
    private final com.google.firebase.firestore.CollectionReference seasonsRef = db.collection("seasons");
    private final com.google.firebase.firestore.CollectionReference voiceChannelsRef = db.collection("voice_channels");

    public String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    public void updateFcmToken() {
        String userId = getCurrentUserId();
        if (userId == null) return;
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = task.getResult();
                playersRef.document(userId).update("fcmToken", token);
            }
        });
    }

    // --- Players ---
    public void createPlayer(Player player, OnResultCallback<Void> callback) {
        playersRef.document(player.getId()).set(player.toMap())
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    public void getPlayer(String userId, OnResultCallback<Player> callback) {
        playersRef.document(userId).get()
                .addOnSuccessListener(doc -> {
                    Player p = doc.toObject(Player.class);
                    callback.onSuccess(p);
                })
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    public void updatePlayerStatus(String playerId, String status, OnResultCallback<Void> callback) {
        playersRef.document(playerId).update("status", status)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    public void updatePlayerRole(String playerId, String role, OnResultCallback<Void> callback) {
        playersRef.document(playerId).update("role", role)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    public void resetPlayerPassword(String playerId, OnResultCallback<Void> callback) {
        playersRef.document(playerId).update("passwordResetPending", true)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    public void getAllPlayers(OnResultCallback<List<Player>> callback) {
        playersRef.orderBy("score", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) { callback.onFailure(e); return; }
                    List<Player> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Player p = doc.toObject(Player.class);
                        if (p != null) list.add(p);
                    }
                    callback.onSuccess(list);
                });
    }

    public void getApprovedPlayers(OnResultCallback<List<Player>> callback) {
        playersRef.whereEqualTo("status", "approved")
                .orderBy("score", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) { callback.onFailure(e); return; }
                    List<Player> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Player p = doc.toObject(Player.class);
                        if (p != null) list.add(p);
                    }
                    callback.onSuccess(list);
                });
    }

    public void getPendingPlayers(OnResultCallback<List<Player>> callback) {
        playersRef.whereEqualTo("status", "pending")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) { callback.onFailure(e); return; }
                    List<Player> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Player p = doc.toObject(Player.class);
                        if (p != null) list.add(p);
                    }
                    callback.onSuccess(list);
                });
    }

    // --- Match Logs ---
    public void addMatchLog(MatchLog log, OnResultCallback<Void> callback) {
        String id = matchLogsRef.document().getId();
        log.setId(id);
        matchLogsRef.document(id).set(log)
                .addOnSuccessListener(v -> {
                    getPlayer(log.getPlayerId(), new OnResultCallback<Player>() {
                        @Override
                        public void onSuccess(Player player) {
                            if (player == null) { callback.onSuccess(null); return; }
                            int newKills = player.getKills() + log.getKills();
                            int newDamage = (int)(player.getDamage() + log.getDamage());
                            int newWins = player.getWins() + (log.isWin() ? 1 : 0);
                            int newMatches = player.getMatches() + 1;
                            long newScore = Player.calculateScore(newKills, newDamage, newWins);
                            String newRank = Player.getRankTier(newScore);
                            int xpGained = log.getKills() * 5 + (int)(log.getDamage() / 50) + (log.isWin() ? 100 : 20);
                            int newXp = player.getXp() + xpGained;
                            int newLevel = player.getLevel();
                            while (newXp >= Player.xpForLevel(newLevel)) {
                                newXp -= Player.xpForLevel(newLevel);
                                newLevel++;
                            }
                            int coinsGained = log.getKills() * 2 + (log.isWin() ? 25 : 5);

                            Map<String, Object> updates = new HashMap<>();
                            updates.put("kills", newKills);
                            updates.put("damage", newDamage);
                            updates.put("wins", newWins);
                            updates.put("matches", newMatches);
                            updates.put("score", newScore);
                            updates.put("rank", newRank);
                            updates.put("xp", newXp);
                            updates.put("level", newLevel);
                            updates.put("coins", player.getCoins() + coinsGained);

                            playersRef.document(log.getPlayerId()).update(updates)
                                    .addOnSuccessListener(unused -> callback.onSuccess(null))
                                    .addOnFailureListener(e -> callback.onFailure(e));
                        }

                        @Override
                        public void onFailure(Exception e) { callback.onFailure(e); }
                    });
                })
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    public void getPlayerMatchLogs(String playerId, OnResultCallback<List<MatchLog>> callback) {
        matchLogsRef.whereEqualTo("playerId", playerId)
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(100)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) { callback.onFailure(e); return; }
                    List<MatchLog> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        MatchLog m = doc.toObject(MatchLog.class);
                        if (m != null) list.add(m);
                    }
                    callback.onSuccess(list);
                });
    }

    public void resetAllPlayerData(OnResultCallback<Void> callback) {
        playersRef.get().addOnSuccessListener(snap -> {
            WriteBatch batch = db.batch();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Map<String, Object> reset = new HashMap<>();
                reset.put("kills", 0);
                reset.put("damage", 0);
                reset.put("wins", 0);
                reset.put("matches", 0);
                reset.put("score", 0L);
                reset.put("rank", "Iron");
                reset.put("xp", 0);
                reset.put("level", 1);
                batch.update(doc.getReference(), reset);
            }
            batch.commit().addOnSuccessListener(v -> callback.onSuccess(null))
                    .addOnFailureListener(e -> callback.onFailure(e));
        }).addOnFailureListener(e -> callback.onFailure(e));
    }

    // --- Announcements ---
    public void createAnnouncement(Announcement a, OnResultCallback<Void> callback) {
        String id = announcementsRef.document().getId();
        a.setId(id);
        announcementsRef.document(id).set(a)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    public void getAnnouncements(OnResultCallback<List<Announcement>> callback) {
        announcementsRef.orderBy("timestamp", Query.Direction.DESCENDING).limit(50)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) { callback.onFailure(e); return; }
                    List<Announcement> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Announcement a = doc.toObject(Announcement.class);
                        if (a != null) list.add(a);
                    }
                    callback.onSuccess(list);
                });
    }

    public void deleteAnnouncement(String id, OnResultCallback<Void> callback) {
        announcementsRef.document(id).delete()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    // --- Chat ---
    public void getChatMessages(OnResultCallback<List<ChatMessage>> callback) {
        chatRef.orderBy("timestamp", Query.Direction.ASCENDING).limit(200)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) { callback.onFailure(e); return; }
                    List<ChatMessage> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        ChatMessage m = doc.toObject(ChatMessage.class);
                        if (m != null && !m.isDeleted()) list.add(m);
                    }
                    callback.onSuccess(list);
                });
    }

    public void sendChatMessage(ChatMessage msg, OnResultCallback<Void> callback) {
        String id = chatRef.document().getId();
        msg.setId(id);
        chatRef.document(id).set(msg)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    public void deleteChatMessage(String messageId, OnResultCallback<Void> callback) {
        chatRef.document(messageId).update("deleted", true)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    public void clearAllChats(OnResultCallback<Void> callback) {
        chatRef.get().addOnSuccessListener(snap -> {
            WriteBatch batch = db.batch();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                batch.delete(doc.getReference());
            }
            batch.commit().addOnSuccessListener(v -> callback.onSuccess(null))
                    .addOnFailureListener(e -> callback.onFailure(e));
        }).addOnFailureListener(e -> callback.onFailure(e));
    }

    // --- Schedules ---
    public void createSchedule(MatchSchedule s, OnResultCallback<Void> callback) {
        String id = schedulesRef.document().getId();
        s.setId(id);
        schedulesRef.document(id).set(s)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    public void getSchedules(OnResultCallback<List<MatchSchedule>> callback) {
        schedulesRef.orderBy("matchTime", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) { callback.onFailure(e); return; }
                    List<MatchSchedule> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        MatchSchedule s = doc.toObject(MatchSchedule.class);
                        if (s != null) list.add(s);
                    }
                    callback.onSuccess(list);
                });
    }

    public void deleteSchedule(String id, OnResultCallback<Void> callback) {
        schedulesRef.document(id).delete()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    // --- Friends ---
    public void sendFriendRequest(FriendRequest req, OnResultCallback<Void> callback) {
        String id = friendRequestsRef.document().getId();
        req.setId(id);
        friendRequestsRef.document(id).set(req)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    public void getFriendRequests(String userId, OnResultCallback<List<FriendRequest>> callback) {
        friendRequestsRef.whereEqualTo("toUserId", userId).whereEqualTo("status", "pending")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) { callback.onFailure(e); return; }
                    List<FriendRequest> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        FriendRequest r = doc.toObject(FriendRequest.class);
                        if (r != null) list.add(r);
                    }
                    callback.onSuccess(list);
                });
    }

    public void respondFriendRequest(FriendRequest request, boolean accept, OnResultCallback<Void> callback) {
        friendRequestsRef.document(request.getId()).update("status", accept ? "accepted" : "rejected")
                .addOnSuccessListener(v -> {
                    if (accept) {
                        Map<String, Object> friendship = new HashMap<>();
                        friendship.put("userId1", request.getFromUserId());
                        friendship.put("userId2", request.getToUserId());
                        friendsRef.document().set(friendship)
                                .addOnSuccessListener(unused -> callback.onSuccess(null))
                                .addOnFailureListener(e -> callback.onFailure(e));
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    // --- Achievements ---
    public void getPlayerAchievements(String playerId, OnResultCallback<List<String>> callback) {
        achievementsRef.whereEqualTo("playerId", playerId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) { callback.onFailure(e); return; }
                    List<String> ids = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String achId = doc.getString("achievementId");
                        if (achId != null) ids.add(achId);
                    }
                    callback.onSuccess(ids);
                });
    }

    public void unlockAchievement(String playerId, String achievementId, OnResultCallback<Void> callback) {
        String docId = playerId + "_" + achievementId;
        achievementsRef.document(docId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("achievementId", achievementId);
                        data.put("playerId", playerId);
                        achievementsRef.document(docId).set(data)
                                .addOnSuccessListener(v -> callback.onSuccess(null))
                                .addOnFailureListener(ex -> callback.onFailure(ex));
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    // --- Voice Channels ---
    public void getVoiceChannels(OnResultCallback<List<VoiceChannel>> callback) {
        voiceChannelsRef.addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) { callback.onFailure(e); return; }
            List<VoiceChannel> list = new ArrayList<>();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                VoiceChannel c = doc.toObject(VoiceChannel.class);
                if (c != null) list.add(c);
            }
            callback.onSuccess(list);
        });
    }

    public void joinVoiceChannel(String channelId, String userId, OnResultCallback<Void> callback) {
        voiceChannelsRef.document(channelId).get()
                .addOnSuccessListener(doc -> {
                    VoiceChannel ch = doc.toObject(VoiceChannel.class);
                    if (ch == null) { callback.onFailure(new Exception("Channel not found")); return; }
                    if (ch.getParticipants().size() >= ch.getMaxParticipants()) {
                        callback.onFailure(new Exception("Channel full"));
                        return;
                    }
                    List<String> participants = new ArrayList<>(ch.getParticipants());
                    if (!participants.contains(userId)) participants.add(userId);
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("participants", participants);
                    updates.put("active", true);
                    voiceChannelsRef.document(channelId).update(updates)
                            .addOnSuccessListener(v -> callback.onSuccess(null))
                            .addOnFailureListener(e -> callback.onFailure(e));
                })
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    public void leaveVoiceChannel(String channelId, String userId, OnResultCallback<Void> callback) {
        voiceChannelsRef.document(channelId).get()
                .addOnSuccessListener(doc -> {
                    VoiceChannel ch = doc.toObject(VoiceChannel.class);
                    if (ch == null) { callback.onFailure(new Exception("Channel not found")); return; }
                    List<String> participants = new ArrayList<>(ch.getParticipants());
                    participants.remove(userId);
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("participants", participants);
                    updates.put("active", !participants.isEmpty());
                    voiceChannelsRef.document(channelId).update(updates)
                            .addOnSuccessListener(v -> callback.onSuccess(null))
                            .addOnFailureListener(e -> callback.onFailure(e));
                })
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    public interface OnResultCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }
}
