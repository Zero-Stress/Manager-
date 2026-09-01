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
    private val notificationsRef = db.collection("notifications")
    private val missionsRef = db.collection("missions")
    private val squadsRef = db.collection("squads")
    private val feedRef = db.collection("teamFeed")
    private val storiesRef = db.collection("teamStories")
    private val trainingRef = db.collection("trainingSessions")
    private val scrimmagesRef = db.collection("scrimmages")
    private val expensesRef = db.collection("expenses")
    private val predictionsRef = db.collection("predictions")
    private val capsulesRef = db.collection("timeCapsules")
    private val coinsRef = db.collection("zsCoins")
    private val shopRef = db.collection("shopItems")
    private val titlesRef = db.collection("playerTitles")
    private val badgesRef = db.collection("achievements")
    private val seasonRef = db.collection("seasonPass")

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

    // ==================== NOTIFICATIONS ====================

    suspend fun sendNotification(recipientPhone: String, title: String, message: String, type: String) {
        val docRef = notificationsRef.document()
        docRef.set(
            AppNotification(
                id = docRef.id,
                recipientPhone = recipientPhone,
                title = title,
                message = message,
                type = type
            )
        ).await()
    }

    fun listenNotifications(phone: String): Flow<List<AppNotification>> = callbackFlow {
        val reg = notificationsRef
            .whereEqualTo("recipientPhone", phone)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val items = snap?.documents?.mapNotNull { it.toObject(AppNotification::class.java) } ?: emptyList()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    suspend fun markNotificationRead(id: String) {
        notificationsRef.document(id).update("isRead", true).await()
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

    // ==================== MISSIONS & CHALLENGES ====================

    fun listenMissions(): Flow<List<Mission>> = callbackFlow {
        val reg = missionsRef.orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val items = snap?.documents?.mapNotNull { it.toObject(Mission::class.java) } ?: emptyList()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    suspend fun createMission(mission: Mission) {
        val doc = missionsRef.document()
        doc.set(mission.copy(id = doc.id)).await()
    }

    suspend fun updateMissionProgress(id: String, progress: Int) {
        missionsRef.document(id).update("currentValue", progress).await()
    }

    suspend fun claimMission(id: String) {
        missionsRef.document(id).update("isClaimed", true, "isCompleted", true).await()
    }

    // ==================== ACHIEVEMENTS ====================

    fun listenAchievements(): Flow<List<Achievement>> = callbackFlow {
        val reg = badgesRef.addSnapshotListener { snap, _ ->
            val items = snap?.documents?.mapNotNull { it.toObject(Achievement::class.java) } ?: emptyList()
            trySend(items)
        }
        awaitClose { reg.remove() }
    }

    suspend fun unlockAchievement(id: String) {
        badgesRef.document(id).update("isUnlocked", true, "unlockedAt", System.currentTimeMillis()).await()
    }

    suspend fun createAchievement(achievement: Achievement) {
        val doc = badgesRef.document()
        doc.set(achievement.copy(id = doc.id)).await()
    }

    // ==================== SEASON PASS ====================

    fun listenSeasonPass(): Flow<SeasonPass?> = callbackFlow {
        val reg = seasonRef.document("current").addSnapshotListener { snap, _ ->
            trySend(snap?.toObject(SeasonPass::class.java))
        }
        awaitClose { reg.remove() }
    }

    suspend fun updateSeasonXP(xp: Int) {
        seasonRef.document("current").update("currentXP", xp).await()
    }

    suspend fun claimSeasonReward(level: Int) {
        seasonRef.document("current").update("rewards.$level.isClaimed", true).await()
    }

    // ==================== ZS COINS ====================

    fun listenCoins(playerName: String): Flow<ZSCoinBalance?> = callbackFlow {
        val reg = coinsRef.document(playerName).addSnapshotListener { snap, _ ->
            trySend(snap?.toObject(ZSCoinBalance::class.java))
        }
        awaitClose { reg.remove() }
    }

    suspend fun addCoins(playerName: String, amount: Int, reason: String) {
        val doc = coinsRef.document(playerName)
        val existing = doc.get().await().toObject(ZSCoinBalance::class.java)
        if (existing != null) {
            doc.update(
                mapOf(
                    "balance" to (existing.balance + amount),
                    "totalEarned" to (existing.totalEarned + amount)
                )
            ).await()
        } else {
            doc.set(ZSCoinBalance(playerName = playerName, balance = amount, totalEarned = amount)).await()
        }
    }

    suspend fun spendCoins(playerName: String, amount: Int): Boolean {
        val doc = coinsRef.document(playerName)
        val existing = doc.get().await().toObject(ZSCoinBalance::class.java) ?: return false
        if (existing.balance < amount) return false
        doc.update(
            mapOf(
                "balance" to (existing.balance - amount),
                "totalSpent" to (existing.totalSpent + amount)
            )
        ).await()
        return true
    }

    suspend fun updateDailySpin(playerName: String) {
        coinsRef.document(playerName).update(
            mapOf(
                "lastDailySpin" to System.currentTimeMillis(),
                "loginStreak" to com.google.firebase.firestore.FieldValue.increment(1)
            )
        ).await()
    }

    // ==================== SHOP ====================

    fun listenShopItems(): Flow<List<ShopItem>> = callbackFlow {
        val reg = shopRef.addSnapshotListener { snap, _ ->
            val items = snap?.documents?.mapNotNull { it.toObject(ShopItem::class.java) } ?: emptyList()
            trySend(items)
        }
        awaitClose { reg.remove() }
    }

    suspend fun purchaseItem(playerName: String, item: ShopItem): Boolean {
        val spent = spendCoins(playerName, item.price)
        if (spent) {
            shopRef.document(item.id).update("isOwned", true).await()
        }
        return spent
    }

    // ==================== SQUADS ====================

    fun listenSquads(): Flow<List<Squad>> = callbackFlow {
        val reg = squadsRef.addSnapshotListener { snap, _ ->
            val items = snap?.documents?.mapNotNull { it.toObject(Squad::class.java) } ?: emptyList()
            trySend(items)
        }
        awaitClose { reg.remove() }
    }

    suspend fun createSquad(squad: Squad) {
        val doc = squadsRef.document()
        doc.set(squad.copy(id = doc.id)).await()
    }

    suspend fun joinSquad(squadId: String, playerName: String) {
        squadsRef.document(squadId).update("members", com.google.firebase.firestore.FieldValue.arrayUnion(playerName)).await()
    }

    suspend fun leaveSquad(squadId: String, playerName: String) {
        squadsRef.document(squadId).update("members", com.google.firebase.firestore.FieldValue.arrayRemove(playerName)).await()
    }

    suspend fun deleteSquad(squadId: String) {
        squadsRef.document(squadId).delete().await()
    }

    // ==================== TEAM FEED ====================

    fun listenFeed(): Flow<List<TeamFeedPost>> = callbackFlow {
        val reg = feedRef.orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val items = snap?.documents?.mapNotNull { it.toObject(TeamFeedPost::class.java) } ?: emptyList()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    suspend fun createPost(post: TeamFeedPost) {
        val doc = feedRef.document()
        doc.set(post.copy(id = doc.id)).await()
    }

    suspend fun likePost(postId: String, playerName: String) {
        feedRef.document(postId).update("likes", com.google.firebase.firestore.FieldValue.arrayUnion(playerName)).await()
    }

    suspend fun unlikePost(postId: String, playerName: String) {
        feedRef.document(postId).update("likes", com.google.firebase.firestore.FieldValue.arrayRemove(playerName)).await()
    }

    suspend fun commentOnPost(postId: String, comment: TeamComment) {
        feedRef.document(postId).update("comments", com.google.firebase.firestore.FieldValue.arrayUnion(comment)).await()
    }

    suspend fun deletePost(postId: String) {
        feedRef.document(postId).delete().await()
    }

    // ==================== TEAM STORIES ====================

    fun listenStories(): Flow<List<TeamStory>> = callbackFlow {
        val reg = storiesRef.orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val items = snap?.documents?.mapNotNull { it.toObject(TeamStory::class.java) }
                    ?.filter { !it.isExpired } ?: emptyList()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    suspend fun postStory(story: TeamStory) {
        val doc = storiesRef.document()
        doc.set(story.copy(id = doc.id, expiresAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000)).await()
    }

    // ==================== TRAINING ====================

    fun listenTrainingSessions(): Flow<List<TrainingSession>> = callbackFlow {
        val reg = trainingRef.orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val items = snap?.documents?.mapNotNull { it.toObject(TrainingSession::class.java) } ?: emptyList()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    suspend fun createTraining(session: TrainingSession) {
        val doc = trainingRef.document()
        doc.set(session.copy(id = doc.id)).await()
    }

    suspend fun attendTraining(sessionId: String, playerName: String) {
        trainingRef.document(sessionId).update("attendees", com.google.firebase.firestore.FieldValue.arrayUnion(playerName)).await()
    }

    suspend fun deleteTraining(sessionId: String) {
        trainingRef.document(sessionId).delete().await()
    }

    // ==================== SCRIMMAGES ====================

    fun listenScrimmages(): Flow<List<Scrimmage>> = callbackFlow {
        val reg = scrimmagesRef.orderBy("scheduledTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val items = snap?.documents?.mapNotNull { it.toObject(Scrimmage::class.java) } ?: emptyList()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    suspend fun createScrimmage(scrimmage: Scrimmage) {
        val doc = scrimmagesRef.document()
        doc.set(scrimmage.copy(id = doc.id)).await()
    }

    suspend fun updateScrimmageResult(id: String, ourScore: Int, theirScore: Int, status: String) {
        scrimmagesRef.document(id).update(mapOf("ourScore" to ourScore, "theirScore" to theirScore, "status" to status)).await()
    }

    suspend fun deleteScrimmage(id: String) {
        scrimmagesRef.document(id).delete().await()
    }

    // ==================== EXPENSES ====================

    fun listenExpenses(): Flow<List<ExpenseEntry>> = callbackFlow {
        val reg = expensesRef.orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val items = snap?.documents?.mapNotNull { it.toObject(ExpenseEntry::class.java) } ?: emptyList()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    suspend fun addExpense(expense: ExpenseEntry) {
        val doc = expensesRef.document()
        doc.set(expense.copy(id = doc.id)).await()
    }

    suspend fun deleteExpense(id: String) {
        expensesRef.document(id).delete().await()
    }

    // ==================== PREDICTIONS ====================

    fun listenPredictions(playerName: String): Flow<List<Prediction>> = callbackFlow {
        val reg = predictionsRef.whereEqualTo("playerName", playerName)
            .addSnapshotListener { snap, _ ->
                val items = snap?.documents?.mapNotNull { it.toObject(Prediction::class.java) } ?: emptyList()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    suspend fun submitPrediction(prediction: Prediction) {
        val doc = predictionsRef.document()
        doc.set(prediction.copy(id = doc.id)).await()
    }

    // ==================== TIME CAPSULES ====================

    fun listenCapsules(playerName: String): Flow<List<TimeCapsule>> = callbackFlow {
        val reg = capsulesRef.whereEqualTo("authorName", playerName)
            .addSnapshotListener { snap, _ ->
                val items = snap?.documents?.mapNotNull { it.toObject(TimeCapsule::class.java) } ?: emptyList()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    suspend fun createCapsule(capsule: TimeCapsule) {
        val doc = capsulesRef.document()
        doc.set(capsule.copy(id = doc.id)).await()
    }

    // ==================== PLAYER TITLES ====================

    fun listenTitles(): Flow<List<PlayerTitle>> = callbackFlow {
        val reg = titlesRef.addSnapshotListener { snap, _ ->
            val items = snap?.documents?.mapNotNull { it.toObject(PlayerTitle::class.java) } ?: emptyList()
            trySend(items)
        }
        awaitClose { reg.remove() }
    }

    suspend fun createTitle(title: PlayerTitle) {
        val doc = titlesRef.document()
        doc.set(title.copy(id = doc.id)).await()
    }

    // ==================== AUTO TEAM BUILDER ====================

    fun buildOptimalTeam(players: List<LeaderboardEntry>, squadSize: Int = 4): List<LeaderboardEntry> {
        if (players.size <= squadSize) return players
        return players.sortedByDescending { it.score }.take(squadSize)
    }

    // ==================== WIN PREDICTION ====================

    fun predictWinChance(playerStats: LeaderboardEntry, opponentStats: LeaderboardEntry): Int {
        val ourScore = playerStats.score.toDouble()
        val theirScore = opponentStats.score.toDouble()
        val total = ourScore + theirScore
        return if (total > 0) ((ourScore / total) * 100).toInt().coerceIn(10, 90) else 50
    }

    // ==================== PERFORMANCE INSIGHTS ====================

    fun generateInsights(records: List<MatchRecord>): List<String> {
        val insights = mutableListOf<String>()
        if (records.size < 3) return listOf("Play more matches to get insights!")
        val recent = records.take(5)
        val older = records.drop(5).take(5)
        val recentAvgKills = if (recent.isNotEmpty()) recent.map { it.kills }.average() else 0.0
        val olderAvgKills = if (older.isNotEmpty()) older.map { it.kills }.average() else recentAvgKills
        if (recentAvgKills > olderAvgKills * 1.2) insights.add("🔥 Your kills are improving! Keep it up!")
        else if (recentAvgKills < olderAvgKills * 0.8) insights.add("📉 Your kills dropped recently. Focus on aim training.")
        val recentAvgDamage = if (recent.isNotEmpty()) recent.map { it.damage }.average() else 0.0
        val olderAvgDamage = if (older.isNotEmpty()) older.map { it.damage }.average() else recentAvgDamage
        if (recentAvgDamage > olderAvgDamage * 1.2) insights.add("💪 Damage output is up! Great aggression.")
        val recentWinRate = if (recent.isNotEmpty()) recent.count { it.wins > 0 }.toDouble() / recent.size * 100 else 0.0
        if (recentWinRate > 60) insights.add("🏆 Win rate above 60% — you're in top form!")
        else if (recentWinRate < 30) insights.add("⚠️ Win rate below 30%. Try changing strategy.")
        val avgSurvival = records.map { it.survivalSeconds }.average()
        if (avgSurvival > 600) insights.add("⏱️ Great survival time — you play smart.")
        else if (avgSurvival < 180) insights.add("⏱️ Low survival time — try playing more defensively.")
        if (insights.isEmpty()) insights.add("📊 Stats are stable. Try new tactics to break through!")
        return insights
    }

    // ==================== BEST TIME TO PLAY ====================

    fun analyzePlayTimes(records: List<MatchRecord>): String {
        if (records.isEmpty()) return "No data yet"
        val winRecords = records.filter { it.wins > 0 }
        if (winRecords.isEmpty()) return "No wins recorded yet"
        val totalWinRate = winRecords.size.toDouble() / records.size * 100
        return "Win rate: ${"%.1f".format(totalWinRate)}% across ${records.size} matches. " +
                "Keep playing consistently to find your peak hours!"
    }

    // ==================== HALL OF FAME ====================

    fun getHallOfFame(records: List<MatchRecord>): Map<String, String> {
        if (records.isEmpty()) return emptyMap()
        val grouped = records.groupBy { it.playerName }
        val mostKills = grouped.maxByOrNull { it.value.sumOf { r -> r.kills } }
        val mostDamage = grouped.maxByOrNull { it.value.sumOf { r -> r.damage } }
        val bestWinRate = grouped.filter { it.value.sumOf { r -> r.matches } >= 3 }
            .maxByOrNull { it.value.sumOf { r -> r.wins }.toDouble() / it.value.sumOf { r -> r.matches } }
        val mostMVP = grouped.maxByOrNull { entry -> entry.value.sumOf { MatchRecord.calculateScore(it.kills, it.damage, it.wins) } }
        return mapOf(
            "🥇 Most Kills" to (mostKills?.key ?: "-"),
            "💥 Most Damage" to (mostDamage?.key ?: "-"),
            "🏆 Best Win Rate" to (bestWinRate?.key ?: "-"),
            "⭐ MVP" to (mostMVP?.key ?: "-")
        )
    }

    // ==================== SESSION TRACKING ====================

    suspend fun saveSessionDuration(playerName: String, durationMinutes: Int) {
        val doc = coinsRef.document("${playerName}_sessions")
        doc.update("totalMinutes", com.google.firebase.firestore.FieldValue.increment(durationMinutes.toLong())).await()
    }

    // ==================== REFERRAL ====================

    suspend fun applyReferral(newPlayer: String, referrer: String) {
        addCoins(referrer, 50, "Referral bonus for inviting $newPlayer")
        addCoins(newPlayer, 25, "Welcome bonus")
    }
}

