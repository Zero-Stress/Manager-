package com.zerostress.manager.models

data class VoiceChannel(
    val id: String = "",
    val name: String = "",
    val isActive: Boolean = false,
    val participants: List<String> = emptyList(),
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
