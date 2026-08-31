package com.zerostress.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zerostress.data.model.VoiceChannel
import com.zerostress.data.model.VoiceParticipant
import com.zerostress.data.repository.VoiceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VoiceChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = VoiceRepository()

    private val _channels = MutableStateFlow<List<VoiceChannel>>(emptyList())
    val channels: StateFlow<List<VoiceChannel>> = _channels.asStateFlow()

    private val _currentChannel = MutableStateFlow<VoiceChannel?>(null)
    val currentChannel: StateFlow<VoiceChannel?> = _currentChannel.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isDeafened = MutableStateFlow(false)
    val isDeafened: StateFlow<Boolean> = _isDeafened.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            repo.listenChannels().collect { channels ->
                _channels.value = channels
                // Update current channel if user is in one
                _currentChannel.value?.let { current ->
                    val updated = channels.find { it.id == current.id }
                    _currentChannel.value = updated
                }
            }
        }
    }

    fun createChannel(name: String, type: String, createdBy: String) {
        viewModelScope.launch {
            repo.createChannel(name, type, createdBy)
            _toastMessage.emit("Voice channel '$name' created")
        }
    }

    fun joinChannel(channelId: String, user: com.zerostress.data.model.Player) {
        viewModelScope.launch {
            val participant = VoiceParticipant(
                phone = user.phone,
                name = user.name,
                joinedAt = System.currentTimeMillis()
            )
            repo.joinChannel(channelId, participant)
            _currentChannel.value = _channels.value.find { it.id == channelId }
            _isMuted.value = false
            _isDeafened.value = false
            _toastMessage.emit("Joined voice channel")
        }
    }

    fun leaveChannel() {
        viewModelScope.launch {
            val channel = _currentChannel.value ?: return@launch
            val userPhone = channel.participants.firstOrNull()?.phone ?: return@launch
            repo.leaveChannel(channel.id, userPhone)
            _currentChannel.value = null
            _isMuted.value = false
            _isDeafened.value = false
            _toastMessage.emit("Left voice channel")
        }
    }

    fun toggleMute() {
        viewModelScope.launch {
            val channel = _currentChannel.value ?: return@launch
            val newMuted = !_isMuted.value
            _isMuted.value = newMuted
            val phone = channel.participants.firstOrNull()?.phone ?: return@launch
            repo.toggleMute(channel.id, phone, newMuted)
        }
    }

    fun toggleDeafen() {
        viewModelScope.launch {
            val channel = _currentChannel.value ?: return@launch
            val newDeafened = !_isDeafened.value
            _isDeafened.value = newDeafened
            if (newDeafened) _isMuted.value = true
            val phone = channel.participants.firstOrNull()?.phone ?: return@launch
            repo.toggleDeafen(channel.id, phone, newDeafened)
        }
    }

    fun deleteChannel(channelId: String) {
        viewModelScope.launch {
            repo.deleteChannel(channelId)
            if (_currentChannel.value?.id == channelId) _currentChannel.value = null
            _toastMessage.emit("Channel deleted")
        }
    }

    fun kickParticipant(channelId: String, phone: String) {
        viewModelScope.launch {
            repo.kickParticipant(channelId, phone)
            _toastMessage.emit("Participant removed")
        }
    }
}
