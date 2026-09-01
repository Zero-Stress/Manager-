package com.zerostress.manager.models

data class Season(
    val id: String = "",
    val name: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val isActive: Boolean = false,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class SeasonSnapshot(
    val id: String = "",
    val seasonId: String = "",
    val playerName: String = "",
    val phone: String = "",
    val totalMatches: Int = 0,
    val totalWins: Int = 0,
    val totalKills: Int = 0,
    val totalAssists: Int = 0,
    val totalDamage: Int = 0,
    val finalScore: Double = 0.0,
    val rank: Int = 0,
    val snapshotAt: Long = System.currentTimeMillis()
)
