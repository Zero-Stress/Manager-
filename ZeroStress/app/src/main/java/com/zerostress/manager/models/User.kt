package com.zerostress.manager.models

data class User(
    val phone: String = "",
    val name: String = "",
    val password: String = "",
    val role: String = "player",
    val status: String = "pending",
    val createdAt: Long = System.currentTimeMillis()
)
