package com.zerostress.manager.models

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class VoiceChannel(
    var id: String = "",
    var name: String = "",
    var category: String = "",
    var active: Boolean = true,
    var maxUsers: Int = 10,
    var isStage: Boolean = false,
    var allowScreenShare: Boolean = true,
    var allowRecording: Boolean = false,
    var participants: List<String> = emptyList(),
    var maxParticipants: Int = 10,
    var createdAt: Long = 0L
) {
    companion object {
        fun create(id: String, name: String, category: String): VoiceChannel = VoiceChannel(
            id = id,
            name = name,
            category = category,
            active = true,
            maxUsers = 10,
            isStage = false,
            allowScreenShare = true,
            allowRecording = false,
            participants = emptyList(),
            maxParticipants = 10,
            createdAt = System.currentTimeMillis()
        )
    }
}
