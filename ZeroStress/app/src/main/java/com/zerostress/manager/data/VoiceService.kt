package com.zerostress.manager.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.zerostress.manager.MainActivity
import com.zerostress.manager.R
import org.webrtc.*
import java.util.concurrent.Executors

class VoiceService : Service() {

    private val binder = LocalBinder()
    private var peerConnection: PeerConnection? = null
    private var audioTrack: AudioTrack? = null
    private var factory: PeerConnectionFactory? = null
    private val executor = Executors.newSingleThreadExecutor()
    private var isMuted = false
    private var channelId = ""

    inner class LocalBinder : Binder() {
        fun getService(): VoiceService = this@VoiceService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initWebRTC()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        channelId = intent?.getStringExtra("channel_id") ?: ""
        startForeground(1, buildNotification("Voice Chat Active"))
        return START_STICKY
    }

    private fun initWebRTC() {
        val initOptions = PeerConnectionFactory.InitializationOptions.builder(this)
            .setEnableInternalTracer(false)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initOptions)

        val options = PeerConnectionFactory.Options()
        factory = PeerConnectionFactory.builder()
            .setOptions(options)
            .createPeerConnectionFactory()

        val audioSource = factory!!.createAudioSource(MediaConstraints())
        audioTrack = factory!!.createAudioTrack("audio_track", audioSource)
    }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        audioTrack?.setEnabled(!isMuted)
        return isMuted
    }

    fun isMuted(): Boolean = isMuted

    fun joinChannel(channelId: String) {
        this.channelId = channelId
        // WebRTC signaling would connect to Firebase here
        audioTrack?.setEnabled(true)
    }

    fun leaveChannel() {
        audioTrack?.setEnabled(false)
        peerConnection?.close()
        peerConnection = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        factory?.dispose()
        PeerConnectionFactory.dispose()
        super.onDestroy()
    }
}
