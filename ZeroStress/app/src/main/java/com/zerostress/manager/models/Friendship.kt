package com.zerostress.manager.models

data class Friendship(
    val id: String = "",
    val requesterPhone: String = "",
    val accepterPhone: String = "",
    val status: String = "pending", // pending, accepted, blocked
    val createdAt: Long = System.currentTimeMillis()
)
