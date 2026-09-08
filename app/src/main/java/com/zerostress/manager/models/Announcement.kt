package com.zerostress.manager.models

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Announcement(
    var id: String = "",
    var text: String = "",
    var author: String = "",
    var timestamp: Long = 0L
) {
    companion object {
        fun create(text: String, author: String): Announcement =
            Announcement(text = text, author = author, timestamp = System.currentTimeMillis())
    }
}
