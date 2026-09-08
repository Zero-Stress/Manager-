package com.zerostress.manager.models

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class ChatMessage(
    var id: String = "",
    var senderId: String = "",
    var senderName: String = "",
    var text: String = "",
    var timestamp: Long = 0L,
    var deleted: Boolean = false,
    var mentions: List<String> = emptyList()
) {
    companion object {
        fun create(senderId: String, senderName: String, text: String): ChatMessage =
            ChatMessage(
                senderId = senderId,
                senderName = senderName,
                text = text,
                timestamp = System.currentTimeMillis(),
                deleted = false
            )
    }
}
