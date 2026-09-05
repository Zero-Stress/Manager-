package com.zerostress.manager;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private EditText etMessage;
    private RecyclerView rvMessages;
    private ProgressBar progressBar;
    private MessageAdapter adapter;
    private List<DocumentSnapshot> messages = new ArrayList<>();
    private List<DocumentSnapshot> players = new ArrayList<>();
    private FirebaseFirestore db;
    private String userId, userName;
    private ListenerRegistration chatListener;
    private TextView tvTypingIndicator;
    private static final String TYPING_PREFIX = "__typing_";
    private ListenerRegistration typingListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        etMessage = findViewById(R.id.etMessage);
        rvMessages = findViewById(R.id.rvMessages);
        progressBar = findViewById(R.id.progressBar);
        MaterialButton btnSend = findViewById(R.id.btnSend);
        TextView btnMention = findViewById(R.id.btnMention);
        tvTypingIndicator = findViewById(R.id.tvTypingIndicator);

        // Track typing status
        etMessage.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                setTyping(true);
            }
        });
        etMessage.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().isEmpty()) {
                    setTyping(true);
                }
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        // Listen for typing indicators
        typingListener = db.collection("chat_typing").addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) return;
            List<String> typers = new ArrayList<>();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                String uid = doc.getId().replace(TYPING_PREFIX, "");
                if (!uid.equals(userId) && System.currentTimeMillis() - doc.getLong("timestamp").longValue() < 5000) {
                    typers.add(uid);
                }
            }
            if (typers.isEmpty()) {
                tvTypingIndicator.setVisibility(View.GONE);
            } else {
                tvTypingIndicator.setVisibility(View.VISIBLE);
                tvTypingIndicator.setText("💬 " + typers.size() + " person" + (typers.size() > 1 ? "s" : "") + " typing");
            }
        });

        loadMessages();
        TextView btnClearChat = findViewById(R.id.btnClearChat);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);
        adapter = new MessageAdapter();
        rvMessages.setAdapter(adapter);

        // Load user name
        db.collection("players").document(userId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    userName = doc.getString("name");
                    if (userName == null) userName = "Unknown";
                }
            });

        // Load players for @mention (include ALL players + admins)
        loadPlayers();

        // @ button in input bar — tap to show mention picker
        btnMention.setOnClickListener(v -> showMentionDialog());

        // Clear chat button (admin only)
        btnClearChat.setOnClickListener(v -> showClearChatDialog());
        checkAdminStatus();

        // Also support long-press on EditText for mentions
        etMessage.setOnLongClickListener(v -> {
            showMentionDialog();
            return true;
        });

        // Send on button click
        btnSend.setOnClickListener(v -> sendMessage());

        // Load messages
        loadMessages();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatListener != null) chatListener.remove();
        if (typingListener != null) typingListener.remove();
        // Clear typing indicator
        if (userId != null) {
            db.collection("chat_typing").document(TYPING_PREFIX + userId).delete();
        }
    }

    private boolean isAdmin = false;

    private void checkAdminStatus() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        db.collection("players").document(uid).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    isAdmin = "admin".equals(doc.getString("role"));
                    TextView btnClearChat = findViewById(R.id.btnClearChat);
                    if (btnClearChat != null) {
                        btnClearChat.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                    }
                }
            });
    }

    private void showClearChatDialog() {
        new AlertDialog.Builder(this)
            .setTitle("🗑️ Clear All Chat Messages")
            .setMessage("This will permanently delete ALL chat messages.\n\nThis action cannot be undone!")
            .setPositiveButton("Clear All", (d, w) -> clearAllMessages())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void clearAllMessages() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        db.collection("chat_messages").get()
            .addOnSuccessListener(query -> {
                if (query.isEmpty()) {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Chat is already empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Delete in batches of 500 (Firestore limit)
                com.google.firebase.firestore.WriteBatch batch = db.batch();
                int count = 0;
                for (com.google.firebase.firestore.DocumentSnapshot doc : query.getDocuments()) {
                    batch.delete(doc.getReference());
                    count++;
                }

                final int totalCount = count;
                batch.commit()
                    .addOnSuccessListener(v -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "✅ Chat cleared! " + totalCount + " messages deleted", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            });
    }

    private void loadPlayers() {
        db.collection("players")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener(query -> {
                players.clear();
                for (DocumentSnapshot doc : query.getDocuments()) {
                    players.add(doc);
                }
            })
            .addOnFailureListener(e -> {
                // Fallback: try without status filter
                db.collection("players").get()
                    .addOnSuccessListener(query -> {
                        players.clear();
                        for (DocumentSnapshot doc : query.getDocuments()) {
                            players.add(doc);
                        }
                    });
            });
    }

    private void showMentionDialog() {
        if (players.isEmpty()) {
            Toast.makeText(this, "No players found. Try again in a moment.", Toast.LENGTH_SHORT).show();
            loadPlayers();
            return;
        }

        String[] playerNames = new String[players.size()];
        for (int i = 0; i < players.size(); i++) {
            String name = players.get(i).getString("name");
            String role = players.get(i).getString("role");
            playerNames[i] = (name != null ? name : "Unknown") + ("admin".equals(role) ? " 👑" : "");
        }

        new AlertDialog.Builder(this)
            .setTitle("📢 Mention a Player")
            .setItems(playerNames, (dialog, which) -> {
                String cleanName = playerNames[which].replace(" 👑", "");
                String currentText = etMessage.getText().toString();
                etMessage.setText(currentText + "@" + cleanName + " ");
                etMessage.setSelection(etMessage.getText().length());
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void loadMessages() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        // Use snapshot listener WITHOUT orderBy to avoid Firestore index requirement
        // Sort client-side by timestamp
        chatListener = db.collection("chat_messages")
            .limit(200)
            .addSnapshotListener((snapshots, e) -> {
                if (progressBar != null) progressBar.setVisibility(View.GONE);

                if (e != null) {
                    Toast.makeText(this, "Chat load error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                messages.clear();
                if (snapshots != null) {
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Boolean deleted = doc.getBoolean("deleted");
                        if (deleted == null || !deleted) {
                            messages.add(doc);
                        }
                    }
                }

                // Sort by timestamp client-side (avoids needing Firestore composite index)
                Collections.sort(messages, new Comparator<DocumentSnapshot>() {
                    @Override
                    public int compare(DocumentSnapshot a, DocumentSnapshot b) {
                        Long tsA = a.getLong("timestamp");
                        Long tsB = b.getLong("timestamp");
                        if (tsA == null) tsA = 0L;
                        if (tsB == null) tsB = 0L;
                        return Long.compare(tsA, tsB);
                    }
                });

                adapter.notifyDataSetChanged();
                if (messages.size() > 0) {
                    rvMessages.scrollToPosition(messages.size() - 1);
                }
            });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        // Check for @mentions
        List<String> mentions = new ArrayList<>();
        for (DocumentSnapshot player : players) {
            String name = player.getString("name");
            if (name != null && text.contains("@" + name)) {
                mentions.add(name);
            }
        }

        Map<String, Object> msg = new HashMap<>();
        msg.put("text", text);
        msg.put("senderId", userId);
        msg.put("senderName", userName != null ? userName : "Unknown");
        msg.put("timestamp", System.currentTimeMillis());
        msg.put("deleted", false);
        if (!mentions.isEmpty()) {
            msg.put("mentions", mentions);
        }

        db.collection("chat_messages").add(msg)
            .addOnSuccessListener(v -> {
                etMessage.setText("");
                // Scroll to bottom
                if (messages.size() > 0) {
                    rvMessages.scrollToPosition(messages.size() - 1);
                }

                // Send notification for @mentions
                if (!mentions.isEmpty()) {
                    sendMentionNotifications(text, mentions);
                }

                // Send notification for chat message to all players
                sendChatNotification(text);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Send failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void sendChatNotification(String text) {
        // Save to notifications collection so Cloud Function triggers FCM push
        // senderId is used by the Cloud Function to skip pushing to this device
        Map<String, Object> notification = new HashMap<>();
        notification.put("title", "💬 New Chat Message");
        notification.put("message", (userName != null ? userName : "Unknown") + ": " + text);
        notification.put("type", "chat");
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("sentBy", userName);
        notification.put("senderId", userId);

        db.collection("notifications").add(notification);
    }

    private void sendMentionNotifications(String text, List<String> mentionedNames) {
        String mentionList = String.join(", ", mentionedNames);
        Map<String, Object> notification = new HashMap<>();
        notification.put("title", "📢 You were mentioned in chat!");
        notification.put("message", (userName != null ? userName : "Unknown") + " mentioned " + mentionList + ": " + text);
        notification.put("type", "mention");
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("mentions", mentionedNames);
        notification.put("senderId", userId);

        db.collection("notifications").add(notification);
    }

    class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.VH> {
        private static final int VIEW_TYPE_SENT = 1;
        private static final int VIEW_TYPE_RECEIVED = 2;

        @Override
        public int getItemViewType(int position) {
            DocumentSnapshot doc = messages.get(position);
            String senderId = doc.getString("senderId");
            return userId.equals(senderId) ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layout = viewType == VIEW_TYPE_SENT ? R.layout.item_chat_sent : R.layout.item_chat_received;
            View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            DocumentSnapshot doc = messages.get(position);
            holder.tvSender.setText(doc.getString("senderName"));

            String text = doc.getString("text");
            boolean isSent = userId.equals(doc.getString("senderId"));

            // Highlight @mentions with bold + color (light cyan on dark bubbles, dark navy on light/sent bubbles)
            if (text != null && text.contains("@")) {
                SpannableStringBuilder builder = new SpannableStringBuilder(text);
                int mentionColor = isSent ? Color.parseColor("#0b1220") : Color.parseColor("#7dd3fc");
                for (DocumentSnapshot player : players) {
                    String name = player.getString("name");
                    if (name != null) {
                        String mention = "@" + name;
                        int startIndex = text.indexOf(mention);
                        while (startIndex >= 0) {
                            builder.setSpan(
                                new ForegroundColorSpan(mentionColor),
                                startIndex,
                                startIndex + mention.length(),
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            );
                            builder.setSpan(
                                new StyleSpan(android.graphics.Typeface.BOLD),
                                startIndex,
                                startIndex + mention.length(),
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            );
                            startIndex = text.indexOf(mention, startIndex + 1);
                        }
                    }
                }
                holder.tvText.setText(builder);
            } else {
                holder.tvText.setText(text);
            }

            Long ts = doc.getLong("timestamp");
            if (ts != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                holder.tvTime.setText(sdf.format(new Date(ts)));
            }
        }

        @Override
        public int getItemCount() { return messages.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvSender, tvText, tvTime;
            VH(View v) {
                super(v);
                tvSender = v.findViewById(R.id.tvSender);
                tvText = v.findViewById(R.id.tvText);
                tvTime = v.findViewById(R.id.tvTime);
            }
        }
    }

    private void setTyping(boolean isTyping) {
        if (userId == null) return;
        Map<String, Object> typing = new HashMap<>();
        typing.put("userId", userId);
        typing.put("timestamp", System.currentTimeMillis());

        if (isTyping) {
            db.collection("chat_typing").document(TYPING_PREFIX + userId).set(typing);
        } else {
            db.collection("chat_typing").document(TYPING_PREFIX + userId).delete();
        }
    }
}
