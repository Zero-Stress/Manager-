package com.zerostress.manager.models

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class MatchSchedule(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var matchTime: Long = 0L,
    var status: String = "upcoming",
    var createdBy: String = ""
) {
    companion object {
        fun create(title: String, description: String, matchTime: Long, createdBy: String): MatchSchedule =
            MatchSchedule(
                title = title,
                description = description,
                matchTime = matchTime,
                createdBy = createdBy,
                status = "upcoming"
            )
    }
}
