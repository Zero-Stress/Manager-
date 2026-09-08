package com.zerostress.manager.models

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class FriendRequest(
    var id: String = "",
    var fromUserId: String = "",
    var fromUserName: String = "",
    var toUserId: String = "",
    var status: String = "pending",
    var timestamp: Long = 0L
) {
    companion object {
        fun create(fromUserId: String, fromUserName: String, toUserId: String): FriendRequest =
            FriendRequest(
                fromUserId = fromUserId,
                fromUserName = fromUserName,
                toUserId = toUserId,
                status = "pending",
                timestamp = System.currentTimeMillis()
            )
    }
}
