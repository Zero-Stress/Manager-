package com.zerostress.manager.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.messaging.FirebaseMessaging
import com.zerostress.manager.models.Announcement
import com.zerostress.manager.models.ChatMessage
import com.zerostress.manager.models.FriendRequest
import com.zerostress.manager.models.MatchLog
import com.zerostress.manager.models.MatchSchedule
import com.zerostress.manager.models.Player
import com.zerostress.manager.models.VoiceChannel

class FirebaseRepository {

    interface OnResultCallback<T> {
        fun onSuccess(result: T?)
        fun onFailure(e: Exception?)
    }

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val playersRef: CollectionReference = db.collection("players")
    private val matchLogsRef: CollectionReference = db.collection("match_logs")
    private val announcementsRef: CollectionReference = db.collection("announcements")
    private val chatRef: CollectionReference = db.collection("chat_messages")
    private val schedulesRef: CollectionReference = db.collection("match_schedules")
    private val friendsRef: CollectionReference = db.collection("friendships")
    private val friendRequestsRef: CollectionReference = db.collection("friend_requests")
    private val achievementsRef: CollectionReference = db.collection("player_achievements")
    private val seasonsRef: CollectionReference = db.collection("seasons")
    private val voiceChannelsRef: CollectionReference = db.collection("voice_channels")

    val currentUserId: String?
        get() = auth.currentUser?.uid

    fun updateFcmToken() {
        val userId = currentUserId ?: return
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                playersRef.document(userId).update("fcmToken", task.result)
            }
        }
    }

    // --- Players ---
    fun createPlayer(player: Player, callback: OnResultCallback<Void>) {
        playersRef.document(player.id).set(player.toMap())
            .addOnSuccessListener { callback.onSuccess(null) }
            .addOnFailureListener { e -> callback.onFailure(e) }
    }

    fun getPlayer(userId: String, callback: OnResultCallback<Player>) {
        playersRef.document(userId).get()
            .addOnSuccessListener { doc -> callback.onSuccess(doc.toObject(Player::class.java)) }
            .addOnFailureListener { e -> callback.onFailure(e) }
    }

    fun updatePlayerStatus(playerId: String, status: String, callback: OnResultCallback<Void>) {
        playersRef.document(playerId).update("status", status)
            .addOnSuccessListener { callback.onSuccess(null) }
            .addOnFailureListener { e -> callback.onFailure(e) }
    }

    fun updatePlayerRole(playerId: String, role: String, callback: OnResultCallback<Void>) {
        playersRef.document(playerId).update("role", role)
            .addOnSuccessListener { callback.onSuccess(null) }
            .addOnFailureListener { e -> callback.onFailure(e) }
    }

    fun resetPlayerPassword(playerId: String, callback: OnResultCallback<Void>) {
        playersRef.document(playerId).update("passwordResetPending", true)
            .addOnSuccessListener { callback.onSuccess(null) }
            .addOnFailureListener { e -> callback.onFailure(e) }
    }

    private inline fun <reified T> snapshotList(
        crossinline register: (Query) -> com.google.firebase.firestore.ListenerRegistration
    ): Unit = Unit

    fun getAllPlayers(callback: OnResultCallback<List<Player>>) {
        playersRef.orderBy("score", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null) {
                    callback.onFailure(e)
                    return@addSnapshotListener
                }
                val list = mutableListOf<Player>()
                for (doc in snap.documents) {
                    doc.toObject(Player::class.java)?.let { list.add(it) }
                }
                callback.onSuccess(list)
            }
    }

    fun getApprovedPlayers(callback: OnResultCallback<List<Player>>) {
        playersRef.whereEqualTo("status", "approved")
            .orderBy("score", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null) {
                    callback.onFailure(e)
                    return@addSnapshotListener
                }
                val list = mutableListOf<Player>()
                for (doc in snap.documents) {
                    doc.toObject(Player::class.java)?.let { list.add(it) }
                }
                callback.onSuccess(list)
            }
    }

    fun getPendingPlayers(callback: OnResultCallback<List<Player>>) {
        playersRef.whereEqualTo("status", "pending")
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null) {
                    callback.onFailure(e)
                    return@addSnapshotListener
                }
                val list = mutableListOf<Player>()
                for (doc in snap.documents) {
                    doc.toObject(Player::class.java)?.let { list.add(it) }
                }
                callback.onSuccess(list)
            }
    }

    // --- Match Logs ---
    fun addMatchLog(log: MatchLog, callback: OnResultCallback<Void>) {
        val id = matchLogsRef.document().id
        log.id = id
        matchLogsRef.document(id).set(log)
            .addOnSuccessListener {
                getPlayer(log.playerId, object : OnResultCallback<Player> {
                    override fun onSuccess(player: Player?) {
                        if (player == null) {
                            callback.onSuccess(null)
                            return
                        }
                        val newKills = player.kills + log.kills
                        val newDamage = player.damage + log.damage
                        val newWins = player.wins + (if (log.win) 1 else 0)
                        val newMatches = player.matches + 1
                        val newScore = Player.calculateScore(newKills, newDamage, newWins)
                        val newRank = Player.getRankTier(newScore)
                        val xpGained = log.kills * 5 + (log.damage / 50).toInt() + (if (log.win) 100 else 20)
                        var newXp = player.xp + xpGained
                        var newLevel = player.level
                        while (newXp >= Player.xpForLevel(newLevel)) {
                            newXp -= Player.xpForLevel(newLevel)
                            newLevel++
                        }
                        val coinsGained = log.kills * 2 + (if (log.win) 25 else 5)

                        val updates = mapOf<String, Any>(
                            "kills" to newKills,
                            "damage" to newDamage,
                            "wins" to newWins,
                            "matches" to newMatches,
                            "score" to newScore,
                            "rank" to newRank,
                            "xp" to newXp,
                            "level" to newLevel,
                            "coins" to player.coins + coinsGained
                        )

                        playersRef.document(log.playerId).update(updates)
                            .addOnSuccessListener { callback.onSuccess(null) }
                            .addOnFailureListener { e -> callback.onFailure(e) }
                    }

                    override fun onFailure(e: Exception?) {
                        callback.onFailure(e)
                    }
                })
            }
            .addOnFailureListener { e -> callback.onFailure(e) }
    }

    fun getPlayerMatchLogs(playerId: String, callback: OnResultCallback<List<MatchLog>>) {
        matchLogsRef.whereEqualTo("playerId", playerId)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null) {
                    callback.onFailure(e)
                    return@addSnapshotListener
                }
                val list = mutableListOf<MatchLog>()
                for (doc in snap.documents) {
                    doc.toObject(MatchLog::class.java)?.let { list.add(it) }
                }
                callback.onSuccess(list)
            }
    }

    fun resetAllPlayerData(callback: OnResultCallback<Void>) {
        playersRef.get().addOnSuccessListener { snap ->
            val batch: WriteBatch = db.batch()
            for (doc in snap.documents) {
                val reset = mapOf<String, Any>(
                    "kills" to 0,
                    "damage" to 0,
                    "wins" to 0,
                    "matches" to 0,
                    "score" to 0L,
                    "rank" to "Iron",
                    "xp" to 0,
                    "level" to 1
                )
                batch.update(doc.reference, reset)
            }
            batch.commit()
                .addOnSuccessListener { callback.onSuccess(null) }
                .addOnFailureListener { e -> callback.onFailure(e) }
        }.addOnFailureListener { e -> callback.onFailure(e) }
    }

    // --- Announcements ---
    fun createAnnouncement(a: Announcement, callback: OnResultCallback<Void>) {
        val id = announcementsRef.document().id
        a.id = id
        announcementsRef.document(id).set(a)
            .addOnSuccessListener { callback.onSuccess(null) }
            .addOnFailureListener { e -> callback.onFailure(e) }
    }

    fun getAnnouncements(callback: OnResultCallback<List<Announcement>>) {
        announcementsRef.orderBy("timestamp", Query.Direction.DESCENDING).limit(50)
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null) {
                    callback.onFailure(e)
                    return@addSnapshotListener
                }
                val list = mutableListOf<Announcement>()
                for (doc in snap.documents) {
                    doc.toObject(Announcement::class.java)?.let { list.add(it) }
                }
                callback.onSuccess(list)
            }
    }

    fun deleteAnnouncement(id: String, callback: OnResultCallback<Void>) {
        announcementsRef.document(id).delete()
            .addOnSuccessListener { callback.onSuccess(null) }
            .addOnFailureListener { e -> callback.onFailure(e) }
    }

    // --- Chat ---
    fun getChatMessages(callback: OnResultCallback<List<ChatMessage>>) {
        chatRef.orderBy("timestamp", Query.Direction.ASCENDING).limit(200)
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null) {
                    callback.onFailure(e)
                    return@addSnapshotListener
                }
                val list = mutableListOf<ChatMessage>()
                for (doc in snap.documents) {
                    val m = doc.toObject(ChatMessage::class.java)
                    if (m != null && !m.deleted) list.add(m)
                }
                callback.onSuccess(list)
            }
    }

    fun sendChatMessage(msg: ChatMessage, callback: OnResultCallback<Void>) {
        val id = chatRef.document().id
        msg.id = id
        chatRef.document(id).set(msg)
            .addOnSuccessListener { callback.onSuccess(null) }
            .addOnFailureListener { e -> callback.onFailure(e) }
    }

    fun deleteChatMessage(messageId: String, callback: OnResultCallback<Void>) {
        chatRef.document(messageId).update("deleted", true)
            .addOnSuccessListener { callback.onSuccess(null) }
            .addOnFailureListener { e -> callback.onFailure(e) }
    }

    fun clearAllChats(callback: OnResultCallback<Void>) {
        chatRef.get().addOnSuccessListener { snap ->
            val batch: WriteBatch = db.batch()
            for (doc in snap.documents) {
                batch.delete(doc.reference)
            }
            batch.commit()
                .addOnSuccessListener { callback.onSuccess(null) }
                .addOnFailureListener { e -> callback.onFailure(e) }
        }.addOnFailureListener { e -> callback.onFailure(e) }
    }

    // --- Schedules ---
    fun createSchedule(s: MatchSchedule, callback: OnResultCallback<Void>) {
        val id = schedulesRef.document().id
        s.id = id
        schedulesRef.document(id).set(s)
            .addOnSuccessListener { callback.onSuccess(null) }
            .addOnFailureListener { e -> callback.onFailure(e) }
    }

    fun getSchedules(callback: OnResultCallback<List<MatchSchedule>>) {
        schedulesRef.orderBy("matchTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null) {
                    callback.onFailure(e)
                    return@addSnapshotListener
                }
                val list = mutableListOf<MatchSchedule>()
                for (doc in snap.documents) {
                    doc.toObject(MatchSchedule::class.java)?.let { list.add(it) }
                }
                callback.onSuccess(list)
            }
    }

    fun deleteSchedule(id: String, callback: OnResultCallback<Void>) {
        schedulesRef.document(id).delete()
            .addOnSuccessListener { callback.onSuccess(null) }
            .addOnFailureListener { e -> callback.onFailure(e) }
    }

    // --- Friends ---
    fun sendFriendRequest(req: FriendRequest, callback: OnResultCallback<Void>) {
        val id = friendRequestsRef.document().id
        req.id = id
        friendRequestsRef.document(id).set(req)
            .addOnSuccessListener { callback.onSuccess(null) }
            .addOnFailureListener { e -> callback.onFailure(e) }
    }

    fun getFriendRequests(userId: String, callback: OnResultCallback<List<FriendRequest>>) {
        friendRequestsRef.whereEqualTo("toUserId", userId).whereEqualTo("status", "pending")
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null) {
                    callback.onFailure(e)
                    return@addSnapshotListener
                }
                val list = mutableListOf<FriendRequest>()
                for (doc in snap.documents) {
                    doc.toObject(FriendRequest::class.java)?.let { list.add(it) }
                }
                callback.onSuccess(list)
            }
    }

    fun respondFriendRequest(request: FriendRequest, accept: Boolean, callback: OnResultCallback<Void>) {
        friendRequestsRef.document(request.id).update("status", if (accept) "accepted" else "rejected")
            .addOnSuccessListener {
                if (accept) {
                    val friendship = mapOf<String, Any>(
                        "userId1" to request.fromUserId,
                        "userId2" to request.toUserId
                    )
                    friendsRef.document().set(friendship)
                        .addOnSuccessListener { callback.onSuccess(null) }
                        .addOnFailureListener { e -> callback.onFailure(e) }
                } else {
                    callback.onSuccess(null)
                }
            }
            .addOnFailureListener { e -> callback.onFailure(e) }
    }

    // --- Achievements ---
    fun getPlayerAchievements(playerId: String, callback: OnResultCallback<List<String>>) {
        achievementsRef.whereEqualTo("playerId", playerId)
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null) {
                    callback.onFailure(e)
                    return@addSnapshotListener
                }
                val ids = mutableListOf<String>()
                for (doc in snap.documents) {
                    doc.getString("achievementId")?.let { ids.add(it) }
                }
                callback.onSuccess(ids)
            }
    }

    fun unlockAchievement(playerId: String, achievementId: String, callback: OnResultCallback<Void>) {
        val docId = "${playerId}_$achievementId"
        achievementsRef.document(docId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    val data = mapOf<String, Any>(
                        "achievementId" to achievementId,
                        "playerId" to playerId
                    )
                    achievementsRef.document(docId).set(data)
                        .addOnSuccessListener { callback.onSuccess(null) }
                        .addOnFailureListener { ex -> callback.onFailure(ex) }
                } else {
                    callback.onSuccess(null)
                }
            }
            .addOnFailureListener { e -> callback.onFailure(e) }
    }

    // --- Voice Channels ---
    fun getVoiceChannels(callback: OnResultCallback<List<VoiceChannel>>) {
        voiceChannelsRef.addSnapshotListener { snap, e ->
            if (e != null || snap == null) {
                callback.onFailure(e)
                return@addSnapshotListener
            }
            val list = mutableListOf<VoiceChannel>()
            for (doc in snap.documents) {
                doc.toObject(VoiceChannel::class.java)?.let { list.add(it) }
            }
            callback.onSuccess(list)
        }
    }

    fun joinVoiceChannel(channelId: String, userId: String, callback: OnResultCallback<Void>) {
        voiceChannelsRef.document(channelId).get()
            .addOnSuccessListener { doc ->
                val ch = doc.toObject(VoiceChannel::class.java)
                if (ch == null) {
                    callback.onFailure(Exception("Channel not found"))
                    return@addOnSuccessListener
                }
                if (ch.participants.size >= ch.maxParticipants) {
                    callback.onFailure(Exception("Channel full"))
                    return@addOnSuccessListener
                }
                val participants = ch.participants.toMutableList()
                if (!participants.contains(userId)) participants.add(userId)
                val updates = mapOf<String, Any>(
                    "participants" to participants,
                    "active" to true
                )
                voiceChannelsRef.document(channelId).update(updates)
                    .addOnSuccessListener { callback.onSuccess(null) }
                    .addOnFailureListener { e -> callback.onFailure(e) }
            }
            .addOnFailureListener { e -> callback.onFailure(e) }
    }

    fun leaveVoiceChannel(channelId: String, userId: String, callback: OnResultCallback<Void>) {
        voiceChannelsRef.document(channelId).get()
            .addOnSuccessListener { doc ->
                val ch = doc.toObject(VoiceChannel::class.java)
                if (ch == null) {
                    callback.onFailure(Exception("Channel not found"))
                    return@addOnSuccessListener
                }
                val participants = ch.participants.toMutableList()
                participants.remove(userId)
                val updates = mapOf<String, Any>(
                    "participants" to participants,
                    "active" to participants.isNotEmpty()
                )
                voiceChannelsRef.document(channelId).update(updates)
                    .addOnSuccessListener { callback.onSuccess(null) }
                    .addOnFailureListener { e -> callback.onFailure(e) }
            }
            .addOnFailureListener { e -> callback.onFailure(e) }
    }

    companion object {
        private const val TAG = "FirebaseRepo"
    }
}
