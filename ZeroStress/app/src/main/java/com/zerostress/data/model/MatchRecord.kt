package com.zerostress.data.model

data class MatchRecord(
    val id: String = "",
    val playerName: String = "",
    val matches: Int = 0,
    val wins: Int = 0,
    val kills: Int = 0,
    val assists: Int = 0,
    val damage: Int = 0,
    val avgDamage: Int = 0,
    val survivalSeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    val winRate: Double
        get() = if (matches > 0) (wins.toDouble() / matches) * 100 else 0.0

    val avgKills: Double
        get() = if (matches > 0) kills.toDouble() / matches else 0.0

    companion object {
        fun calculateScore(kills: Int, damage: Int, wins: Int): Int {
            return (kills * 10) + (damage / 100) + (wins * 50)
        }
    }
}
