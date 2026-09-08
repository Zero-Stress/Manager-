package com.zerostress.manager.models

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Achievement(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var xpReward: Int = 0,
    var coinReward: Int = 0,
    var icon: String? = null,
    var requirement: String? = null
) {
    companion object {
        fun create(
            name: String,
            description: String,
            xpReward: Int,
            coinReward: Int,
            icon: String,
            requirement: String
        ): Achievement = Achievement(
            name = name,
            description = description,
            xpReward = xpReward,
            coinReward = coinReward,
            icon = icon,
            requirement = requirement
        )
    }
}
