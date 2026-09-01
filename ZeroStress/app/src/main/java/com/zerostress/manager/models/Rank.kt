package com.zerostress.manager.models

data class PlayerRank(
    val phone: String = "",
    val level: Int = 1,
    val xp: Int = 0,
    val rankTier: String = "Bronze",
    val coins: Int = 0,
    val title: String = "",
    val winStreak: Int = 0,
    val bestWinStreak: Int = 0,
    val lastLoginDate: String = "",
    val loginStreak: Int = 0,
    val totalLoginDays: Int = 0
) {
    fun xpForNextLevel(): Int = level * 100 + 50
    fun xpProgress(): Float = xp.toFloat() / xpForNextLevel()

    companion object {
        val TIERS = listOf(
            "Bronze" to 0, "Silver" to 5, "Gold" to 15, "Platinum" to 30,
            "Diamond" to 50, "Master" to 75, "Legend" to 100, "Mythic" to 150
        )

        val TITLES = listOf(
            "Rookie" to 0, "Fighter" to 5, "Warrior" to 10, "Slayer" to 20,
            "Champion" to 35, "Veteran" to 50, "Elite" to 75, "Master" to 100,
            "Grandmaster" to 150, "The Destroyer" to 200, "Iron Wall" to 250,
            "Clutch Master" to 300, "Legendary" to 400, "Mythic God" to 500
        )

        fun tierForLevel(level: String): String {
            var tier = "Bronze"
            for ((name, minLevel) in TIERS) {
                if (level.toIntOrNull() != null && level.toInt() >= minLevel) tier = name
            }
            return tier
        }

        fun titleForWins(wins: Int): String {
            var title = "Rookie"
            for ((name, minWins) in TITLES) {
                if (wins >= minWins) title = name
            }
            return title
        }

        fun xpFromMatch(kills: Int, wins: Int, damage: Int): Int {
            return (kills * 5) + (wins * 25) + (damage / 100)
        }
    }
}

data class DailyReward(
    val phone: String = "",
    val day: Int = 0,
    val reward: String = "",
    val coins: Int = 0,
    val collectedAt: Long = System.currentTimeMillis()
)

val DAILY_REWARD_TABLE = listOf(
    DayReward(1, "10 Coins", 10), DayReward(2, "15 Coins", 15),
    DayReward(3, "20 Coins", 20), DayReward(4, "25 Coins", 25),
    DayReward(5, "30 Coins", 30), DayReward(6, "40 Coins", 40),
    DayReward(7, "Mystery Box", 100)
)

data class DayReward(val day: Int, val reward: String, val coins: Int)
