package com.zerostress.manager;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VoiceActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private TextView tvStatus, tvChannelName, tvTimer, tvUserStatus;
    private View tvAdminPanel;
    private ProgressBar progressBar;
    private MaterialButton btnJoinLeave, btnMute, btnDeafen, btnScreenShare, 
        btnPushToTalk, btnHandRaise, btnMoreOptions, btnManagePermissions, btnCreateChannel;
    private ImageButton btnBack;
    private RecyclerView rvUsers, rvChat;
    private EditText etChatMessage;
    private View chatContainer;
    private FirebaseFirestore db;
    private String userId, userName, userStatus = "ONLINE";
    private boolean isInVoice = false;
    private boolean isMuted = false;
    private boolean isDeafened = false;
    private boolean isScreenSharing = false;
    private boolean isHandRaised = false;
    private boolean isPushToTalk = false;
    private boolean isSpeaking = false;
    private boolean isAdmin = false;
    private String currentChannelId = null;
    private long startTime;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private List<VoiceUserInfo> voiceUsers = new ArrayList<>();
    private List<ChatMessageInfo> chatMessages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_discord);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        if (userId == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        checkUserRole();
        loadVoiceChannels();
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tvVoiceStatus);
        tvChannelName = findViewById(R.id.tvChannelName);
        tvTimer = findViewById(R.id.tvTimer);
        tvUserStatus = findViewById(R.id.tvUserStatus);
        tvAdminPanel = findViewById(R.id.tvAdminPanel);
        progressBar = findViewById(R.id.progressBar);
        btnJoinLeave = findViewById(R.id.btnJoinLeave);
        btnMute = findViewById(R.id.btnMute);
        btnDeafen = findViewById(R.id.btnDeafen);
        btnScreenShare = findViewById(R.id.btnScreenShare);
        btnPushToTalk = findViewById(R.id.btnPushToTalk);
        btnHandRaise = findViewById(R.id.btnHandRaise);
        btnMoreOptions = findViewById(R.id.btnMoreOptions);
        btnBack = findViewById(R.id.btnBack);
        btnManagePermissions = findViewById(R.id.btnManagePermissions);
        btnCreateChannel = findViewById(R.id.btnCreateChannel);
        rvUsers = findViewById(R.id.rvUsers);
        rvChat = findViewById(R.id.rvChat);
        etChatMessage = findViewById(R.id.etChatMessage);
        chatContainer = findViewById(R.id.chatContainer);

        // Setup RecyclerViews
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setLayoutManager(new LinearLayoutManager(this));

        // Click listeners
        btnJoinLeave.setOnClickListener(v -> {
            if (isInVoice) leaveVoice();
            else checkPermissionsAndJoin();
        });

        btnMute.setOnClickListener(v -> toggleMute());
        btnDeafen.setOnClickListener(v -> toggleDeafen());
        btnScreenShare.setOnClickListener(v -> toggleScreenShare());
        btnHandRaise.setOnClickListener(v -> toggleHandRaise());
        btnMoreOptions.setOnClickListener(v -> showMoreOptions());
        btnBack.setOnClickListener(v -> finish());

        // Push to Talk (hold button)
        btnPushToTalk.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    setPushToTalk(true);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    setPushToTalk(false);
                    break;
            }
            return true;
        });

        // Chat send
        findViewById(R.id.btnSendChat).setOnClickListener(v -> sendChatMessage());

        // Admin: Create Channel
        btnCreateChannel.setOnClickListener(v -> showCreateChannelDialog());

        // Admin: Manage Permissions
        btnManagePermissions.setOnClickListener(v -> showManagePermissionsDialog());
    }

    private void checkUserRole() {
        db.collection("players").document(userId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    userName = doc.getString("name");
                    if (userName == null) userName = "Unknown";
                    isAdmin = "admin".equals(doc.getString("role"));
                    if (tvAdminPanel != null) tvAdminPanel.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                }
            });
    }

    private void loadVoiceChannels() {
        db.collection("voice_channels")
            .get()
            .addOnSuccessListener(query -> {
                voiceUsers.clear();
                for (DocumentSnapshot doc : query.getDocuments()) {
                    // Load users in each channel
                    loadChannelUsers(doc.getId(), doc.getString("name"));
                }
            });
    }

    private void loadChannelUsers(String channelId, String channelName) {
        db.collection("voice_channels").document(channelId)
            .collection("participants")
            .get()
            .addOnSuccessListener(query -> {
                for (DocumentSnapshot doc : query.getDocuments()) {
                    VoiceUserInfo user = new VoiceUserInfo();
                    user.userId = doc.getId();
                    user.userName = doc.getString("userName");
                    user.isMuted = Boolean.TRUE.equals(doc.getBoolean("muted"));
                    user.isDeafened = Boolean.TRUE.equals(doc.getBoolean("deafened"));
                    user.isScreenSharing = Boolean.TRUE.equals(doc.getBoolean("screenSharing"));
                    user.isHandRaised = Boolean.TRUE.equals(doc.getBoolean("handRaised"));
                    user.isSpeaking = Boolean.TRUE.equals(doc.getBoolean("speaking"));
                    user.channelId = channelId;
                    user.channelName = channelName;
                    voiceUsers.add(user);
                }
                rvUsers.setAdapter(new VoiceUsersAdapter());
            });
    }

    private void checkPermissionsAndJoin() {
        // Check if player has voice permission (admin always allowed)
        if (!isAdmin) {
            db.collection("players").document(userId).get()
                .addOnSuccessListener(doc -> {
                    Boolean allowed = doc.getBoolean("voiceAllowed");
                    if (allowed != null && !allowed) {
                        Toast.makeText(this, "Voice chat permission denied.\nAsk admin to enable it.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    requestMicPermissionAndJoin();
                });
        } else {
            requestMicPermissionAndJoin();
        }
    }

    private void requestMicPermissionAndJoin() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
        } else {
            showChannelSelectionDialog();
        }
    }

    private void showCreateChannelDialog() {
        EditText input = new EditText(this);
        input.setHint("Channel name (e.g., Squad Alpha)");
        input.setTextColor(0xFFFFFFFF);
        input.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(this)
            .setTitle("➕ Create Voice Channel")
            .setView(input)
            .setPositiveButton("Create", (d, w) -> {
                String name = input.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(this, "Channel name required", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> channel = new HashMap<>();
                channel.put("name", name);
                channel.put("active", true);
                channel.put("createdAt", System.currentTimeMillis());
                channel.put("createdBy", userId);

                db.collection("voice_channels").document(name).set(channel)
                    .addOnSuccessListener(v -> {
                        Toast.makeText(this, "✅ Channel \"" + name + "\" created!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showManagePermissionsDialog() {
        db.collection("players")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener(query -> {
                List<String> names = new ArrayList<>();
                List<String> ids = new ArrayList<>();
                List<Boolean> allowed = new ArrayList<>();

                for (DocumentSnapshot doc : query.getDocuments()) {
                    names.add(doc.getString("name"));
                    ids.add(doc.getId());
                    Boolean canJoin = doc.getBoolean("voiceAllowed");
                    allowed.add(canJoin == null || canJoin); // Default: allowed
                }

                String[] namesArr = names.toArray(new String[0]);
                boolean[] checkedArr = new boolean[allowed.size()];
                for (int i = 0; i < allowed.size(); i++) checkedArr[i] = allowed.get(i);

                new AlertDialog.Builder(this)
                    .setTitle("👥 Manage Voice Permissions")
                    .setMessage("✅ = Can join voice | ❌ = Denied access")
                    .setMultiChoiceItems(namesArr, checkedArr, (dialog, which, isChecked) -> {
                        checkedArr[which] = isChecked;
                    })
                    .setPositiveButton("Save", (d, w) -> {
                        int allowedCount = 0;
                        for (int i = 0; i < ids.size(); i++) {
                            db.collection("players").document(ids.get(i))
                                .update("voiceAllowed", checkedArr[i]);
                            if (checkedArr[i]) allowedCount++;
                        }
                        Toast.makeText(this, "✅ Updated! " + allowedCount + "/" + ids.size() + " players allowed", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .setNeutralButton("Allow All", (d, w) -> {
                        for (int i = 0; i < ids.size(); i++) {
                            db.collection("players").document(ids.get(i))
                                .update("voiceAllowed", true);
                        }
                        Toast.makeText(this, "✅ All players allowed", Toast.LENGTH_SHORT).show();
                    })
                    .show();
            });
    }

    private void showChannelSelectionDialog() {
        db.collection("voice_channels").get()
            .addOnSuccessListener(query -> {
                List<String> channelNames = new ArrayList<>();
                List<String> channelIds = new ArrayList<>();
                
                for (DocumentSnapshot doc : query.getDocuments()) {
                    channelNames.add(doc.getString("name"));
                    channelIds.add(doc.getId());
                }

                String[] namesArray = channelNames.toArray(new String[0]);

                new AlertDialog.Builder(this)
                    .setTitle("🎙️ Select Voice Channel")
                    .setItems(namesArray, (dialog, which) -> {
                        joinVoiceChannel(channelIds.get(which), channelNames.get(which));
                    })
                    .show();
            });
    }

    private void joinVoiceChannel(String channelId, String channelName) {
        currentChannelId = channelId;
        tvChannelName.setText(channelName);

        Map<String, Object> participant = new HashMap<>();
        participant.put("userId", userId);
        participant.put("userName", userName);
        participant.put("joinedAt", System.currentTimeMillis());
        participant.put("muted", false);
        participant.put("deafened", false);
        participant.put("screenSharing", false);
        participant.put("handRaised", false);
        participant.put("speaking", false);
        participant.put("isAdmin", isAdmin);
        participant.put("userStatus", userStatus);

        db.collection("voice_channels").document(channelId)
            .collection("participants").document(userId)
            .set(participant)
            .addOnSuccessListener(v -> {
                isInVoice = true;
                startTime = System.currentTimeMillis();
                startTimer();
                updateUIForConnected();
                listenToParticipants(channelId);
                listenToChat(channelId);
                Toast.makeText(this, "Joined " + channelName, Toast.LENGTH_SHORT).show();
            });
    }

    private void updateUIForConnected() {
        btnJoinLeave.setText("🔴 Leave");
        btnJoinLeave.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.danger));
        btnMute.setVisibility(View.VISIBLE);
        btnDeafen.setVisibility(View.VISIBLE);
        btnScreenShare.setVisibility(View.VISIBLE);
        btnPushToTalk.setVisibility(View.VISIBLE);
        btnHandRaise.setVisibility(View.VISIBLE);
        btnMoreOptions.setVisibility(View.VISIBLE);
        if (tvStatus != null) tvStatus.setText("🟢 Connected");
    }

    private void listenToParticipants(String channelId) {
        db.collection("voice_channels").document(channelId)
            .collection("participants")
            .addSnapshotListener((snapshots, e) -> {
                if (e != null || snapshots == null) return;
                voiceUsers.clear();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    VoiceUserInfo user = new VoiceUserInfo();
                    user.userId = doc.getId();
                    user.userName = doc.getString("userName");
                    user.isMuted = Boolean.TRUE.equals(doc.getBoolean("muted"));
                    user.isDeafened = Boolean.TRUE.equals(doc.getBoolean("deafened"));
                    user.isScreenSharing = Boolean.TRUE.equals(doc.getBoolean("screenSharing"));
                    user.isHandRaised = Boolean.TRUE.equals(doc.getBoolean("handRaised"));
                    user.isSpeaking = Boolean.TRUE.equals(doc.getBoolean("speaking"));
                    user.channelId = channelId;
                    voiceUsers.add(user);
                }
                rvUsers.setAdapter(new VoiceUsersAdapter());
                if (tvStatus != null) {
                    tvStatus.setText("🟢 " + voiceUsers.size() + " users connected");
                }
            });
    }

    private void listenToChat(String channelId) {
        db.collection("voice_channels").document(channelId)
            .collection("chat")
            .orderBy("timestamp")
            .addSnapshotListener((snapshots, e) -> {
                if (e != null || snapshots == null) return;
                chatMessages.clear();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    ChatMessageInfo msg = new ChatMessageInfo();
                    msg.senderName = doc.getString("senderName");
                    msg.text = doc.getString("text");
                    msg.timestamp = doc.getLong("timestamp");
                    chatMessages.add(msg);
                }
                rvChat.setAdapter(new ChatAdapter());
                if (chatMessages.size() > 0) {
                    rvChat.scrollToPosition(chatMessages.size() - 1);
                }
            });
    }

    private void sendChatMessage() {
        String text = etChatMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text) || currentChannelId == null) return;

        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId", userId);
        msg.put("senderName", userName);
        msg.put("text", text);
        msg.put("timestamp", System.currentTimeMillis());

        db.collection("voice_channels").document(currentChannelId)
            .collection("chat").add(msg)
            .addOnSuccessListener(v -> etChatMessage.setText(""));
    }

    // Voice Controls
    private void toggleMute() {
        isMuted = !isMuted;
        btnMute.setText(isMuted ? "🔊" : "🔇");
        updateParticipantField("muted", isMuted);
    }

    private void toggleDeafen() {
        isDeafened = !isDeafened;
        btnDeafen.setText(isDeafened ? "👂" : "🔕");
        if (isDeafened && !isMuted) toggleMute();
        updateParticipantField("deafened", isDeafened);
    }

    private void toggleScreenShare() {
        isScreenSharing = !isScreenSharing;
        btnScreenShare.setText(isScreenSharing ? "📺" : "🖥️");
        updateParticipantField("screenSharing", isScreenSharing);
        Toast.makeText(this, isScreenSharing ? "Screen sharing started" : "Screen sharing stopped", 
            Toast.LENGTH_SHORT).show();
    }

    private void toggleHandRaise() {
        isHandRaised = !isHandRaised;
        btnHandRaise.setText(isHandRaised ? "✋" : "🤚");
        updateParticipantField("handRaised", isHandRaised);
    }

    private void setPushToTalk(boolean active) {
        isPushToTalk = active;
        isSpeaking = active;
        btnPushToTalk.setText(active ? "🗣️" : "🎤");
        updateParticipantField("speaking", active);
    }

    private void updateParticipantField(String field, boolean value) {
        if (currentChannelId != null) {
            db.collection("voice_channels").document(currentChannelId)
                .collection("participants").document(userId)
                .update(field, value);
        }
    }

    private void showMoreOptions() {
        String[] options = {
            "Set Status", "User Profile", "Direct Message", 
            "Invite Friend", "Report User", "Channel Info"
        };

        new AlertDialog.Builder(this)
            .setTitle("More Options")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: showStatusDialog(); break;
                    case 1: showUserProfileDialog(); break;
                    case 2: showDirectMessageDialog(); break;
                    case 3: showInviteFriendDialog(); break;
                    case 4: Toast.makeText(this, "User reported", Toast.LENGTH_SHORT).show(); break;
                    case 5: showChannelInfoDialog(); break;
                }
            })
            .show();
    }

    private void showStatusDialog() {
        String[] statuses = {"🟢 Online", "🟡 Idle", "🔴 Do Not Disturb"};
        new AlertDialog.Builder(this)
            .setTitle("Set Status")
            .setItems(statuses, (dialog, which) -> {
                String[] statusValues = {"ONLINE", "IDLE", "DO_NOT_DISTURB"};
                userStatus = statusValues[which];
                tvUserStatus.setText(statuses[which]);
                updateParticipantField("userStatus", false); // Will update with string
                db.collection("voice_channels").document(currentChannelId)
                    .collection("participants").document(userId)
                    .update("userStatus", userStatus);
            })
            .show();
    }

    private void showUserProfileDialog() {
        if (currentChannelId == null) return;
        
        db.collection("players").document(userId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String profile = "Name: " + doc.getString("name") + "\n" +
                        "Level: " + doc.getLong("level") + "\n" +
                        "Rank: " + doc.getString("rank") + "\n" +
                        "Status: " + userStatus;
                    
                    new AlertDialog.Builder(this)
                        .setTitle("👤 Your Profile")
                        .setMessage(profile)
                        .setPositiveButton("OK", null)
                        .show();
                }
            });
    }

    private void showDirectMessageDialog() {
        if (voiceUsers.size() <= 1) {
            Toast.makeText(this, "No other users to message", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> userNames = new ArrayList<>();
        List<String> userIds = new ArrayList<>();
        for (VoiceUserInfo user : voiceUsers) {
            if (!user.userId.equals(userId)) {
                userNames.add(user.userName);
                userIds.add(user.userId);
            }
        }

        String[] namesArray = userNames.toArray(new String[0]);
        new AlertDialog.Builder(this)
            .setTitle("📩 Direct Message")
            .setItems(namesArray, (dialog, which) -> {
                Toast.makeText(this, "DM sent to " + userNames.get(which), Toast.LENGTH_SHORT).show();
            })
            .show();
    }

    private void showInviteFriendDialog() {
        db.collection("players")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener(query -> {
                List<String> friendNames = new ArrayList<>();
                for (DocumentSnapshot doc : query.getDocuments()) {
                    if (!doc.getId().equals(userId)) {
                        friendNames.add(doc.getString("name"));
                    }
                }

                String[] namesArray = friendNames.toArray(new String[0]);
                new AlertDialog.Builder(this)
                    .setTitle("➕ Invite to Voice")
                    .setItems(namesArray, (dialog, which) -> {
                        Toast.makeText(this, "Invite sent to " + friendNames.get(which), 
                            Toast.LENGTH_SHORT).show();
                    })
                    .show();
            });
    }

    private void showChannelInfoDialog() {
        String info = "Channel: " + tvChannelName.getText() + "\n" +
            "Users: " + voiceUsers.size() + "\n" +
            "Duration: " + tvTimer.getText();
        
        new AlertDialog.Builder(this)
            .setTitle("ℹ️ Channel Info")
            .setMessage(info)
            .setPositiveButton("OK", null)
            .show();
    }

    private void leaveVoice() {
        if (currentChannelId != null) {
            db.collection("voice_channels").document(currentChannelId)
                .collection("participants").document(userId).delete();
        }
        isInVoice = false;
        isMuted = false;
        isDeafened = false;
        isScreenSharing = false;
        isHandRaised = false;
        currentChannelId = null;
        stopTimer();
        updateUIForDisconnected();
    }

    private void updateUIForDisconnected() {
        btnJoinLeave.setText("🎙️ Join Voice");
        btnJoinLeave.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.success));
        btnMute.setVisibility(View.GONE);
        btnDeafen.setVisibility(View.GONE);
        btnScreenShare.setVisibility(View.GONE);
        btnPushToTalk.setVisibility(View.GONE);
        btnHandRaise.setVisibility(View.GONE);
        btnMoreOptions.setVisibility(View.GONE);
        tvChannelName.setText("Select Channel");
        tvTimer.setText("00:00:00");
        if (tvStatus != null) tvStatus.setText("🔴 Not connected");
    }

    // Timer
    private void startTimer() {
        timerHandler.postDelayed(timerRunnable, 0);
    }

    private void stopTimer() {
        timerHandler.removeCallbacks(timerRunnable);
    }

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long elapsed = System.currentTimeMillis() - startTime;
            int seconds = (int) (elapsed / 1000) % 60;
            int minutes = (int) ((elapsed / 1000) / 60) % 60;
            int hours = (int) ((elapsed / 1000) / 3600);
            tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && 
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showChannelSelectionDialog();
        } else {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isInVoice) leaveVoice();
        stopTimer();
    }

    // Helper classes
    static class VoiceUserInfo {
        String userId, userName, channelId, channelName;
        boolean isMuted, isDeafened, isScreenSharing, isHandRaised, isSpeaking;
    }

    static class ChatMessageInfo {
        String senderName, text;
        Long timestamp;
    }

    // Adapters
    class VoiceUsersAdapter extends RecyclerView.Adapter<VoiceUsersAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_voice_user, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            VoiceUserInfo user = voiceUsers.get(position);
            holder.tvUserName.setText(user.userName);
            
            StringBuilder status = new StringBuilder();
            if (user.isSpeaking) status.append("🗣️ ");
            if (user.isMuted) status.append("🔇 ");
            if (user.isDeafened) status.append("🔕 ");
            if (user.isScreenSharing) status.append("📺 ");
            if (user.isHandRaised) status.append("✋ ");
            
            holder.tvUserStatus.setText(status.length() > 0 ? status.toString() : "🟢");
            
            // Click for admin actions
            if (isAdmin && !user.userId.equals(userId)) {
                holder.itemView.setOnClickListener(v -> {
                    String[] options = {"Mute User", "Kick User", "Ban User"};
                    new AlertDialog.Builder(VoiceActivity.this)
                        .setTitle(user.userName)
                        .setItems(options, (dialog, which) -> {
                            switch (which) {
                                case 0: 
                                    db.collection("voice_channels").document(currentChannelId)
                                        .collection("participants").document(user.userId)
                                        .update("muted", true);
                                    break;
                                case 1:
                                    db.collection("voice_channels").document(currentChannelId)
                                        .collection("participants").document(user.userId).delete();
                                    Toast.makeText(VoiceActivity.this, user.userName + " kicked", Toast.LENGTH_SHORT).show();
                                    break;
                                case 2:
                                    Toast.makeText(VoiceActivity.this, user.userName + " banned", Toast.LENGTH_SHORT).show();
                                    break;
                            }
                        }).show();
                });
            }
        }

        @Override
        public int getItemCount() { return voiceUsers.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvUserName, tvUserStatus;
            VH(View v) {
                super(v);
                tvUserName = v.findViewById(R.id.tvVoiceUserName);
                tvUserStatus = v.findViewById(R.id.tvVoiceUserStatus);
            }
        }
    }

    class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_voice_chat_message, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ChatMessageInfo msg = chatMessages.get(position);
            holder.tvSender.setText(msg.senderName);
            holder.tvMessage.setText(msg.text);
            if (msg.timestamp != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                holder.tvTime.setText(sdf.format(new Date(msg.timestamp)));
            }
        }

        @Override
        public int getItemCount() { return chatMessages.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvSender, tvMessage, tvTime;
            VH(View v) {
                super(v);
                tvSender = v.findViewById(R.id.tvSenderName);
                tvMessage = v.findViewById(R.id.tvMessage);
                tvTime = v.findViewById(R.id.tvTimestamp);
            }
        }
    }
}
