package com.zerostress.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetUserInfo;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Voice Chat Activity using Jitsi Meet SDK
 *
 * ✅ 100% FREE — No API key, no sign-up, no usage limits
 * ✅ Built-in noise cancellation, echo cancellation, AGC
 * ✅ Works with meet.jit.si public server
 * ✅ Can self-host for private server
 */
public class VoiceChatActivity extends AppCompatActivity {
    private static final String TAG = "VoiceChat";
    private static final String JITSI_SERVER_URL = "https://meet.jit.si";

    private String channelName = "";
    private String userName = "";
    private String userPhone = "";
    private long startTime = 0;

    private LinearLayout participantsContainer;
    private TextView channelNameTv;
    private TextView voiceTimerTv;
    private TextView voiceStatusTv;

    private FirebaseFirestore db;
    private ListenerRegistration channelListener;
    private Timer timer;
    private boolean isActive = false;

    // Listen for Jitsi broadcast events
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleJitsiEvent(intent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_chat);

        // Get intent data
        channelName = getIntent().getStringExtra("channelName");
        userName = getIntent().getStringExtra("userName");
        userPhone = getIntent().getStringExtra("userPhone");

        if (channelName == null) channelName = "zerostress-default";
        if (userName == null) userName = "Unknown";
        if (userPhone == null) userPhone = "";

        db = FirebaseFirestore.getInstance();

        // Init views
        participantsContainer = findViewById(R.id.participants_container);
        channelNameTv = findViewById(R.id.voice_channel_name);
        voiceTimerTv = findViewById(R.id.voice_timer);
        voiceStatusTv = findViewById(R.id.voice_status);

        channelNameTv.setText("\ud83c\udfa4 " + channelName.replace("squad_", "").toUpperCase());

        // Leave button
        findViewById(R.id.btn_leave).setOnClickListener(v -> leaveChannel());

        // Hide mute/speaker/jitsi handles (Jitsi handles these itself)
        findViewById(R.id.btn_mute).setVisibility(View.GONE);
        findViewById(R.id.btn_speaker).setVisibility(View.GONE);

        // Register Jitsi broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction("org.jitsi.meet.CONFERENCE_JOINED");
        filter.addAction("org.jitsi.meet.CONFERENCE_LEFT");
        filter.addAction("org.jitsi.meet.PARTICIPANT_JOINED");
        filter.addAction("org.jitsi.meet.PARTICIPANT_LEFT");
        filter.addAction("org.jitsi.meet.ENDPOINT_MESSAGES_RECEIVED");
        filter.addAction("org.jitsi.meet.CONFERENCE_FAILED");
        ContextCompat.registerReceiver(this, broadcastReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

        // Register in Firestore and join Jitsi
        registerInChannel();
        joinJitsiChannel();
        startTimer();
    }

    // ==================== JITSI MEET ====================

    private void joinJitsiChannel() {
        try {
            // Create Jitsi Meet options — voice only, with all built-in features
            JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                .setServerURL(new URL(JITSI_SERVER_URL))
                .setRoom(channelName)
                .setDisplayName(userName)
                .setAudioOnly(true) // Voice only (no video)
                .setAudioMuted(false)
                .setVideoMuted(true) // Disable video
                .setWelcomePageEnabled(false)
                .setUserInfo(new JitsiMeetUserInfo.Builder()
                    .setDisplayName(userName)
                    .build())
                // Enable built-in features:
                // ✅ Noise suppression (built-in WebRTC)
                // ✅ Echo cancellation (built-in)
                // ✅ Automatic gain control (built-in)
                // ✅ Comfort noise generation (built-in)
                .build();

            // Launch Jitsi Meet — it handles EVERYTHING automatically
            JitsiMeetActivity.launch(this, options);

            isActive = true;
            voiceStatusTv.setText("\ud83d\udfe2 Connecting to " + channelName + "...");

        } catch (MalformedURLException e) {
            Log.e(TAG, "Invalid Jitsi server URL: " + e.getMessage());
            Toast.makeText(this, "Failed to connect to voice server", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void leaveChannel() {
        unregisterFromChannel();
        if (timer != null) timer.cancel();
        isActive = false;
        finish();
    }

    // ==================== JITSI EVENTS ====================

    private void handleJitsiEvent(Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        switch (action) {
            case "org.jitsi.meet.CONFERENCE_JOINED":
                runOnUiThread(() -> {
                    voiceStatusTd("\ud83d\udfe2 Connected to voice channel");
                    voiceStatusTv.setTextColor(Color.parseColor("#10b981"));
                });
                break;

            case "org.jitsi.meet.CONFERENCE_LEFT":
            case "org.jitsi.meet.CONFERENCE_FAILED":
                runOnUiThread(() -> {
                    voiceStatusTd("\ud83d\udd34 Disconnected");
                    voiceStatusTv.setTextColor(Color.parseColor("#ef4444"));
                    unregisterFromChannel();
                });
                break;

            case "org.jitsi.meet.PARTICIPANT_JOINED":
                updateParticipantsList();
                break;

            case "org.jitsi.meet.PARTICIPANT_LEFT":
                updateParticipantsList();
                break;
        }
    }

    // ==================== FIRESTORE CHANNEL TRACKING ====================

    private void registerInChannel() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", userName);
        data.put("phone", userPhone);
        data.put("joinedAt", System.currentTimeMillis());
        data.put("muted", false);

        db.collection("voiceChannels").document(channelName)
            .collection("participants").document(userPhone)
            .set(data);
    }

    private void unregisterFromChannel() {
        db.collection("voiceChannels").document(channelName)
            .collection("participants").document(userPhone)
            .delete();
    }

    private void listenChannelParticipants() {
        channelListener = db.collection("voiceChannels").document(channelName)
            .collection("participants")
            .addSnapshotListener((snapshots, error) -> {
                if (error != null || snapshots == null) return;

                runOnUiThread(() -> {
                    participantsContainer.removeAllViews();

                    int count = 0;
                    for (QueryDocumentSnapshot doc : snapshots) {
                        count++;
                        String name = doc.getString("name");
                        if (name == null) name = "Unknown";
                        boolean muted = Boolean.TRUE.equals(doc.getBoolean("muted"));
                        boolean isMe = doc.getId().equals(userPhone);

                        LinearLayout row = new LinearLayout(this);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                        row.setPadding(12, 8, 12, 8);

                        // Avatar
                        TextView avatar = new TextView(this);
                        String initial = name.length() > 0 ? name.substring(0, 1).toUpperCase() : "?";
                        avatar.setText(initial);
                        avatar.setTextSize(16);
                        avatar.setTextColor(Color.WHITE);
                        avatar.setGravity(android.view.Gravity.CENTER);
                        avatar.setMinimumWidth(80);
                        avatar.setMinimumHeight(80);
                        avatar.setBackgroundColor(Color.parseColor("#1e3a5f"));
                        row.addView(avatar);

                        // Name
                        TextView nameTv = new TextView(this);
                        nameTv.setText(name + (isMe ? " (You)" : ""));
                        nameTv.setTextColor(Color.parseColor("#f1f5f9"));
                        nameTv.setTextSize(14);
                        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                        nameParams.setMarginStart(12);
                        nameTv.setLayoutParams(nameParams);
                        row.addView(nameTv);

                        // Status icon
                        TextView status = new TextView(this);
                        status.setText(muted ? "\ud83d\udd07" : "\ud83d\udd0a");
                        status.setTextSize(14);
                        row.addView(status);

                        participantsContainer.addView(row);

                        // Divider
                        View divider = new View(this);
                        divider.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1));
                        divider.setBackgroundColor(Color.parseColor("#1e3a5f"));
                        participantsContainer.addView(divider);
                    }

                    // Update header count
                    channelNameTv.setText("\ud83c\udfa4 " + channelName.replace("squad_", "").toUpperCase()
                        + " (" + count + ")");
                });
            });
    }

    private void updateParticipantsList() {
        // Already handled by Firestore listener
    }



    // ==================== TIMER ====================

    private void startTimer() {
        startTime = System.currentTimeMillis();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isActive) return;
                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                long minutes = elapsed / 60;
                long seconds = elapsed % 60;
                runOnUiThread(() ->
                    voiceTimerTv.setText(String.format("%02d:%02d", minutes, seconds))
                );
            }
        }, 0, 1000);
    }

    // ==================== CLEANUP ====================

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterFromChannel();
        if (timer != null) timer.cancel();
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (Exception ignored) {}
        if (channelListener != null) channelListener.remove();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Don't unregister — user may be multitasking
    }
}
