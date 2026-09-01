package com.zerostress.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zerostress.data.model.*
import com.zerostress.data.repository.FirestoreRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = FirestoreRepository()

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players.asStateFlow()

    private val _dailyLogs = MutableStateFlow<List<MatchRecord>>(emptyList())
    val dailyLogs: StateFlow<List<MatchRecord>> = _dailyLogs.asStateFlow()

    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard.asStateFlow()

    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())
    val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _weeklyData = MutableStateFlow<Map<String, Map<String, Any>>>(emptyMap())
    val weeklyData: StateFlow<Map<String, Map<String, Any>>> = _weeklyData.asStateFlow()

    private val _notifications = MutableStateFlow<List<com.zerostress.data.model.AppNotification>>(emptyList())
    val notifications: StateFlow<List<com.zerostress.data.model.AppNotification>> = _notifications.asStateFlow()

    // New feature states
    private val _missions = MutableStateFlow<List<Mission>>(emptyList())
    val missions: StateFlow<List<Mission>> = _missions.asStateFlow()

    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    private val _seasonPass = MutableStateFlow<SeasonPass?>(null)
    val seasonPass: StateFlow<SeasonPass?> = _seasonPass.asStateFlow()

    private val _coinBalance = MutableStateFlow<ZSCoinBalance?>(null)
    val coinBalance: StateFlow<ZSCoinBalance?> = _coinBalance.asStateFlow()

    private val _shopItems = MutableStateFlow<List<ShopItem>>(emptyList())
    val shopItems: StateFlow<List<ShopItem>> = _shopItems.asStateFlow()

    private val _squads = MutableStateFlow<List<Squad>>(emptyList())
    val squads: StateFlow<List<Squad>> = _squads.asStateFlow()

    private val _feedPosts = MutableStateFlow<List<TeamFeedPost>>(emptyList())
    val feedPosts: StateFlow<List<TeamFeedPost>> = _feedPosts.asStateFlow()

    private val _stories = MutableStateFlow<List<TeamStory>>(emptyList())
    val stories: StateFlow<List<TeamStory>> = _stories.asStateFlow()

    private val _trainingSessions = MutableStateFlow<List<TrainingSession>>(emptyList())
    val trainingSessions: StateFlow<List<TrainingSession>> = _trainingSessions.asStateFlow()

    private val _scrimmages = MutableStateFlow<List<Scrimmage>>(emptyList())
    val scrimmages: StateFlow<List<Scrimmage>> = _scrimmages.asStateFlow()

    private val _expenses = MutableStateFlow<List<ExpenseEntry>>(emptyList())
    val expenses: StateFlow<List<ExpenseEntry>> = _expenses.asStateFlow()

    private val _playerTitles = MutableStateFlow<List<PlayerTitle>>(emptyList())
    val playerTitles: StateFlow<List<PlayerTitle>> = _playerTitles.asStateFlow()

    private val _insights = MutableStateFlow<List<String>>(emptyList())
    val insights: StateFlow<List<String>> = _insights.asStateFlow()

    private val _hallOfFame = MutableStateFlow<Map<String, String>>(emptyMap())
    val hallOfFame: StateFlow<Map<String, String>> = _hallOfFame.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    init {
        startListeners()
    }

    private fun startListeners() {
        viewModelScope.launch {
            repo.listenUsers().collect { _players.value = it }
        }
        viewModelScope.launch {
            repo.listenDailyLogs().collect { logs ->
                _dailyLogs.value = logs
                val entries = repo.calculateLeaderboard(logs, _players.value)
                _leaderboard.value = entries
            }
        }
        viewModelScope.launch {
            repo.listenAnnouncements().collect { _announcements.value = it }
        }
        viewModelScope.launch {
            repo.listenChat().collect { _chatMessages.value = it }
        }
        viewModelScope.launch {
            repo.listenWeeklyData().collect { _weeklyData.value = it }
        }
    }

    // ==================== ADMIN ACTIONS ====================

    fun listenMyNotifications(phone: String) {
        viewModelScope.launch {
            repo.listenNotifications(phone).collect { _notifications.value = it }
        }
    }

    fun approvePlayer(phone: String) {
        viewModelScope.launch {
            repo.updateUserStatus(phone, "confirmed")
            repo.sendNotification(phone, "Account Approved", "Your account has been approved! You can now login.", "approval")
            _toastMessage.emit("Player approved")
        }
    }

    fun rejectPlayer(phone: String) {
        viewModelScope.launch {
            repo.updateUserStatus(phone, "rejected")
            repo.sendNotification(phone, "Account Rejected", "Your account request has been rejected.", "rejection")
            _toastMessage.emit("Player rejected")
        }
    }

    fun addPlayer(name: String, phone: String, password: String) {
        viewModelScope.launch {
            val result = repo.register("+880$phone", name, password)
            result.fold(
                onSuccess = { _toastMessage.emit("Player $name added successfully") },
                onFailure = { _toastMessage.emit("Failed: ${it.message}") }
            )
        }
    }

    fun updatePlayerRole(phone: String, role: String) {
        viewModelScope.launch {
            repo.updateUserRole(phone, role)
            repo.sendNotification(phone, "Role Changed", "Your role has been changed to ${role.uppercase()} by an admin.", "role_change")
            _toastMessage.emit("Role updated to $role")
        }
    }

    fun deletePlayer(phone: String) {
        viewModelScope.launch {
            repo.deleteUser(phone)
            _toastMessage.emit("Player deleted")
        }
    }

    fun resetPassword(phone: String, newPassword: String) {
        viewModelScope.launch {
            repo.resetPassword(phone, newPassword)
            repo.sendNotification(phone, "Password Reset", "Your password has been reset by an admin. Please login with the new password.", "password_reset")
            _toastMessage.emit("Password reset")
        }
    }

    // ==================== DAILY LOGS ====================

    fun addDailyRecord(record: MatchRecord) {
        viewModelScope.launch {
            repo.addDailyRecord(record)
            _toastMessage.emit("Record saved!")
        }
    }

    fun deleteDailyRecord(id: String) {
        viewModelScope.launch {
            repo.deleteDailyRecord(id)
            _toastMessage.emit("Record deleted")
        }
    }

    fun resetDailyLogs() {
        viewModelScope.launch {
            repo.resetDailyLogs()
            _toastMessage.emit("Daily logs cleared")
        }
    }

    // ==================== ANNOUNCEMENTS ====================

    fun postAnnouncement(message: String, author: String) {
        viewModelScope.launch {
            repo.postAnnouncement(message, author)
            _toastMessage.emit("Announcement posted!")
        }
    }

    fun deleteAnnouncement(id: String) {
        viewModelScope.launch {
            repo.deleteAnnouncement(id)
            _toastMessage.emit("Announcement deleted")
        }
    }

    // ==================== CHAT ====================

    fun sendMessage(sender: String, message: String, isAdmin: Boolean) {
        viewModelScope.launch {
            repo.sendMessage(sender, message, isAdmin)
        }
    }

    fun replyToMessage(sender: String, message: String, replyToSender: String, isAdmin: Boolean) {
        viewModelScope.launch {
            repo.sendMessage(sender, "↩$replyToSender: $message", isAdmin)
        }
    }

    fun deleteMessage(id: String) {
        viewModelScope.launch {
            repo.deleteMessage(id)
            _toastMessage.emit("Message deleted")
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repo.clearChat()
            _toastMessage.emit("Chat cleared")
        }
    }

    // ==================== MISSIONS ====================

    fun createMission(mission: Mission) {
        viewModelScope.launch { repo.createMission(mission); _toastMessage.emit("Mission created!") }
    }

    fun claimMission(id: String) {
        viewModelScope.launch { repo.claimMission(id); _toastMessage.emit("Reward claimed!") }
    }

    // ==================== SEASON PASS ====================

    fun claimSeasonReward(level: Int) {
        viewModelScope.launch { repo.claimSeasonReward(level); _toastMessage.emit("Season reward claimed!") }
    }

    // ==================== ZS COINS ====================

    fun addCoins(playerName: String, amount: Int) {
        viewModelScope.launch { repo.addCoins(playerName, amount, "bonus") }
    }

    fun spendCoins(playerName: String, amount: Int) {
        viewModelScope.launch {
            val ok = repo.spendCoins(playerName, amount)
            _toastMessage.emit(if (ok) "Purchased!" else "Not enough coins!")
        }
    }

    fun dailySpin(playerName: String): Int {
        val rewards = listOf(5, 10, 15, 20, 25, 50, 100)
        val reward = rewards.random()
        viewModelScope.launch {
            repo.addCoins(playerName, reward, "daily spin")
            repo.updateDailySpin(playerName)
            _toastMessage.emit("You won $reward ZS Coins!")
        }
        return reward
    }

    // ==================== SHOP ====================

    fun purchaseShopItem(playerName: String, item: ShopItem) {
        viewModelScope.launch {
            val ok = repo.purchaseItem(playerName, item)
            _toastMessage.emit(if (ok) "${item.name} purchased!" else "Not enough coins!")
        }
    }

    // ==================== SQUADS ====================

    fun createSquad(squad: Squad) {
        viewModelScope.launch { repo.createSquad(squad); _toastMessage.emit("Squad created!") }
    }

    fun joinSquad(squadId: String, playerName: String) {
        viewModelScope.launch { repo.joinSquad(squadId, playerName); _toastMessage.emit("Joined squad!") }
    }

    fun leaveSquad(squadId: String, playerName: String) {
        viewModelScope.launch { repo.leaveSquad(squadId, playerName) }
    }

    fun deleteSquad(squadId: String) {
        viewModelScope.launch { repo.deleteSquad(squadId) }
    }

    // ==================== TEAM FEED ====================

    fun createPost(post: TeamFeedPost) {
        viewModelScope.launch { repo.createPost(post) }
    }

    fun likePost(postId: String, playerName: String) {
        viewModelScope.launch { repo.likePost(postId, playerName) }
    }

    fun commentOnPost(postId: String, comment: TeamComment) {
        viewModelScope.launch { repo.commentOnPost(postId, comment) }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch { repo.deletePost(postId) }
    }

    // ==================== STORIES ====================

    fun postStory(story: TeamStory) {
        viewModelScope.launch { repo.postStory(story) }
    }

    // ==================== TRAINING ====================

    fun createTraining(session: TrainingSession) {
        viewModelScope.launch { repo.createTraining(session); _toastMessage.emit("Training scheduled!") }
    }

    fun attendTraining(sessionId: String, playerName: String) {
        viewModelScope.launch { repo.attendTraining(sessionId, playerName) }
    }

    // ==================== SCRIMMAGES ====================

    fun createScrimmage(scrimmage: Scrimmage) {
        viewModelScope.launch { repo.createScrimmage(scrimmage); _toastMessage.emit("Scrimmage scheduled!") }
    }

    // ==================== EXPENSES ====================

    fun addExpense(expense: ExpenseEntry) {
        viewModelScope.launch { repo.addExpense(expense); _toastMessage.emit("Expense recorded!") }
    }

    // ==================== PREDICTIONS ====================

    fun submitPrediction(prediction: Prediction) {
        viewModelScope.launch { repo.submitPrediction(prediction); _toastMessage.emit("Prediction submitted!") }
    }

    // ==================== TIME CAPSULES ====================

    fun createCapsule(capsule: TimeCapsule) {
        viewModelScope.launch { repo.createCapsule(capsule); _toastMessage.emit("Time capsule created!") }
    }

    // ==================== SMART FEATURES ====================

    fun generateInsights(records: List<MatchRecord>) {
        _insights.value = repo.generateInsights(records)
    }

    fun getHallOfFame(records: List<MatchRecord>) {
        _hallOfFame.value = repo.getHallOfFame(records)
    }

    fun buildOptimalTeam(entries: List<LeaderboardEntry>, size: Int): List<LeaderboardEntry> {
        return repo.buildOptimalTeam(entries, size)
    }

    fun analyzePlayTimes(records: List<MatchRecord>): String = repo.analyzePlayTimes(records)

    // ==================== OCR ====================

    fun parseOcrText(text: String): Map<String, Int> = repo.parseOcrText(text)
}
