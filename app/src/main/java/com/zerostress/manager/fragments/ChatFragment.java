package com.zerostress.manager.fragments;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
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
import com.zerostress.manager.models.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatFragment extends Fragment {
    private FirestoreRepository repo;
    private LinearLayout chatContainer;
    private LinearLayout mentionSuggestions;
    private ScrollView chatScroll;
    private EditText messageInput;
    private String userName = "";
    private String userPhone = "";
    private boolean isUserAdmin = false;
    private List<Player> allPlayers = new ArrayList<>();
    private boolean showingMentions = false;

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
        mentionSuggestions = view.findViewById(R.id.mention_suggestions);
        Button sendBtn = view.findViewById(R.id.send_btn);

        if (getArguments() != null) {
            userName = getArguments().getString("userName", "");
            userPhone = getArguments().getString("userPhone", "");
            isUserAdmin = "admin".equals(getArguments().getString("userRole", "player"));
        }

        sendBtn.setOnClickListener(v -> sendMessage());

        // Listen for @ mentions
        messageInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                checkForMention(s.toString());
            }
        });

        // Load players for mention list
        repo.listenUsers(players -> {
            allPlayers.clear();
            for (Player p : players) {
                if ("confirmed".equals(p.getStatus())) allPlayers.add(p);
            }
        });

        repo.listenChat(messages -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> renderChat(messages));
            }
        });
    }

    private void checkForMention(String text) {
        if (!isAdded() || getContext() == null) return;

        // Find @ symbol - check if user is typing a mention
        int atIndex = text.lastIndexOf('@');
        if (atIndex >= 0 && (atIndex == 0 || text.charAt(atIndex - 1) == ' ')) {
            String query = text.substring(atIndex + 1).toLowerCase();
            if (!query.contains(" ")) {
                showMentionSuggestions(query);
                return;
            }
        }
        hideMentionSuggestions();
    }

    private void showMentionSuggestions(String query) {
        if (!isAdded() || getContext() == null) return;

        mentionSuggestions.removeAllViews();
        mentionSuggestions.setVisibility(View.VISIBLE);
        showingMentions = true;

        boolean found = false;
        for (Player p : allPlayers) {
            String name = p.getName();
            if (name != null && name.toLowerCase().contains(query)) {
                found = true;
                TextView item = new TextView(getContext());
                item.setText((p.isCurrentlyOnline() ? "\uD83D\uDFE2 " : "") + name);
                item.setTextColor(Color.WHITE);
                item.setTextSize(14);
                item.setPadding(24, 12, 24, 12);
                item.setBackgroundColor(Color.parseColor("#1e3a5f"));

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 0, 0, 2);
                item.setLayoutParams(params);

                item.setOnClickListener(v -> insertMention(name));
                mentionSuggestions.addView(item);
            }
        }

        if (!found) {
            TextView noResult = new TextView(getContext());
            noResult.setText("No players found");
            noResult.setTextColor(Color.parseColor("#64748b"));
            noResult.setTextSize(12);
            noResult.setPadding(24, 8, 24, 8);
            mentionSuggestions.addView(noResult);
        }
    }

    private void hideMentionSuggestions() {
        if (mentionSuggestions != null) {
            mentionSuggestions.setVisibility(View.GONE);
            mentionSuggestions.removeAllViews();
        }
        showingMentions = false;
    }

    private void insertMention(String name) {
        String text = messageInput.getText().toString();
        int atIndex = text.lastIndexOf('@');
        if (atIndex >= 0) {
            String before = text.substring(0, atIndex);
            String newText = before + "@" + name + " ";
            messageInput.setText(newText);
            messageInput.setSelection(newText.length());
        }
        hideMentionSuggestions();
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
                sysTv.setText("\uD83D\uDCE2 " + msg.getMessage());
                sysTv.setTextColor(Color.parseColor("#94a3b8"));
                sysTv.setTextSize(12);
                sysTv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                bubble.addView(sysTv);
            } else {
                bubble.setBackgroundColor(isOwn ? Color.parseColor("#1e3a5f") : Color.parseColor("#0f1729"));
                bubble.setPadding(12, 8, 12, 8);

                if (!isOwn) {
                    TextView nameTv = new TextView(getContext());
                    nameTv.setText(msg.getSenderName() + (isUserAdmin ? " \u2B50" : ""));
                    nameTv.setTextColor(Color.parseColor("#38bdf8"));
                    nameTv.setTextSize(11);
                    nameTv.setTypeface(null, Typeface.BOLD);
                    bubble.addView(nameTv);
                }

                // Render message with @mention highlighting
                TextView msgTv = new TextView(getContext());
                SpannableStringBuilder spannable = new SpannableStringBuilder(msg.getMessage());
                highlightMentions(spannable, msg.getMessage());
                msgTv.setText(spannable);
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

        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void highlightMentions(SpannableStringBuilder builder, String text) {
        // Find all @mentions in the message
        int i = 0;
        while (i < text.length()) {
            int atIndex = text.indexOf('@', i);
            if (atIndex < 0) break;

            // Find the end of the mention (space or end of string)
            int endIndex = text.indexOf(' ', atIndex + 1);
            if (endIndex < 0) endIndex = text.length();

            String mention = text.substring(atIndex + 1, endIndex);
            if (!mention.isEmpty()) {
                // Highlight the @mention in cyan/bold
                builder.setSpan(new ForegroundColorSpan(Color.parseColor("#38bdf8")),
                    atIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new StyleSpan(Typeface.BOLD),
                    atIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            i = endIndex;
        }
    }
}
