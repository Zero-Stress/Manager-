package com.zerostress.data.model

data class AppNotification(
    val id: String = "",
    val recipientPhone: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "info", // info, approval, rejection, role_change, password_reset
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
