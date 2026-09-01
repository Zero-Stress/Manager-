package com.zerostress.manager.models

data class MatchSchedule(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val time: String = "",
    val map: String = "",
    val mode: String = "",
    val maxPlayers: Int = 0,
    val status: String = "upcoming",
    val createdBy: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
