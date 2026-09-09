package com.zerostress.manager.models

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class Player(
    var id: String = "",
    var name: String = "",
    var phone: String = "",
    var role: String = "player",
    var status: String = "pending",
    var score: Long = 0L,
    var kills: Int = 0,
    var deaths: Int = 0,
    var assists: Int = 0,
    var damage: Long = 0L,
    var wins: Int = 0,
    var matches: Int = 0,
    var xp: Int = 0,
    var level: Int = 1,
    var coins: Int = 0,
    var rank: String = "Iron",
    var fcmToken: String? = null
) {
    fun toMap(): Map<String, Any> = mapOf(
        "uid" to id,
        "name" to name,
        "phone" to phone,
        "role" to role,
        "status" to status,
        "score" to score,
        "kills" to kills,
        "deaths" to deaths,
        "assists" to assists,
        "damage" to damage,
        "wins" to wins,
        "matches" to matches,
        "xp" to xp,
        "level" to level,
        "coins" to coins,
        "rank" to rank
    )

    val winRate: Double
        get() = if (matches > 0) wins * 100.0 / matches else 0.0

    val avgDamage: Double
        get() = if (matches > 0) damage * 1.0 / matches else 0.0

    companion object {
        @JvmStatic
        fun calculateScore(kills: Int, damage: Long, wins: Int): Long =
            (kills * 10L + damage / 100 + wins * 50L).toLong()

        @JvmStatic
        fun getRankTier(score: Long): String = when {
            score >= 5000 -> "Mythic"
            score >= 4000 -> "Diamond"
            score >= 3000 -> "Platinum"
            score >= 2000 -> "Gold"
            score >= 1200 -> "Silver"
            score >= 600 -> "Bronze"
            else -> "Iron"
        }

        @JvmStatic
        fun xpForLevel(level: Int): Int = level * 500
    }
}
