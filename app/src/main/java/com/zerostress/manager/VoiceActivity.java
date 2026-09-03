package com.zerostress.manager;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.models.ChannelMediaOptions;

public class VoiceActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private RecyclerView rvChannels;
    private TextView tvStatus;
    private ProgressBar progressBar;
    private MaterialButton btnJoinLeave, btnMute;
    private FirebaseFirestore db;
    private String userId, userName;
    private boolean isInVoice = false;
    private boolean isMuted = false;
    private String currentChannelId = null;

    // Agora SDK
    private RtcEngine mRtcEngine;
    private boolean isEngineInitialized = false;

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            runOnUiThread(() -> {
                tvStatus.setText("🟢 Connected — You are in voice chat");
                tvStatus.setTextColor(ContextCompat.getColor(VoiceActivity.this, R.color.success));
            });
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            runOnUiThread(() -> {
                tvStatus.setText("🟢 Connected — " + (int)(tvStatus.getTag() != null ? (int)tvStatus.getTag() : 0 + 1) + " participants");
            });
        }

        @Override
        public void onUserOffline(int uid, UserOfflineReason reason) {
            runOnUiThread(() -> {
                tvStatus.setText("🟢 Connected — User left");
            });
        }

        @Override
        public void onLeaveChannel(RtcStats stats) {
            runOnUiThread(() -> {
                tvStatus.setText("🔴 Not connected");
                tvStatus.setTextColor(ContextCompat.getColor(VoiceActivity.this, R.color.text_secondary));
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        rvChannels = findViewById(R.id.rvChannels);
        tvStatus = findViewById(R.id.tvVoiceStatus);
        progressBar = findViewById(R.id.progressBar);
        btnJoinLeave = findViewById(R.id.btnJoinLeave);
        btnMute = findViewById(R.id.btnMute);

        rvChannels.setLayoutManager(new LinearLayoutManager(this));

        db.collection("players").document(userId).get()
            .addOnSuccessListener(doc -> {
                userName = doc.getString("name");
                if (userName == null) userName = "Unknown";
            });

        btnJoinLeave.setOnClickListener(v -> {
            if (isInVoice) {
                leaveVoice();
            } else {
                checkPermissionsAndJoin();
            }
        });

        btnMute.setOnClickListener(v -> toggleMute());

        loadChannels();
    }

    private void initAgoraEngine() {
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), AgoraConfig.APP_ID, mRtcEventHandler);
            mRtcEngine.enableAudio();
            isEngineInitialized = true;
        } catch (Exception e) {
            Toast.makeText(this, "Failed to initialize voice engine: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadChannels() {
        db.collection("voice_channels")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<String> channels = new ArrayList<>();
                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    String channelName = doc.getString("name");
                    if (channelName != null) {
                        channels.add(channelName);
                    }
                }
                if (channels.isEmpty()) {
                    tvStatus.setText("No voice channels available");
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load channels", Toast.LENGTH_SHORT).show();
            });
    }

    private void checkPermissionsAndJoin() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
        } else {
            joinVoiceChannel();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            joinVoiceChannel();
        } else {
            Toast.makeText(this, "Microphone permission required for voice chat", Toast.LENGTH_SHORT).show();
        }
    }

    private void joinVoiceChannel() {
        if (!isEngineInitialized) {
            initAgoraEngine();
        }

        if (mRtcEngine == null) {
            Toast.makeText(this, "Voice engine not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        String channelId = AgoraConfig.CHANNEL;
        currentChannelId = channelId;

        // Join Agora channel
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.channelProfile = io.agora.rtc.Constants.CHANNEL_PROFILE_COMMUNICATION;
        options.audioScenario = io.agora.rtc.Constants.AUDIO_SCENARIO_CHATROOM;

        mRtcEngine.joinChannel(AgoraConfig.TOKEN, channelId, 0, options);

        // Save to Firestore
        Map<String, Object> participant = new HashMap<>();
        participant.put("userId", userId);
        participant.put("userName", userName);
        participant.put("joinedAt", System.currentTimeMillis());
        participant.put("muted", false);

        db.collection("voice_channels").document(channelId)
            .collection("participants").document(userId)
            .set(participant)
            .addOnSuccessListener(v -> {
                isInVoice = true;
                isMuted = false;
                btnJoinLeave.setText("🔴 Leave Voice");
                btnJoinLeave.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.danger));
                btnMute.setVisibility(View.VISIBLE);
                btnMute.setText("🔇 Unmute");
                Toast.makeText(this, "Joined voice channel!", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to join: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void toggleMute() {
        if (mRtcEngine == null) return;

        isMuted = !isMuted;
        mRtcEngine.muteLocalAudioStream(isMuted);
        btnMute.setText(isMuted ? "🔊 Unmute" : "🔇 Mute");
    }

    private void leaveVoice() {
        if (mRtcEngine != null) {
            mRtcEngine.leaveChannel();
        }

        if (currentChannelId != null) {
            db.collection("voice_channels").document(currentChannelId)
                .collection("participants").document(userId)
                .delete();
        }

        isInVoice = false;
        isMuted = false;
        currentChannelId = null;
        btnJoinLeave.setText("🎙️ Join Voice");
        btnJoinLeave.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.accent));
        btnMute.setVisibility(View.GONE);
        tvStatus.setText("🔴 Not connected");
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isInVoice) leaveVoice();
        if (mRtcEngine != null) {
            mRtcEngine.destroy();
            mRtcEngine = null;
            isEngineInitialized = false;
        }
    }
}
