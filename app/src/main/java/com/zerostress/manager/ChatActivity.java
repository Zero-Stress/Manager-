package com.zerostress.manager;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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

        btnSend.setOnClickListener(v -> sendMessage());
        loadMessages();
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

        Map<String, Object> msg = new HashMap<>();
        msg.put("text", text);
        msg.put("senderId", userId);
        msg.put("senderName", userName != null ? userName : "Unknown");
        msg.put("timestamp", System.currentTimeMillis());
        msg.put("deleted", false);

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
            holder.tvText.setText(doc.getString("text"));
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
