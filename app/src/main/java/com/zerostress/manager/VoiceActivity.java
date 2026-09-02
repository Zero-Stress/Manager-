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

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VoiceActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private RecyclerView rvChannels;
    private TextView tvStatus;
    private ProgressBar progressBar;
    private MaterialButton btnJoinLeave;
    private FirebaseFirestore db;
    private String userId, userName;
    private boolean isInVoice = false;
    private String currentChannelId = null;

    // WebRTC
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private EglBase eglBase;
    private AudioTrack localAudioTrack;

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

        loadChannels();
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

    private void initWebRTC() {
        eglBase = EglBase.create();
        PeerConnectionFactory.InitializationOptions initOptions = PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions();
        PeerConnectionFactory.initialize(initOptions);

        peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBase.getEglBaseContext()))
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true))
                .createPeerConnectionFactory();

        MediaConstraints audioConstraints = new MediaConstraints();
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googAutoGainControl", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googHighpassFilter", "true"));

        AudioSource audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
        localAudioTrack = peerConnectionFactory.createAudioTrack("local_audio", audioSource);
        localAudioTrack.setEnabled(true);
    }

    private void joinVoiceChannel() {
        String channelId = "general_voice";
        currentChannelId = channelId;

        initWebRTC();

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
                btnJoinLeave.setText("🔴 Leave Voice");
                btnJoinLeave.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.danger));
                tvStatus.setText("🟢 Connected to Voice Channel");
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.success));
                Toast.makeText(this, "Joined voice channel!", Toast.LENGTH_SHORT).show();

                listenToParticipants(channelId);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to join: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void listenToParticipants(String channelId) {
        db.collection("voice_channels").document(channelId)
            .collection("participants")
            .addSnapshotListener((snapshots, e) -> {
                if (e != null || snapshots == null) return;
                int count = snapshots.size();
                tvStatus.setText("🟢 Connected — " + count + " participant" + (count != 1 ? "s" : ""));
            });
    }

    private void leaveVoice() {
        if (currentChannelId != null) {
            db.collection("voice_channels").document(currentChannelId)
                .collection("participants").document(userId)
                .delete();
        }

        if (peerConnection != null) {
            peerConnection.close();
            peerConnection = null;
        }
        if (localAudioTrack != null) {
            localAudioTrack.dispose();
            localAudioTrack = null;
        }

        isInVoice = false;
        currentChannelId = null;
        btnJoinLeave.setText("🎙️ Join Voice");
        btnJoinLeave.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.accent));
        tvStatus.setText("🔴 Not connected");
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isInVoice) leaveVoice();
    }
}
