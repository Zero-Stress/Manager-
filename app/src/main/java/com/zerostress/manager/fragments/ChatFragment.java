package com.zerostress.manager.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.zerostress.manager.FirestoreRepository;
import com.zerostress.manager.R;
import com.zerostress.manager.models.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatFragment extends Fragment {
    private FirestoreRepository repo;
    private LinearLayout chatContainer;
    private ScrollView chatScroll;
    private EditText messageInput;
    private String userName = "";
    private String userPhone = "";
    private boolean isUserAdmin = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new FirestoreRepository();
        chatContainer = view.findViewById(R.id.chat_container);
        chatScroll = view.findViewById(R.id.chat_scroll);
        messageInput = view.findViewById(R.id.message_input);
        Button sendBtn = view.findViewById(R.id.send_btn);

        if (getArguments() != null) {
            userName = getArguments().getString("userName", "");
            userPhone = getArguments().getString("userPhone", "");
            isUserAdmin = "admin".equals(getArguments().getString("userRole", "player"));
        }

        sendBtn.setOnClickListener(v -> sendMessage());

        repo.listenChat(messages -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> renderChat(messages));
            }
        });
    }

    private void sendMessage() {
        if (!isAdded() || getContext() == null) return;

        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) return;

        ChatMessage msg = new ChatMessage(userName, userPhone, text);
        repo.sendChatMessage(msg, new FirestoreRepository.OnResultCallback() {
            @Override public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> messageInput.setText(""));
                }
            }
            @Override public void onFailure(String e) {}
        });
    }

    private void renderChat(List<ChatMessage> messages) {
        if (!isAdded() || getContext() == null) return;

        chatContainer.removeAllViews();
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        for (ChatMessage msg : messages) {
            boolean isOwn = msg.getSenderPhone() != null && msg.getSenderPhone().equals(userPhone);
            boolean isSystem = "system".equals(msg.getType());

            LinearLayout bubble = new LinearLayout(getContext());
            bubble.setOrientation(LinearLayout.VERTICAL);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(16, 4, 16, 4);
            bubble.setLayoutParams(params);

            if (isSystem) {
                bubble.setBackgroundColor(Color.parseColor("#1a2535"));
                bubble.setPadding(12, 8, 12, 8);

                TextView sysTv = new TextView(getContext());
                sysTv.setText("📢 " + msg.getMessage());
                sysTv.setTextColor(Color.parseColor("#94a3b8"));
                sysTv.setTextSize(12);
                sysTv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                bubble.addView(sysTv);
            } else {
                bubble.setBackgroundColor(isOwn ? Color.parseColor("#1e3a5f") : Color.parseColor("#0f1729"));
                bubble.setPadding(12, 8, 12, 8);

                if (!isOwn) {
                    TextView nameTv = new TextView(getContext());
                    nameTv.setText(msg.getSenderName() + (isUserAdmin ? " ⭐" : ""));
                    nameTv.setTextColor(Color.parseColor("#38bdf8"));
                    nameTv.setTextSize(11);
                    nameTv.setTypeface(null, android.graphics.Typeface.BOLD);
                    bubble.addView(nameTv);
                }

                TextView msgTv = new TextView(getContext());
                msgTv.setText(msg.getMessage());
                msgTv.setTextColor(Color.WHITE);
                msgTv.setTextSize(14);
                bubble.addView(msgTv);

                TextView timeTv = new TextView(getContext());
                timeTv.setText(sdf.format(new Date(msg.getTimestamp())));
                timeTv.setTextColor(Color.parseColor("#64748b"));
                timeTv.setTextSize(10);
                bubble.addView(timeTv);
            }

            chatContainer.addView(bubble);
        }

        // Scroll to bottom
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }
}
