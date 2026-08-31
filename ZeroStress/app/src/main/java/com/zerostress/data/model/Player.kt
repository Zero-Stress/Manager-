package com.zerostress.data.model

data class Player(
    val name: String = "",
    val phone: String = "",
    val password: String = "",
    val role: String = "player",
    val status: String = "pending",
    val createdAt: Long = System.currentTimeMillis(),
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L
) {
    val isAdmin get() = role == "admin"
    val isConfirmed get() = status == "confirmed"
}
