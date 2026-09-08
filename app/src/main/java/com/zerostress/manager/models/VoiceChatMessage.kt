package com.zerostress.manager.models

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class VoiceChatMessage(
    var id: String = "",
    var senderId: String = "",
    var senderName: String = "",
    var text: String = "",
    var timestamp: Long = 0L
) {
    companion object {
        fun create(senderId: String, senderName: String, text: String): VoiceChatMessage =
            VoiceChatMessage(
                senderId = senderId,
                senderName = senderName,
                text = text,
                timestamp = System.currentTimeMillis()
            )
    }
}
