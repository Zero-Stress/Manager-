package com.zerostress.data.model

data class Announcement(
    val id: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val author: String = "Admin"
)
