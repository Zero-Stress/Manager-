package com.zerostress.manager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zerostress.manager.data.FirebaseRepository
import com.zerostress.manager.models.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppViewModel : ViewModel() {
    private val repo = FirebaseRepository()

    val users = repo.observeUsers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val dailyLogs = repo.observeDailyLogs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val announcements = repo.observeAnnouncements().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val chatMessages = repo.observeChatMessages().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val voiceChannels = repo.observeVoiceChannels().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val schedules = repo.observeSchedules().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val seasons = repo.observeSeasons().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _friends = MutableStateFlow<List<Friendship>>(emptyList())
    val friends: StateFlow<List<Friendship>> = _friends
    private val _friendRequests = MutableStateFlow<List<Friendship>>(emptyList())
    val friendRequests: StateFlow<List<Friendship>> = _friendRequests
    private val _playerAchievements = MutableStateFlow<List<PlayerAchievement>>(emptyList())
    val playerAchievements: StateFlow<List<PlayerAchievement>> = _playerAchievements
    private val _seasonSnapshots = MutableStateFlow<List<SeasonSnapshot>>(emptyList())
    val seasonSnapshots: StateFlow<List<SeasonSnapshot>> = _seasonSnapshots
    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults

    fun loadFriends(phone: String) {
        repo.observeFriends(phone).onEach { _friends.value = it }.launchIn(viewModelScope)
        repo.observeFriendRequests(phone).onEach { _friendRequests.value = it }.launchIn(viewModelScope)
    }

    fun loadAchievements(phone: String) {
        repo.observeAchievements(phone).onEach { _playerAchievements.value = it }.launchIn(viewModelScope)
    }

    fun loadSeasonSnapshots(seasonId: String) {
        repo.observeSeasonSnapshots(seasonId).onEach { _seasonSnapshots.value = it }.launchIn(viewModelScope)
    }

    fun searchUsers(query: String) = viewModelScope.launch {
        _searchResults.value = repo.searchUsers(query)
    }

    // ==================== USER ACTIONS ====================
    fun approvePlayer(phone: String) = viewModelScope.launch { repo.updateUserStatus(phone, "confirmed") }
    fun setPlayerPending(phone: String) = viewModelScope.launch { repo.updateUserStatus(phone, "pending") }
    fun deletePlayer(phone: String) = viewModelScope.launch { repo.deleteUser(phone) }
    fun updatePlayerName(phone: String, name: String) = viewModelScope.launch { repo.updateUserName(phone, name) }
    fun updatePlayerRole(phone: String, role: String) = viewModelScope.launch { repo.updateUserRole(phone, role) }
    fun resetPlayerPassword(phone: String, newPassword: String) = viewModelScope.launch { repo.resetUserPassword(phone, newPassword) }
    fun addPlayer(user: User) = viewModelScope.launch { repo.createUser(user) }

    // ==================== DAILY LOG ACTIONS ====================
    fun addDailyLog(log: DailyLog) = viewModelScope.launch { repo.addDailyLog(log) }
    fun deleteDailyLog(id: String) = viewModelScope.launch { repo.deleteDailyLog(id) }
    fun resetDailyLogs() = viewModelScope.launch { repo.resetDailyLogs() }

    // ==================== ANNOUNCEMENT ACTIONS ====================
    fun postAnnouncement(announcement: Announcement) = viewModelScope.launch { repo.postAnnouncement(announcement) }
    fun deleteAnnouncement(id: String) = viewModelScope.launch { repo.deleteAnnouncement(id) }

    // ==================== CHAT ACTIONS ====================
    fun sendChatMessage(message: ChatMessage) = viewModelScope.launch { repo.sendChatMessage(message) }
    fun clearChat() = viewModelScope.launch { repo.clearChat() }

    // ==================== VOICE ACTIONS ====================
    fun createVoiceChannel(name: String, createdBy: String) = viewModelScope.launch {
        repo.createVoiceChannel(VoiceChannel(name = name, createdBy = createdBy, isActive = true))
    }
    fun joinVoiceChannel(channelId: String, phone: String) = viewModelScope.launch { repo.joinVoiceChannel(channelId, phone) }
    fun leaveVoiceChannel(channelId: String, phone: String) = viewModelScope.launch { repo.leaveVoiceChannel(channelId, phone) }
    fun deleteVoiceChannel(channelId: String) = viewModelScope.launch { repo.deleteVoiceChannel(channelId) }

    // ==================== SCHEDULE ACTIONS ====================
    fun createSchedule(schedule: MatchSchedule) = viewModelScope.launch { repo.createSchedule(schedule) }
    fun updateScheduleStatus(id: String, status: String) = viewModelScope.launch { repo.updateScheduleStatus(id, status) }
    fun deleteSchedule(id: String) = viewModelScope.launch { repo.deleteSchedule(id) }

    // ==================== FRIEND ACTIONS ====================
    fun sendFriendRequest(from: String, to: String) = viewModelScope.launch { repo.sendFriendRequest(from, to) }
    fun acceptFriendRequest(id: String) = viewModelScope.launch { repo.acceptFriendRequest(id) }
    fun removeFriend(id: String) = viewModelScope.launch { repo.removeFriend(id) }

    // ==================== ACHIEVEMENT ACTIONS ====================
    fun unlockAchievement(phone: String, achievementId: String) = viewModelScope.launch { repo.unlockAchievement(phone, achievementId) }

    // ==================== SEASON ACTIONS ====================
    fun createSeason(season: Season) = viewModelScope.launch { repo.createSeason(season) }
    fun setActiveSeason(id: String) = viewModelScope.launch { repo.setActiveSeason(id) }
    fun endSeason(seasonId: String) = viewModelScope.launch { repo.endSeasonAndSnapshot(seasonId) }
}
