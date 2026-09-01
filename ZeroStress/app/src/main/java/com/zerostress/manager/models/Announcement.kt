package com.zerostress.manager.models

data class Announcement(
    val id: String = "",
    val message: String = "",
    val postedBy: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
