package com.zerostress.manager.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.zerostress.manager.models.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersRef = db.collection("users")
    private val dailyLogsRef = db.collection("dailylogs")
    private val announcementsRef = db.collection("announcements")
    private val chatRef = db.collection("chat")
    private val voiceRef = db.collection("voice_channels")
    private val scheduleRef = db.collection("match_schedules")
    private val friendsRef = db.collection("friendships")
    private val achievementsRef = db.collection("player_achievements")
    private val seasonsRef = db.collection("seasons")
    private val seasonSnapshotsRef = db.collection("season_snapshots")
    private val ranksRef = db.collection("player_ranks")
    private val challengesRef = db.collection("weekly_challenges")
    private val challengeProgressRef = db.collection("challenge_progress")
    private val mvpVotesRef = db.collection("mvp_votes")
    private val pollsRef = db.collection("polls")
    private val lfgRef = db.collection("lfg_posts")
    private val reviewsRef = db.collection("player_reviews")
    private val settingsRef = db.collection("admin_settings")

    // ==================== USERS ====================
    suspend fun createUser(user: User) {
        usersRef.document(user.phone).set(user).await()
    }

    suspend fun getUser(phone: String): User? {
        val doc = usersRef.document(phone).get().await()
        return doc.toObject(User::class.java)
    }

    fun observeUsers(): Flow<List<User>> = callbackFlow {
        val reg = usersRef.addSnapshotListener { snap, _ ->
            val users = snap?.documents?.mapNotNull { it.toObject(User::class.java) } ?: emptyList()
            trySend(users)
        }
        awaitClose { reg.remove() }
    }

    suspend fun updateUserStatus(phone: String, status: String) {
        usersRef.document(phone).update("status", status).await()
    }

    suspend fun updateUserName(phone: String, name: String) {
        usersRef.document(phone).update("name", name).await()
    }

    suspend fun updateUserRole(phone: String, role: String) {
        usersRef.document(phone).update("role", role).await()
    }

    suspend fun resetUserPassword(phone: String, newPassword: String) {
        usersRef.document(phone).update("password", newPassword).await()
    }

    suspend fun deleteUser(phone: String) {
        usersRef.document(phone).delete().await()
    }

    suspend fun getAllUsers(): List<User> {
        return usersRef.get().await().documents.mapNotNull { it.toObject(User::class.java) }
    }

    // ==================== DAILY LOGS ====================
    suspend fun addDailyLog(log: DailyLog) {
        val docRef = dailyLogsRef.document()
        val logWithId = log.copy(id = docRef.id, score = log.calculateScore())
        docRef.set(logWithId).await()
    }

    fun observeDailyLogs(): Flow<List<DailyLog>> = callbackFlow {
        val reg = dailyLogsRef.orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val logs = snap?.documents?.mapNotNull { it.toObject(DailyLog::class.java) } ?: emptyList()
                trySend(logs)
            }
        awaitClose { reg.remove() }
    }

    suspend fun deleteDailyLog(id: String) {
        dailyLogsRef.document(id).delete().await()
    }

    suspend fun resetDailyLogs() {
        val batch = db.batch()
        dailyLogsRef.get().await().documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    // ==================== ANNOUNCEMENTS ====================
    suspend fun postAnnouncement(announcement: Announcement) {
        val docRef = announcementsRef.document()
        docRef.set(announcement.copy(id = docRef.id)).await()
    }

    fun observeAnnouncements(): Flow<List<Announcement>> = callbackFlow {
        val reg = announcementsRef.orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val items = snap?.documents?.mapNotNull { it.toObject(Announcement::class.java) } ?: emptyList()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    suspend fun deleteAnnouncement(id: String) {
        announcementsRef.document(id).delete().await()
    }

    // ==================== CHAT ====================
    suspend fun sendChatMessage(message: ChatMessage) {
        val docRef = chatRef.document()
        docRef.set(message.copy(id = docRef.id)).await()
    }

    fun observeChatMessages(): Flow<List<ChatMessage>> = callbackFlow {
        val reg = chatRef.orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                val messages = snap?.documents?.mapNotNull { it.toObject(ChatMessage::class.java) } ?: emptyList()
                trySend(messages)
            }
        awaitClose { reg.remove() }
    }

    suspend fun clearChat() {
        val batch = db.batch()
        chatRef.get().await().documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    // ==================== VOICE CHANNELS ====================
    suspend fun createVoiceChannel(channel: VoiceChannel) {
        val docRef = voiceRef.document()
        docRef.set(channel.copy(id = docRef.id)).await()
    }

    fun observeVoiceChannels(): Flow<List<VoiceChannel>> = callbackFlow {
        val reg = voiceRef.addSnapshotListener { snap, _ ->
            val channels = snap?.documents?.mapNotNull { it.toObject(VoiceChannel::class.java) } ?: emptyList()
            trySend(channels)
        }
        awaitClose { reg.remove() }
    }

    suspend fun joinVoiceChannel(channelId: String, phone: String) {
        val doc = voiceRef.document(channelId).get().await()
        val channel = doc.toObject(VoiceChannel::class.java) ?: return
        val updated = channel.copy(participants = channel.participants + phone, isActive = true)
        voiceRef.document(channelId).set(updated).await()
    }

    suspend fun leaveVoiceChannel(channelId: String, phone: String) {
        val doc = voiceRef.document(channelId).get().await()
        val channel = doc.toObject(VoiceChannel::class.java) ?: return
        val updated = channel.copy(
            participants = channel.participants - phone,
            isActive = channel.participants.size > 1
        )
        voiceRef.document(channelId).set(updated).await()
    }

    suspend fun deleteVoiceChannel(channelId: String) {
        voiceRef.document(channelId).delete().await()
    }

    // ==================== MATCH SCHEDULES ====================
    suspend fun createSchedule(schedule: MatchSchedule) {
        val docRef = scheduleRef.document()
        docRef.set(schedule.copy(id = docRef.id)).await()
    }

    fun observeSchedules(): Flow<List<MatchSchedule>> = callbackFlow {
        val reg = scheduleRef.orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val schedules = snap?.documents?.mapNotNull { it.toObject(MatchSchedule::class.java) } ?: emptyList()
                trySend(schedules)
            }
        awaitClose { reg.remove() }
    }

    suspend fun updateScheduleStatus(id: String, status: String) {
        scheduleRef.document(id).update("status", status).await()
    }

    suspend fun deleteSchedule(id: String) {
        scheduleRef.document(id).delete().await()
    }

    // ==================== FRIENDS ====================
    suspend fun sendFriendRequest(requesterPhone: String, accepterPhone: String) {
        val existing = friendsRef.whereEqualTo("requesterPhone", requesterPhone)
            .whereEqualTo("accepterPhone", accepterPhone).get().await()
        if (existing.isEmpty) {
            val docRef = friendsRef.document()
            docRef.set(Friendship(id = docRef.id, requesterPhone = requesterPhone, accepterPhone = accepterPhone)).await()
        }
    }

    suspend fun acceptFriendRequest(friendshipId: String) {
        friendsRef.document(friendshipId).update("status", "accepted").await()
    }

    suspend fun removeFriend(friendshipId: String) {
        friendsRef.document(friendshipId).delete().await()
    }

    fun observeFriends(phone: String): Flow<List<Friendship>> = callbackFlow {
        val reg = friendsRef
            .whereEqualTo("status", "accepted")
            .addSnapshotListener { snap, _ ->
                val all = snap?.documents?.mapNotNull { it.toObject(Friendship::class.java) } ?: emptyList()
                val myFriends = all.filter { it.requesterPhone == phone || it.accepterPhone == phone }
                trySend(myFriends)
            }
        awaitClose { reg.remove() }
    }

    fun observeFriendRequests(phone: String): Flow<List<Friendship>> = callbackFlow {
        val reg = friendsRef
            .whereEqualTo("accepterPhone", phone)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snap, _ ->
                val requests = snap?.documents?.mapNotNull { it.toObject(Friendship::class.java) } ?: emptyList()
                trySend(requests)
            }
        awaitClose { reg.remove() }
    }

    suspend fun searchUsers(query: String): List<User> {
        if (query.isBlank()) return emptyList()
        return usersRef.whereGreaterThanOrEqualTo("name", query)
            .whereLessThanOrEqualTo("name", query + "\uF8FF")
            .get().await().documents.mapNotNull { it.toObject(User::class.java) }
    }

    // ==================== ACHIEVEMENTS ====================
    suspend fun unlockAchievement(phone: String, achievementId: String) {
        val existing = achievementsRef
            .whereEqualTo("phone", phone)
            .whereEqualTo("achievementId", achievementId)
            .get().await()
        if (existing.isEmpty) {
            achievementsRef.add(PlayerAchievement(phone = phone, achievementId = achievementId)).await()
        }
    }

    fun observeAchievements(phone: String): Flow<List<PlayerAchievement>> = callbackFlow {
        val reg = achievementsRef
            .whereEqualTo("phone", phone)
            .addSnapshotListener { snap, _ ->
                val achievements = snap?.documents?.mapNotNull { it.toObject(PlayerAchievement::class.java) } ?: emptyList()
                trySend(achievements)
            }
        awaitClose { reg.remove() }
    }

    // ==================== SEASONS ====================
    suspend fun createSeason(season: Season) {
        val docRef = seasonsRef.document()
        seasonsRef.document(docRef.id).set(season.copy(id = docRef.id)).await()
    }

    fun observeSeasons(): Flow<List<Season>> = callbackFlow {
        val reg = seasonsRef.orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val seasons = snap?.documents?.mapNotNull { it.toObject(Season::class.java) } ?: emptyList()
                trySend(seasons)
            }
        awaitClose { reg.remove() }
    }

    suspend fun setActiveSeason(seasonId: String) {
        // Deactivate all
        seasonsRef.get().await().documents.forEach { doc ->
            seasonsRef.document(doc.id).update("isActive", false).await()
        }
        seasonsRef.document(seasonId).update("isActive", true).await()
    }

    suspend fun saveSeasonSnapshot(snapshot: SeasonSnapshot) {
        seasonSnapshotsRef.add(snapshot).await()
    }

    fun observeSeasonSnapshots(seasonId: String): Flow<List<SeasonSnapshot>> = callbackFlow {
        val reg = seasonSnapshotsRef
            .whereEqualTo("seasonId", seasonId)
            .orderBy("finalScore", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val snapshots = snap?.documents?.mapNotNull { it.toObject(SeasonSnapshot::class.java) } ?: emptyList()
                trySend(snapshots)
            }
        awaitClose { reg.remove() }
    }

    // ==================== RANKS & LEVELS ====================
    suspend fun getOrCreateRank(phone: String): PlayerRank {
        val doc = ranksRef.document(phone).get().await()
        return doc.toObject(PlayerRank::class.java) ?: PlayerRank(phone = phone).also {
            ranksRef.document(phone).set(it).await()
        }
    }

    suspend fun updateRank(rank: PlayerRank) {
        ranksRef.document(rank.phone).set(rank).await()
    }

    fun observeRank(phone: String): Flow<PlayerRank?> = callbackFlow {
        val reg = ranksRef.document(phone).addSnapshotListener { snap, _ ->
            trySend(snap?.toObject(PlayerRank::class.java))
        }
        awaitClose { reg.remove() }
    }

    fun observeAllRanks(): Flow<List<PlayerRank>> = callbackFlow {
        val reg = ranksRef.addSnapshotListener { snap, _ ->
            val ranks = snap?.documents?.mapNotNull { it.toObject(PlayerRank::class.java) } ?: emptyList()
            trySend(ranks)
        }
        awaitClose { reg.remove() }
    }

    // ==================== WEEKLY CHALLENGES ====================
    suspend fun createChallenge(challenge: WeeklyChallenge) {
        val docRef = challengesRef.document()
        challengesRef.document(docRef.id).set(challenge.copy(id = docRef.id)).await()
    }

    fun observeChallenges(): Flow<List<WeeklyChallenge>> = callbackFlow {
        val reg = challengesRef.orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val challenges = snap?.documents?.mapNotNull { it.toObject(WeeklyChallenge::class.java) } ?: emptyList()
                trySend(challenges)
            }
        awaitClose { reg.remove() }
    }

    suspend fun updateChallengeProgress(challengeId: String, phone: String, playerName: String, value: Int) {
        val existing = challengeProgressRef
            .whereEqualTo("challengeId", challengeId)
            .whereEqualTo("phone", phone)
            .get().await()
        if (existing.isEmpty) {
            challengeProgressRef.add(ChallengeProgress(challengeId = challengeId, phone = phone, playerName = playerName, currentValue = value)).await()
        } else {
            challengeProgressRef.document(existing.documents[0].id)
                .update("currentValue", value).await()
        }
    }

    fun observeChallengeProgress(challengeId: String): Flow<List<ChallengeProgress>> = callbackFlow {
        val reg = challengeProgressRef
            .whereEqualTo("challengeId", challengeId)
            .addSnapshotListener { snap, _ ->
                val progress = snap?.documents?.mapNotNull { it.toObject(ChallengeProgress::class.java) } ?: emptyList()
                trySend(progress)
            }
        awaitClose { reg.remove() }
    }

    // ==================== MVP VOTES ====================
    suspend fun castMVPVote(matchId: String, voterPhone: String, candidatePhone: String, candidateName: String) {
        val existing = mvpVotesRef
            .whereEqualTo("matchId", matchId)
            .whereEqualTo("voterPhone", voterPhone)
            .get().await()
        if (existing.isEmpty) {
            mvpVotesRef.add(MVPVote(matchId = matchId, voterPhone = voterPhone, candidatePhone = candidatePhone, candidateName = candidateName)).await()
        }
    }

    fun observeMVPVotes(matchId: String): Flow<List<MVPVote>> = callbackFlow {
        val reg = mvpVotesRef
            .whereEqualTo("matchId", matchId)
            .addSnapshotListener { snap, _ ->
                val votes = snap?.documents?.mapNotNull { it.toObject(MVPVote::class.java) } ?: emptyList()
                trySend(votes)
            }
        awaitClose { reg.remove() }
    }

    // ==================== POLLS ====================
    suspend fun createPoll(poll: Poll) {
        val docRef = pollsRef.document()
        pollsRef.document(docRef.id).set(poll.copy(id = docRef.id)).await()
    }

    fun observePolls(): Flow<List<Poll>> = callbackFlow {
        val reg = pollsRef.orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val polls = snap?.documents?.mapNotNull { it.toObject(Poll::class.java) } ?: emptyList()
                trySend(polls)
            }
        awaitClose { reg.remove() }
    }

    suspend fun votePoll(pollId: String, option: String) {
        val doc = pollsRef.document(pollId).get().await()
        val poll = doc.toObject(Poll::class.java) ?: return
        val votes = poll.votes.toMutableMap()
        votes[option] = (votes[option] ?: 0) + 1
        pollsRef.document(pollId).update("votes", votes).await()
    }

    suspend fun deletePoll(id: String) { pollsRef.document(id).delete().await() }

    // ==================== LFG ====================
    suspend fun createLFGPost(post: LFGPost) {
        val docRef = lfgRef.document()
        lfgRef.document(docRef.id).set(post.copy(id = docRef.id)).await()
    }

    fun observeLFGPosts(): Flow<List<LFGPost>> = callbackFlow {
        val reg = lfgRef.whereEqualTo("isActive", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val posts = snap?.documents?.mapNotNull { it.toObject(LFGPost::class.java) } ?: emptyList()
                trySend(posts)
            }
        awaitClose { reg.remove() }
    }

    suspend fun deleteLFGPost(id: String) { lfgRef.document(id).delete().await() }

    // ==================== REVIEWS ====================
    suspend fun submitReview(review: PlayerReview) {
        reviewsRef.add(review).await()
    }

    fun observeReviews(targetPhone: String): Flow<List<PlayerReview>> = callbackFlow {
        val reg = reviewsRef
            .whereEqualTo("targetPhone", targetPhone)
            .addSnapshotListener { snap, _ ->
                val reviews = snap?.documents?.mapNotNull { it.toObject(PlayerReview::class.java) } ?: emptyList()
                trySend(reviews)
            }
        awaitClose { reg.remove() }
    }

    // ==================== ADMIN SETTINGS ====================
    suspend fun getAdminSettings(): AdminSettings {
        val doc = settingsRef.document("main").get().await()
        return doc.toObject(AdminSettings::class.java) ?: AdminSettings()
    }

    suspend fun updateAdminSettings(settings: AdminSettings) {
        settingsRef.document("main").set(settings).await()
    }

    suspend fun banPlayer(phone: String) {
        val s = getAdminSettings()
        updateAdminSettings(s.copy(bannedPlayers = s.bannedPlayers + phone))
    }

    suspend fun unbanPlayer(phone: String) {
        val s = getAdminSettings()
        updateAdminSettings(s.copy(bannedPlayers = s.bannedPlayers - phone))
    }

    suspend fun endSeasonAndSnapshot(seasonId: String) {
        val users = getAllUsers().filter { it.status == "confirmed" }
        val allLogs = db.collection("dailylogs").get().await()
            .documents.mapNotNull { it.toObject(DailyLog::class.java) }

        users.forEachIndexed { index, user ->
            val userLogs = allLogs.filter { it.phone == user.phone }
            val snapshot = SeasonSnapshot(
                seasonId = seasonId,
                playerName = user.name,
                phone = user.phone,
                totalMatches = userLogs.sumOf { it.matches },
                totalWins = userLogs.sumOf { it.wins },
                totalKills = userLogs.sumOf { it.kills },
                totalAssists = userLogs.sumOf { it.assists },
                totalDamage = userLogs.sumOf { it.damage },
                finalScore = userLogs.sumOf { it.calculateScore() },
                rank = index + 1
            )
            saveSeasonSnapshot(snapshot)
        }
    }
}
