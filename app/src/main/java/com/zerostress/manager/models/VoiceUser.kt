package com.zerostress.manager.models

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class VoiceUser(
    var userId: String = "",
    var userName: String = "",
    var userStatus: String = Status.ONLINE.name,
    var voiceState: String = VoiceState.CONNECTED.name,
    var isMuted: Boolean = false,
    var isDeafened: Boolean = false,
    var isScreenSharing: Boolean = false,
    var isHandRaised: Boolean = false,
    var isSpeaking: Boolean = false,
    var isHost: Boolean = false,
    var isSpeaker: Boolean = false,
    var joinedAt: Long = 0L,
    var lastActive: Long = 0L
) {
    enum class Status { ONLINE, IDLE, DO_NOT_DISTURB }
    enum class VoiceState { CONNECTED, MUTED, DEAFENED, SCREEN_SHARING, HAND_RAISED, SPEAKING }

    companion object {
        fun create(userId: String, userName: String): VoiceUser {
            val now = System.currentTimeMillis()
            return VoiceUser(
                userId = userId,
                userName = userName,
                userStatus = Status.ONLINE.name,
                voiceState = VoiceState.CONNECTED.name,
                isMuted = false,
                isDeafened = false,
                isScreenSharing = false,
                isHandRaised = false,
                isSpeaking = false,
                isHost = false,
                isSpeaker = false,
                joinedAt = now,
                lastActive = now
            )
        }
    }
}
