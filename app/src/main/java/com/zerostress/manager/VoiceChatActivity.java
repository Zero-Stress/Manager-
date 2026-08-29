package com.zerostress.manager;

import android.Manifest;
import android.content.pm.PackageManager;
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

import java.util.HashMap;
import java.util.Map;

import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.audio.AudioParams;
import io.agora.rtc.models.AudioVolumeInfo;

public class VoiceChatActivity extends AppCompatActivity {
    private static final String TAG = "VoiceChat";
    private static final int PERMISSION_REQUEST_CODE = 200;

    // =====================================================
    // 🔑 AGORA APP ID — Replace with your own from agora.io
    // Sign up free at https://console.agora.io
    // Free tier: 10,000 minutes/month
    // =====================================================
    private static final String AGORA_APP_ID = "YOUR_AGORA_APP_ID_HERE";

    private RtcEngine agoraEngine;
    private IRtcEngineEventHandler rtcEventHandler;

    private String channelName = "";
    private String userName = "";
    private String userPhone = "";
    private boolean isMuted = false;
    private boolean isSpeakerOn = true;
    private boolean isNoiseCancellationOn = true;
    private long startTime = 0;

    private LinearLayout participantsContainer;
    private TextView channelNameTv;
    private TextView voiceTimerTv;
    private TextView voiceStatusTv;
    private TextView muteIconTv;
    private TextView muteLabelTv;
    private TextView speakerIconTv;
    private TextView speakerLabelTv;
    private SwitchCompat noiseCancelSwitch;

    private FirebaseFirestore db;
    private ListenerRegistration channelListener;
    private Handler timerHandler;
    private Runnable timerRunnable;

    // Track online users in channel
    private final Map<String, String> channelUsers = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_chat);

        // Get intent data
        channelName = getIntent().getStringExtra("channelName");
        userName = getIntent().getStringExtra("userName");
        userPhone = getIntent().getStringExtra("userPhone");

        if (channelName == null) channelName = "default";
        if (userName == null) userName = "Unknown";
        if (userPhone == null) userPhone = "";

        db = FirebaseFirestore.getInstance();

        // Init views
        participantsContainer = findViewById(R.id.participants_container);
        channelNameTv = findViewById(R.id.voice_channel_name);
        voiceTimerTv = findViewById(R.id.voice_timer);
        voiceStatusTv = findViewById(R.id.voice_status);
        muteIconTv = findViewById(R.id.mute_icon);
        muteLabelTv = findViewById(R.id.mute_label);
        speakerIconTv = findViewById(R.id.speaker_icon);
        speakerLabelTv = findViewById(R.id.speaker_label);
        noiseCancelSwitch = findViewById(R.id.noise_cancel_switch);

        channelNameTv.setText("🎤 " + channelName);

        // Control buttons
        findViewById(R.id.btn_mute).setOnClickListener(v -> toggleMute());
        findViewById(R.id.btn_speaker).setOnClickListener(v -> toggleSpeaker());
        findViewById(R.id.btn_leave).setOnClickListener(v -> leaveChannel());
        noiseCancelSwitch.setOnCheckedChangeListener((btn, checked) -> {
            isNoiseCancellationOn = checked;
            applyNoiseCancellation();
        });

        // Check permissions and init
        if (checkAudioPermission()) {
            initAgoraEngine();
            joinChannel();
        }

        // Listen to channel participants
        listenChannelParticipants();

        // Start timer
        startTimer();
    }

    // ==================== PERMISSIONS ====================

    private boolean checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initAgoraEngine();
                joinChannel();
            } else {
                Toast.makeText(this, "Microphone permission required for voice chat", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    // ==================== AGORA ENGINE ====================

    private void initAgoraEngine() {
        try {
            // Create Agora engine
            agoraEngine = RtcEngine.create(getApplicationContext(), AGORA_APP_ID, new IRtcEngineEventHandler() {
                @Override
                public void onUserJoined(int uid, int elapsed) {
                    Log.d(TAG, "User joined: " + uid);
                    runOnUiThread(() -> updateParticipantsList());
                }

                @Override
                public void onUserOffline(int uid, int reason) {
                    Log.d(TAG, "User left: " + uid);
                    runOnUiThread(() -> updateParticipantsList());
                }

                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    Log.d(TAG, "Joined channel: " + channel + " uid: " + uid);
                    runOnUiThread(() -> {
                        voiceStatusTv.setText("🟢 Connected to " + channelName);
                        voiceStatusTv.setTextColor(Color.parseColor("#10b981"));
                    });
                }

                @Override
                public void onLeaveChannel(IRtcEngineEventHandler.RtcStats stats) {
                    Log.d(TAG, "Left channel");
                    runOnUiThread(() -> {
                        voiceStatusTv.setText("🔴 Disconnected");
                        voiceStatusTv.setTextColor(Color.parseColor("#ef4444"));
                    });
                }

                @Override
                public void onActiveSpeaker(int uid) {
                    // Highlight who's speaking
                    runOnUiThread(() -> highlightSpeaker(uid));
                }
            });

            // ========== AUDIO QUALITY SETTINGS ==========

            // Set audio profile: High quality voice
            agoraEngine.setAudioProfile(
                io.agora.rtc.Constants.AUDIO_PROFILE_MUSIC_HIGH_QUALITY_STEREO,
                io.agora.rtc.Constants.AUDIO_SCENARIO_GAME_STREAMING
            );

            // Enable AEC (Acoustic Echo Cancellation)
            agoraEngine.setParameters("{\"che.audio.aec.enable\":true}");

            // Enable AGC (Automatic Gain Control) - keeps volume consistent
            agoraEngine.setParameters("{\"che.audio.agc.enable\":true}");

            // Enable NS (Noise Suppression) - removes background noise
            agoraEngine.setParameters("{\"che.audio.ns.enable\":true}");

            // Enable AI Noise Cancellation (if available in SDK version)
            try {
                agoraEngine.setParameters("{\"che.audio.enable.aianc\":true}");
                Log.d(TAG, "AI Noise Cancellation enabled");
            } catch (Exception e) {
                Log.d(TAG, "AI Noise Cancellation not available, using standard NS");
            }

            // Set channel profile to Communication (2-way voice)
            agoraEngine.setChannelProfile(io.agora.rtc.Constants.CHANNEL_PROFILE_COMMUNICATION);

            // Enable audio module
            agoraEngine.enableAudio();

            Log.d(TAG, "Agora engine initialized with noise cancellation");

        } catch (Exception e) {
            Log.e(TAG, "Failed to init Agora engine: " + e.getMessage());
            Toast.makeText(this, "Voice chat initialization failed", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // ==================== CHANNEL MANAGEMENT ====================

    private void joinChannel() {
        if (agoraEngine == null) return;

        if (AGORA_APP_ID.equals("YOUR_AGORA_APP_ID_HERE")) {
            Toast.makeText(this,
                "⚠️ Add your Agora App ID first!\n\n1. Sign up at agora.io (free)\n2. Create a project\n3. Copy App ID\n4. Paste in VoiceChatActivity.java",
                Toast.LENGTH_LONG).show();
            voiceStatusTv.setText("⚠️ Need Agora App ID");
            voiceStatusTv.setTextColor(Color.parseColor("#f59e0b"));
            return;
        }

        try {
            // Join channel with token (null for testing)
            agoraEngine.joinChannel(null, channelName, "", 0);

            // Register user in Firestore channel
            registerInChannel();

            startTime = System.currentTimeMillis();

        } catch (Exception e) {
            Log.e(TAG, "Failed to join channel: " + e.getMessage());
        }
    }

    private void leaveChannel() {
        // Unregister from Firestore
        unregisterFromChannel();

        // Leave Agora channel
        if (agoraEngine != null) {
            agoraEngine.leaveChannel();
        }

        // Stop timer
        if (timerHandler != null) timerHandler.removeCallbacks(timerRunnable);

        finish();
    }

    // ==================== AUDIO CONTROLS ====================

    private void toggleMute() {
        isMuted = !isMuted;
        if (agoraEngine != null) {
            agoraEngine.muteLocalAudioStream(isMuted);
        }
        muteIconTv.setText(isMuted ? "🔇" : "🎤");
        muteLabelTv.setText(isMuted ? "Unmute" : "Mute");
        muteLabelTv.setTextColor(isMuted ? Color.parseColor("#ef4444") : Color.parseColor("#94a3b8"));

        // Update in Firestore
        updateMuteStatus(isMuted);
    }

    private void toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn;
        if (agoraEngine != null) {
            agoraEngine.setEnableSpeakerphone(isSpeakerOn);
        }
        speakerIconTv.setText(isSpeakerOn ? "🔊" : "🔈");
        speakerLabelTv.setText(isSpeakerOn ? "Speaker" : "Earpiece");
        speakerLabelTv.setTextColor(isSpeakerOn ? Color.parseColor("#38bdf8") : Color.parseColor("#94a3b8"));
    }

    private void applyNoiseCancellation() {
        if (agoraEngine == null) return;

        if (isNoiseCancellationOn) {
            // Enable full noise cancellation
            agoraEngine.setParameters("{\"che.audio.ns.enable\":true}");
            agoraEngine.setParameters("{\"che.audio.aec.enable\":true}");
            agoraEngine.setParameters("{\"che.audio.agc.enable\":true}");
            try {
                agoraEngine.setParameters("{\"che.audio.enable.aianc\":true}");
            } catch (Exception ignored) {}
            Toast.makeText(this, "🔇 Noise cancellation ON", Toast.LENGTH_SHORT).show();
        } else {
            // Disable noise cancellation
            agoraEngine.setParameters("{\"che.audio.ns.enable\":false}");
            try {
                agoraEngine.setParameters("{\"che.audio.enable.aianc\":false}");
            } catch (Exception ignored) {}
            Toast.makeText(this, "🎤 Noise cancellation OFF", Toast.LENGTH_SHORT).show();
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

    private void updateMuteStatus(boolean muted) {
        Map<String, Object> update = new HashMap<>();
        update.put("muted", muted);

        db.collection("voiceChannels").document(channelName)
            .collection("participants").document(userPhone)
            .update(update);
    }

    private void listenChannelParticipants() {
        channelListener = db.collection("voiceChannels").document(channelName)
            .collection("participants")
            .addSnapshotListener((snapshots, error) -> {
                if (error != null || snapshots == null) return;

                channelUsers.clear();
                for (QueryDocumentSnapshot doc : snapshots) {
                    String name = doc.getString("name");
                    Boolean muted = doc.getBoolean("muted");
                    if (name != null) {
                        channelUsers.put(doc.getId(), name + "|" + (muted != null && muted));
                    }
                }
                updateParticipantsList();
            });
    }

    private void updateParticipantsList() {
        participantsContainer.removeAllViews();

        if (channelUsers.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Waiting for others to join...");
            empty.setTextColor(Color.parseColor("#94a3b8"));
            empty.setTextSize(14);
            empty.setPadding(16, 32, 16, 32);
            participantsContainer.addView(empty);
            return;
        }

        for (Map.Entry<String, String> entry : channelUsers.entrySet()) {
            String[] parts = entry.getValue().split("\\|");
            String name = parts[0];
            boolean muted = parts.length > 1 && Boolean.parseBoolean(parts[1]);
            boolean isMe = entry.getKey().equals(userPhone);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(12, 8, 12, 8);

            // Avatar circle
            TextView avatar = new TextView(this);
            String initial = name.length() > 0 ? name.substring(0, 1).toUpperCase() : "?";
            avatar.setText(initial);
            avatar.setTextSize(16);
            avatar.setTextColor(Color.WHITE);
            avatar.setGravity(android.view.Gravity.CENTER);
            avatar.setWidth(100);
            avatar.setHeight(100);
            avatar.setBackgroundColor(Color.parseColor("#1e3a5f"));
            row.addView(avatar);

            LinearLayout.LayoutParams avatarParams = (LinearLayout.LayoutParams) avatar.getLayoutParams();
            avatarParams.setMargins(0, 0, 12, 0);

            // Name
            TextView nameTv = new TextView(this);
            nameTv.setText(name + (isMe ? " (You)" : ""));
            nameTv.setTextColor(Color.parseColor("#f1f5f9"));
            nameTv.setTextSize(14);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            nameTv.setLayoutParams(nameParams);
            row.addView(nameTv);

            // Speaking indicator
            TextView speakIndicator = new TextView(this);
            speakIndicator.setText(muted ? "🔇" : "🔊");
            speakIndicator.setTextSize(14);
            row.addView(speakIndicator);

            participantsContainer.addView(row);

            // Divider
            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(Color.parseColor("#1e3a5f"));
            participantsContainer.addView(divider);
        }
    }

    private void highlightSpeaker(int uid) {
        // Update visual feedback for active speaker
        updateParticipantsList();
    }

    // ==================== TIMER ====================

    private void startTimer() {
        timerHandler = new Handler(Looper.getMainLooper());
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (startTime > 0) {
                    long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                    long minutes = elapsed / 60;
                    long seconds = elapsed % 60;
                    voiceTimerTv.setText(String.format("%02d:%02d", minutes, seconds));
                }
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    // ==================== CLEANUP ====================

    @Override
    protected void onDestroy() {
        super.onDestroy();
        leaveChannel();
        if (agoraEngine != null) {
            RtcEngine.destroy();
            agoraEngine = null;
        }
        if (channelListener != null) channelListener.remove();
        if (timerHandler != null) timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Mute self when app goes to background
        if (agoraEngine != null && !isMuted) {
            agoraEngine.muteLocalAudioStream(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Unmute when coming back
        if (agoraEngine != null && !isMuted) {
            agoraEngine.muteLocalAudioStream(false);
        }
    }
}
