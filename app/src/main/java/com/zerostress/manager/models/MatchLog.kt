package com.zerostress.manager.models

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class MatchLog(
    var id: String = "",
    var playerId: String = "",
    var playerName: String = "",
    var kills: Int = 0,
    var deaths: Int = 0,
    var assists: Int = 0,
    var damage: Long = 0L,
    var win: Boolean = false,
    var matchType: String? = null,
    var date: Long = 0L
) {
    val score: Long
        get() = (kills * 10L + damage / 100 + (if (win) 200L else 0L))

    companion object {
        fun create(
            playerId: String,
            playerName: String,
            kills: Int,
            deaths: Int,
            assists: Int,
            damage: Long,
            win: Boolean
        ): MatchLog = MatchLog(
            playerId = playerId,
            playerName = playerName,
            kills = kills,
            deaths = deaths,
            assists = assists,
            damage = damage,
            win = win,
            date = System.currentTimeMillis()
        )
    }
}
