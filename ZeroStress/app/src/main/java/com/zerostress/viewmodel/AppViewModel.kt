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

    fun approvePlayer(phone: String) {
        viewModelScope.launch {
            repo.updateUserStatus(phone, "confirmed")
            _toastMessage.emit("Player approved")
        }
    }

    fun rejectPlayer(phone: String) {
        viewModelScope.launch {
            repo.updateUserStatus(phone, "rejected")
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

    fun clearChat() {
        viewModelScope.launch {
            repo.clearChat()
            _toastMessage.emit("Chat cleared")
        }
    }

    // ==================== OCR ====================

    fun parseOcrText(text: String): Map<String, Int> = repo.parseOcrText(text)
}
