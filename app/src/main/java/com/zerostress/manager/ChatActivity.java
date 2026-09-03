package com.zerostress.manager;

import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
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

public class ChatActivity extends AppCompatActivity {

    private EditText etMessage;
    private RecyclerView rvMessages;
    private ProgressBar progressBar;
    private MessageAdapter adapter;
    private List<DocumentSnapshot> messages = new ArrayList<>();
    private List<DocumentSnapshot> players = new ArrayList<>();
    private FirebaseFirestore db;
    private String userId, userName;

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

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);
        adapter = new MessageAdapter();
        rvMessages.setAdapter(adapter);

        db.collection("players").document(userId).get()
            .addOnSuccessListener(doc -> {
                userName = doc.getString("name");
                if (userName == null) userName = "Unknown";
            });

        // Load players for @mention
        loadPlayers();

        // Add @mention button
        etMessage.setOnLongClickListener(v -> {
            showMentionDialog();
            return true;
        });

        btnSend.setOnClickListener(v -> sendMessage());
        loadMessages();
    }

    private void loadPlayers() {
        db.collection("players")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener(query -> {
                players.clear();
                for (DocumentSnapshot doc : query.getDocuments()) {
                    String role = doc.getString("role");
                    if (!"admin".equals(role)) {
                        players.add(doc);
                    }
                }
            });
    }

    private void showMentionDialog() {
        if (players.isEmpty()) {
            Toast.makeText(this, "No players found", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] playerNames = new String[players.size()];
        for (int i = 0; i < players.size(); i++) {
            playerNames[i] = players.get(i).getString("name");
        }

        new AlertDialog.Builder(this)
            .setTitle("📢 Mention a Player")
            .setItems(playerNames, (dialog, which) -> {
                String mentionedName = playerNames[which];
                String currentText = etMessage.getText().toString();
                etMessage.setText(currentText + "@" + mentionedName + " ");
                etMessage.setSelection(etMessage.getText().length());
            })
            .show();
    }

    private void loadMessages() {
        db.collection("chat_messages")
            .orderBy("timestamp")
            .addSnapshotListener((snapshots, e) -> {
                if (e != null) return;
                messages.clear();
                if (snapshots != null) {
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Boolean deleted = doc.getBoolean("deleted");
                        if (deleted == null || !deleted) {
                            messages.add(doc);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
                if (messages.size() > 0) {
                    rvMessages.scrollToPosition(messages.size() - 1);
                }
            });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        // Check for mentions
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
            .addOnSuccessListener(v -> etMessage.setText(""))
            .addOnFailureListener(e -> Toast.makeText(this, "Send failed", Toast.LENGTH_SHORT).show());
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
            
            // Highlight @mentions
            if (text != null && text.contains("@")) {
                SpannableStringBuilder builder = new SpannableStringBuilder(text);
                for (DocumentSnapshot player : players) {
                    String name = player.getString("name");
                    if (name != null) {
                        String mention = "@" + name;
                        int startIndex = text.indexOf(mention);
                        while (startIndex >= 0) {
                            builder.setSpan(
                                new ForegroundColorSpan(Color.parseColor("#667eea")),
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
}
