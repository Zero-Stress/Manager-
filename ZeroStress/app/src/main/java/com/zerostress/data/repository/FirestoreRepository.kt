package com.zerostress.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.zerostress.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersRef = db.collection("users")
    private val dailyLogsRef = db.collection("dailylogs")
    private val announcementsRef = db.collection("announcements")
    private val chatRef = db.collection("chat")
    private val settingsRef = db.collection("appSettings")

    // ==================== AUTH ====================

    suspend fun login(phone: String, password: String): Player? {
        val snapshot = usersRef.document(phone).get().await()
        if (!snapshot.exists()) return null
        val player = snapshot.toObject(Player::class.java) ?: return null
        return if (player.password == password) {
            usersRef.document(phone).update("isOnline", true, "lastSeen", System.currentTimeMillis()).await()
            player
        } else null
    }

    suspend fun register(phone: String, name: String, password: String, role: String = "player"): Result<Player> {
        return try {
            val existing = usersRef.document(phone).get().await()
            if (existing.exists()) return Result.failure(Exception("Phone number already registered"))
            val player = Player(
                name = name,
                phone = phone,
                password = password,
                role = role,
                status = if (role == "admin") "confirmed" else "pending",
                createdAt = System.currentTimeMillis()
            )
            usersRef.document(phone).set(player).await()
            Result.success(player)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(phone: String) {
        try { usersRef.document(phone).update("isOnline", false, "lastSeen", System.currentTimeMillis()).await() } catch (_: Exception) {}
    }

    // ==================== USERS ====================

    fun listenUsers(): Flow<List<Player>> = callbackFlow {
        val reg = usersRef.addSnapshotListener { snap, _ ->
            val players = snap?.documents?.mapNotNull { it.toObject(Player::class.java) } ?: emptyList()
            trySend(players)
        }
        awaitClose { reg.remove() }
    }

    suspend fun updateUserStatus(phone: String, status: String) {
        usersRef.document(phone).update("status", status).await()
    }

    suspend fun updateUserRole(phone: String, role: String) {
        usersRef.document(phone).update("role", role).await()
    }

    suspend fun deleteUser(phone: String) {
        usersRef.document(phone).delete().await()
    }

    suspend fun resetPassword(phone: String, newPassword: String) {
        usersRef.document(phone).update("password", newPassword).await()
    }

    // ==================== DAILY LOGS ====================

    suspend fun addDailyRecord(record: MatchRecord) {
        val docRef = dailyLogsRef.document()
        docRef.set(record.copy(id = docRef.id)).await()
        aggregateWeeklyData(record)
    }

    suspend fun deleteDailyRecord(id: String) {
        dailyLogsRef.document(id).delete().await()
    }

    fun listenDailyLogs(): Flow<List<MatchRecord>> = callbackFlow {
        val reg = dailyLogsRef.orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val records = snap?.documents?.mapNotNull { it.toObject(MatchRecord::class.java) } ?: emptyList()
                trySend(records)
            }
        awaitClose { reg.remove() }
    }

    suspend fun resetDailyLogs() {
        val snapshot = dailyLogsRef.get().await()
        for (doc in snapshot.documents) { doc.reference.delete().await() }
    }

    // ==================== WEEKLY DATA ====================

    private suspend fun aggregateWeeklyData(record: MatchRecord) {
        val weeklyRef = settingsRef.document("weeklyData")
        try {
            val existing = weeklyRef.get().await()
            @Suppress("UNCHECKED_CAST")
            val data = (existing.data?.get("data") as? Map<String, Map<String, Any>>) ?: emptyMap()
            val playerData = data[record.playerName]?.toMutableMap() ?: mutableMapOf(
                "matches" to 0L, "wins" to 0L, "kills" to 0L, "assists" to 0L,
                "damage" to 0L, "survivalSeconds" to 0L
            )
            playerData["matches"] = (playerData["matches"] as? Long ?: 0) + record.matches
            playerData["wins"] = (playerData["wins"] as? Long ?: 0) + record.wins
            playerData["kills"] = (playerData["kills"] as? Long ?: 0) + record.kills
            playerData["assists"] = (playerData["assists"] as? Long ?: 0) + record.assists
            playerData["damage"] = (playerData["damage"] as? Long ?: 0) + record.damage
            playerData["survivalSeconds"] = (playerData["survivalSeconds"] as? Long ?: 0) + record.survivalSeconds
            val updated = data.toMutableMap()
            updated[record.playerName] = playerData
            weeklyRef.set(mapOf("data" to updated)).await()
        } catch (_: Exception) {}
    }

    fun listenWeeklyData(): Flow<Map<String, Map<String, Any>>> = callbackFlow {
        val reg = settingsRef.document("weeklyData").addSnapshotListener { snap, _ ->
            @Suppress("UNCHECKED_CAST")
            val data = snap?.data?.get("data") as? Map<String, Map<String, Any>> ?: emptyMap()
            trySend(data)
        }
        awaitClose { reg.remove() }
    }

    // ==================== LEADERBOARD ====================

    fun calculateLeaderboard(records: List<MatchRecord>, players: List<Player>): List<LeaderboardEntry> {
        val grouped = records.groupBy { it.playerName }
        val onlineMap = players.associate { it.phone to it.isOnline }
        return grouped.map { (name, logs) ->
            val totalMatches = logs.sumOf { it.matches }
            val totalWins = logs.sumOf { it.wins }
            val totalKills = logs.sumOf { it.kills }
            val totalAssists = logs.sumOf { it.assists }
            val totalDamage = logs.sumOf { it.damage }
            val totalSurvival = logs.sumOf { it.survivalSeconds }
            val avgDmg = if (totalMatches > 0) totalDamage / totalMatches else 0
            val winRate = if (totalMatches > 0) (totalWins.toDouble() / totalMatches) * 100 else 0.0
            val avgK = if (totalMatches > 0) totalKills.toDouble() / totalMatches else 0.0
            val score = MatchRecord.calculateScore(totalKills, totalDamage, totalWins)
            val isOnline = players.any { it.name == name && it.isOnline }
            LeaderboardEntry(
                playerName = name, matches = totalMatches, wins = totalWins,
                kills = totalKills, assists = totalAssists, damage = totalDamage,
                avgDamage = avgDmg, survivalSeconds = totalSurvival,
                score = score, winRate = winRate, avgKills = avgK,
                isOnline = isOnline
            )
        }.sortedByDescending { it.score }
    }

    // ==================== ANNOUNCEMENTS ====================

    fun listenAnnouncements(): Flow<List<Announcement>> = callbackFlow {
        val reg = announcementsRef.orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val items = snap?.documents?.mapNotNull { it.toObject(Announcement::class.java) } ?: emptyList()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    suspend fun postAnnouncement(message: String, author: String) {
        val docRef = announcementsRef.document()
        docRef.set(Announcement(id = docRef.id, message = message, author = author)).await()
    }

    suspend fun deleteAnnouncement(id: String) {
        announcementsRef.document(id).delete().await()
    }

    // ==================== CHAT ====================

    fun listenChat(): Flow<List<ChatMessage>> = callbackFlow {
        val reg = chatRef.orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                val messages = snap?.documents?.mapNotNull { it.toObject(ChatMessage::class.java) } ?: emptyList()
                trySend(messages)
            }
        awaitClose { reg.remove() }
    }

    suspend fun sendMessage(sender: String, message: String, isAdmin: Boolean) {
        val docRef = chatRef.document()
        docRef.set(ChatMessage(id = docRef.id, sender = sender, message = message, isAdmin = isAdmin)).await()
    }

    suspend fun deleteMessage(id: String) {
        chatRef.document(id).delete().await()
    }

    suspend fun clearChat() {
        val snapshot = chatRef.get().await()
        for (doc in snapshot.documents) { doc.reference.delete().await() }
    }

    // ==================== SETTINGS ====================

    fun listenThemeSettings(): Flow<Map<String, Any>> = callbackFlow {
        val reg = settingsRef.document("theme").addSnapshotListener { snap, _ ->
            trySend(snap?.data ?: emptyMap())
        }
        awaitClose { reg.remove() }
    }

    suspend fun saveThemeSettings(settings: Map<String, Any>) {
        settingsRef.document("theme").set(settings).await()
    }

    // ==================== OCR HELPER ====================

    fun parseOcrText(text: String): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        val patterns = mapOf(
            "kills" to Regex("""[Kk]ills?\s*[:=]?\s*(\d+)"""),
            "assists" to Regex("""[Aa]ssists?\s*[:=]?\s*(\d+)"""),
            "damage" to Regex("""[Dd]amage\s*[:=]?\s*(\d+)"""),
            "survival" to Regex("""[Ss]urvival\s*(?:[Tt]ime)?\s*[:=]?\s*(\d+)"""),
            "wins" to Regex("""[Ww]ins?\s*[:=]?\s*(\d+)""")
        )
        for ((key, regex) in patterns) {
            regex.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { result[key] = it }
        }
        return result
    }
}
