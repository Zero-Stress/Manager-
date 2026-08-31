package com.zerostress.data.model

data class ChatMessage(
    val id: String = "",
    val sender: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isAdmin: Boolean = false
)
