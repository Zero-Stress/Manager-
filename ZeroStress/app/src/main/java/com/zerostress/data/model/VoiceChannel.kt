package com.zerostress.data.model

data class VoiceChannel(
    val id: String = "",
    val name: String = "",
    val type: String = "squad", // squad, general, tournament
    val isActive: Boolean = true,
    val participants: List<VoiceParticipant> = emptyList(),
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class VoiceParticipant(
    val phone: String = "",
    val name: String = "",
    val isMuted: Boolean = false,
    val isDeafened: Boolean = false,
    val isSpeaking: Boolean = false,
    val joinedAt: Long = System.currentTimeMillis()
)
