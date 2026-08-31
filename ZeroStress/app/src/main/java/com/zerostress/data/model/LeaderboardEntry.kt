package com.zerostress.data.model

data class LeaderboardEntry(
    val playerName: String = "",
    val matches: Int = 0,
    val wins: Int = 0,
    val kills: Int = 0,
    val assists: Int = 0,
    val damage: Int = 0,
    val avgDamage: Int = 0,
    val survivalSeconds: Int = 0,
    val score: Int = 0,
    val winRate: Double = 0.0,
    val avgKills: Double = 0.0,
    val isOnline: Boolean = false
) {
    val rank: Int = 0
}
