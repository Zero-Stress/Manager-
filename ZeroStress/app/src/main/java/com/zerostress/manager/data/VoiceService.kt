package com.zerostress.manager.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.zerostress.manager.MainActivity

class VoiceService : Service() {

    private val binder = LocalBinder()
    private var isMuted = false
    private var channelId = ""

    inner class LocalBinder : Binder() {
        fun getService(): VoiceService = this@VoiceService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        channelId = intent?.getStringExtra("channel_id") ?: ""
        startForeground(1, buildNotification("Voice Chat Active"))
        return START_STICKY
    }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        return isMuted
    }

    fun isMuted(): Boolean = isMuted

    fun joinChannel(channelId: String) {
        this.channelId = channelId
        // Voice channel join logic via Firebase
    }

    fun leaveChannel() {
        // Voice channel leave logic via Firebase
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "voice_channel", "Voice Chat",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Voice chat in progress" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, "voice_channel")
            .setContentTitle("ZERO STRESS Voice")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        leaveChannel()
        super.onDestroy()
    }
}
