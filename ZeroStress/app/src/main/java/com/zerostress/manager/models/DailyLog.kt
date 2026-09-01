package com.zerostress.manager.models

data class DailyLog(
    val id: String = "",
    val playerName: String = "",
    val phone: String = "",
    val matches: Int = 0,
    val wins: Int = 0,
    val kills: Int = 0,
    val assists: Int = 0,
    val damage: Int = 0,
    val survivalMinutes: Int = 0,
    val survivalSeconds: Int = 0,
    val score: Double = 0.0,
    val date: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun calculateScore(): Double {
        return (kills * 10.0) + (damage / 100.0) + (wins * 50.0)
    }

    fun avgDamage(): Double = if (matches > 0) damage.toDouble() / matches else 0.0
    fun avgKills(): Double = if (matches > 0) kills.toDouble() / matches else 0.0
    fun winRate(): Double = if (matches > 0) (wins.toDouble() / matches) * 100 else 0.0
    fun survivalFormatted(): String = String.format("%dm %ds", survivalMinutes, survivalSeconds)
}
