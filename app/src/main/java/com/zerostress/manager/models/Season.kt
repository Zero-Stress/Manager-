package com.zerostress.manager.models

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Season(
    var id: String = "",
    var name: String = "",
    var description: String? = null,
    var duration: String? = null,
    var startDate: Long = 0L,
    var endDate: Long = 0L,
    var active: Boolean = true,
    var topRewardCoins: Int = 500,
    var createdAt: Long = 0L
) {
    companion object {
        fun create(name: String, startDate: Long, endDate: Long): Season =
            Season(
                name = name,
                startDate = startDate,
                endDate = endDate,
                active = true,
                topRewardCoins = 500
            )
    }
}
