package com.zerostress.manager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Real-time Voice Chat using Jitsi Meet Web Client via WebView
 *
 * No SDK dependency needed — loads Jitsi's web interface directly.
 * Features: noise cancellation, echo cancellation, auto gain control — all built into WebRTC.
 */
public class VoiceChatActivity extends AppCompatActivity {
    private static final String TAG = "VoiceChat";

    private static final String JITSI_SERVER_URL = "https://meet.jit.si";

    private String channelName = "";
    private String userName = "";
    private String userPhone = "";
    private long startTime = 0;
    private boolean isInCall = false;

    private LinearLayout participantsContainer;
    private TextView channelNameTv;
    private TextView voiceTimerTv;
    private TextView voiceStatusTv;
    private WebView webView;

    private FirebaseFirestore db;
    private ListenerRegistration channelListener;
    private Timer timer;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @SuppressLint("SetJavaScriptEnabled")
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

        // Sanitize channel name for Jitsi (only alphanumeric, hyphens, underscores)
        channelName = channelName.replaceAll("[^a-zA-Z0-9_-]", "-");

        db = FirebaseFirestore.getInstance();

        // Init views
        participantsContainer = findViewById(R.id.participants_container);
        channelNameTv = findViewById(R.id.voice_channel_name);
        voiceTimerTv = findViewById(R.id.voice_timer);
        voiceStatusTv = findViewById(R.id.voice_status);
        webView = findViewById(R.id.webview);

        updateStatus("\uD83D\uDFE1 Connecting...");
        channelNameTv.setText("\uD83C\uDFA4 " + channelName.replace("squad_", "").toUpperCase());

        // Leave button
        findViewById(R.id.btn_leave).setOnClickListener(v -> leaveChannel());

        // Check network
        if (checkNetwork()) {
            setupWebView();
            registerInChannel();
            listenChannelParticipants();
            startTimer();
        }
    }

    private boolean checkNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            showError("No network available");
            return false;
        }
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork == null || !activeNetwork.isConnectedOrConnecting()) {
            showError("No internet connection");
            return false;
        }
        return true;
    }

    private void showError(String message) {
        updateStatus("\uD83D\uDD34 " + message);
        voiceStatusTv.setTextColor(Color.parseColor("#ef4444"));
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        handler.postDelayed(this::finish, 3000);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                updateStatus("\uD83D\uDFE2 LIVE — Voice chat active");
                voiceStatusTv.setTextColor(Color.parseColor("#10b981"));
                isInCall = true;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Log.e(TAG, "WebView error: " + description);
                attemptReconnect();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }
        });

        updateStatus("\uD83D\uDFE1 Joining voice channel...");

        // Build Jitsi Meet URL with config parameters for voice-only, low-latency
        String roomUrl = JITSI_SERVER_URL + "/" + channelName
            + "#config.startAudioOnly=true"
            + "&config.startVideoMuted=true"
            + "&config.prejoinPageEnabled=false"
            + "&config.disableDeepLinking=true"
            + "&config.toolbarEnabled=false"
            + "&config.notifications.enabled=true"
            + "&userInfo.displayName=" + userName;

        webView.loadUrl(roomUrl);
    }

    private void leaveChannel() {
        isInCall = false;
        unregisterFromChannel();
        if (timer != null) timer.cancel();
        handler.removeCallbacksAndMessages(null);
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.destroy();
        }
        finish();
    }

    private void attemptReconnect() {
        updateStatus("\uD83D\uDD04 Reconnecting...");
        voiceStatusTv.setTextColor(Color.parseColor("#f59e0b"));
        handler.postDelayed(() -> {
            if (!isFinishing()) {
                setupWebView();
            }
        }, 2000);
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
        if (userPhone.isEmpty()) return;
        db.collection("voiceChannels").document(channelName)
            .collection("participants").document(userPhone)
            .delete();
    }

    private void listenChannelParticipants() {
        channelListener = db.collection("voiceChannels").document(channelName)
            .collection("participants")
            .addSnapshotListener((snapshots, error) -> {
                if (error != null || snapshots == null) return;
                runOnUiThread(() -> renderParticipants(snapshots));
            });
    }

    private void renderParticipants(com.google.firebase.firestore.QuerySnapshot snapshots) {
        participantsContainer.removeAllViews();
        int count = 0;

        for (QueryDocumentSnapshot doc : snapshots) {
            count++;
            String name = doc.getString("name");
            if (name == null) name = "Unknown";
            Boolean muted = doc.getBoolean("muted");
            boolean isMuted = muted != null && muted;
            boolean isMe = doc.getId().equals(userPhone);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(12, 10, 12, 10);

            // Avatar circle
            TextView avatar = new TextView(this);
            String initial = name.length() > 0 ? name.substring(0, 1).toUpperCase() : "?";
            avatar.setText(initial);
            avatar.setTextSize(18);
            avatar.setTextColor(Color.WHITE);
            avatar.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(90, 90);
            avatarLp.setMarginEnd(12);
            avatar.setLayoutParams(avatarLp);
            avatar.setBackgroundColor(Color.parseColor(isMe ? "#38bdf8" : "#1e3a5f"));
            row.addView(avatar);

            // Name
            TextView nameTv = new TextView(this);
            nameTv.setText(name + (isMe ? " (You)" : ""));
            nameTv.setTextColor(Color.parseColor("#f1f5f9"));
            nameTv.setTextSize(14);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            nameTv.setLayoutParams(nameParams);
            row.addView(nameTv);

            // Mic status
            TextView micStatus = new TextView(this);
            micStatus.setText(isMuted ? "\uD83D\uDD07" : "\uD83C\uDF99\uFE0F");
            micStatus.setTextSize(16);
            row.addView(micStatus);

            participantsContainer.addView(row);

            // Divider
            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(Color.parseColor("#1e3a5f"));
            participantsContainer.addView(divider);
        }

        channelNameTv.setText("\uD83C\uDFA4 " + channelName.replace("squad_", "").toUpperCase()
            + " (" + count + " in voice)");
    }

    private void updateStatus(String text) {
        voiceStatusTv.setText(text);
    }

    // ==================== TIMER ====================

    private void startTimer() {
        startTime = System.currentTimeMillis();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isInCall) return;
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
        handler.removeCallbacksAndMessages(null);
        if (channelListener != null) channelListener.remove();
        try {
            if (webView != null) {
                webView.loadUrl("about:blank");
                webView.destroy();
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onBackPressed() {
        leaveChannel();
    }
}
