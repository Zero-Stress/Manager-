package com.zerostress.manager.fragments;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.ListenerRegistration;
import com.zerostress.manager.FirestoreRepository;
import com.zerostress.manager.R;
import com.zerostress.manager.models.ChatMessage;
import com.zerostress.manager.models.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatFragment extends Fragment {
    private FirestoreRepository repo;
    private LinearLayout chatContainer;
    private LinearLayout mentionSuggestions;
    private ScrollView chatScroll;
    private EditText messageInput;
    private String userName = "";
    private String userPhone = "";
    private boolean isUserAdmin = false;

    // Thread-safe list for players (accessed from multiple threads)
    private final CopyOnWriteArrayList<Player> allPlayers = new CopyOnWriteArrayList<>();
    private boolean showingMentions = false;

    // Listener registrations for cleanup
    private ListenerRegistration chatListener;
    private ListenerRegistration usersListener;

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
        View clearChatBtn = view.findViewById(R.id.clear_chat_btn);

        if (getArguments() != null) {
            userName = getArguments().getString("userName", "");
            userPhone = getArguments().getString("userPhone", "");
            isUserAdmin = "admin".equals(getArguments().getString("userRole", "player"));
        }

        // Show clear chat button only for admin
        if (isUserAdmin && clearChatBtn != null) {
            clearChatBtn.setVisibility(View.VISIBLE);
            clearChatBtn.setOnClickListener(v -> showClearChatDialog());
        }

        sendBtn.setOnClickListener(v -> sendMessage());

        // Listen for @ mentions
        messageInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (isAdded()) {
                    checkForMention(s.toString());
                }
            }
        });

        // Load players for mention list (thread-safe)
        usersListener = repo.listenUsers(players -> {
            if (!isAdded()) return;
            allPlayers.clear();
            for (Player p : players) {
                if ("confirmed".equals(p.getStatus())) {
                    allPlayers.add(p);
                }
            }
        });

        // Listen for chat messages
        chatListener = repo.listenChat(messages -> {
            if (getActivity() != null && isAdded()) {
                getActivity().runOnUiThread(() -> renderChat(messages));
            }
        });
    }

    private void checkForMention(String text) {
        if (!isAdded() || getContext() == null) return;

        // Find last @ symbol that's either at start or after a space
        int atIndex = text.lastIndexOf('@');
        if (atIndex >= 0) {
            // Check if @ is at start or preceded by space (not in middle of word)
            boolean validMention = (atIndex == 0) || (atIndex > 0 && text.charAt(atIndex - 1) == ' ');
            if (validMention) {
                String query = text.substring(atIndex + 1).toLowerCase();
                // Only show suggestions if query doesn't contain spaces (still typing name)
                if (!query.contains(" ") && !query.isEmpty()) {
                    showMentionSuggestions(query);
                    return;
                }
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
                String onlineStatus = p.isCurrentlyOnline() ? "\uD83D\uDFE2 " : "";
                item.setText(onlineStatus + name + " (" + p.getRoleLabel() + ")");
                item.setTextColor(Color.WHITE);
                item.setTextSize(14);
                item.setPadding(24, 14, 24, 14);
                item.setBackgroundColor(Color.parseColor("#1e3a5f"));

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 0, 0, 2);
                item.setLayoutParams(params);

                // Store name for click
                final String playerName = name;
                item.setOnClickListener(v -> insertMention(playerName));
                mentionSuggestions.addView(item);
            }
        }

        if (!found) {
            TextView noResult = new TextView(getContext());
            noResult.setText("No players found");
            noResult.setTextColor(Color.parseColor("#64748b"));
            noResult.setTextSize(12);
            noResult.setPadding(24, 12, 24, 12);
            mentionSuggestions.addView(noResult);
        }
    }

    private void hideMentionSuggestions() {
        if (mentionSuggestions != null && isAdded()) {
            mentionSuggestions.setVisibility(View.GONE);
            mentionSuggestions.removeAllViews();
        }
        showingMentions = false;
    }

    private void insertMention(String name) {
        if (!isAdded()) return;

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

        // Validate user info
        if (userName.isEmpty() || userPhone.isEmpty()) {
            Toast.makeText(getContext(), "User info not loaded. Please restart the app.", Toast.LENGTH_SHORT).show();
            return;
        }

        ChatMessage msg = new ChatMessage(userName, userPhone, text);
        repo.sendChatMessage(msg, new FirestoreRepository.OnResultCallback() {
            @Override public void onSuccess() {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        messageInput.setText("");
                        hideMentionSuggestions();
                    });
                }
            }
            @Override public void onFailure(String e) {
                if (isAdded() && getContext() != null) {
                    getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Failed to send: " + e, Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void renderChat(List<ChatMessage> messages) {
        if (!isAdded() || getContext() == null) return;

        chatContainer.removeAllViews();
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        // Show empty state if no messages
        if (messages.isEmpty()) {
            TextView emptyTv = new TextView(getContext());
            emptyTv.setText("\uD83D\uDCAC No messages yet\n\nStart the conversation!");
            emptyTv.setTextColor(Color.parseColor("#64748b"));
            emptyTv.setTextSize(14);
            emptyTv.setPadding(32, 48, 32, 48);
            emptyTv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            chatContainer.addView(emptyTv);
            return;
        }

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
                // System message (centered, muted)
                bubble.setBackgroundColor(Color.parseColor("#1a2535"));
                bubble.setPadding(12, 10, 12, 10);

                TextView sysTv = new TextView(getContext());
                sysTv.setText("\uD83D\uDCE2 " + msg.getMessage());
                sysTv.setTextColor(Color.parseColor("#94a3b8"));
                sysTv.setTextSize(12);
                sysTv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                bubble.addView(sysTv);
            } else {
                // Regular message
                bubble.setBackgroundColor(isOwn ? Color.parseColor("#1e3a5f") : Color.parseColor("#0f1729"));
                bubble.setPadding(14, 10, 14, 10);

                // Sender name (only for others' messages)
                if (!isOwn) {
                    TextView nameTv = new TextView(getContext());
                    String adminBadge = isUserAdmin ? " ⭐" : "";
                    nameTv.setText(msg.getSenderName() + adminBadge);
                    nameTv.setTextColor(Color.parseColor("#38bdf8"));
                    nameTv.setTextSize(11);
                    nameTv.setTypeface(null, Typeface.BOLD);
                    bubble.addView(nameTv);
                }

                // Message text with @mention highlighting
                TextView msgTv = new TextView(getContext());
                String messageText = msg.getMessage();
                if (messageText != null && !messageText.isEmpty()) {
                    SpannableStringBuilder spannable = new SpannableStringBuilder(messageText);
                    highlightMentions(spannable, messageText);
                    msgTv.setText(spannable);
                } else {
                    msgTv.setText("");
                }
                msgTv.setTextColor(Color.WHITE);
                msgTv.setTextSize(14);
                bubble.addView(msgTv);

                // Timestamp
                TextView timeTv = new TextView(getContext());
                long ts = msg.getTimestamp();
                if (ts > 0) {
                    timeTv.setText(sdf.format(new Date(ts)));
                } else {
                    timeTv.setText("Just now");
                }
                timeTv.setTextColor(Color.parseColor("#64748b"));
                timeTv.setTextSize(10);
                bubble.addView(timeTv);

                // Long press to delete (admin only)
                if (isUserAdmin) {
                    final String msgId = msg.getId();
                    final String senderName = msg.getSenderName();
                    bubble.setOnLongClickListener(v -> {
                        showDeleteMessageDialog(msgId, senderName);
                        return true;
                    });
                }
            }

            chatContainer.addView(bubble);
        }

        // Auto-scroll to bottom
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void showDeleteMessageDialog(String msgId, String senderName) {
        if (!isAdded() || getContext() == null || msgId == null) return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Delete Message");
        builder.setMessage("Delete message from " + senderName + "?");

        builder.setPositiveButton("Delete", (d, w) -> {
            // Delete from Firestore using the ID
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("chat").document(msgId)
                .delete()
                .addOnSuccessListener(a -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Message deleted", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Failed to delete", Toast.LENGTH_SHORT).show();
                    }
                });
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showClearChatDialog() {
        if (!isAdded() || getContext() == null) return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("🗑️ Clear All Chat");
        builder.setMessage("This will permanently delete ALL chat messages. This cannot be undone.\n\nAre you sure?");

        builder.setPositiveButton("Clear All", (d, w) -> {
            repo.clearAllChat(new FirestoreRepository.OnResultCallback() {
                @Override public void onSuccess() {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "All chat messages cleared!", Toast.LENGTH_SHORT).show();
                        // Post a system message
                        ChatMessage sysMsg = new ChatMessage("System", "system", "Chat has been cleared by admin");
                        sysMsg.setType("system");
                        repo.sendChatMessage(sysMsg, new FirestoreRepository.OnResultCallback() {
                            @Override public void onSuccess() {}
                            @Override public void onFailure(String e) {}
                        });
                    }
                }
                @Override public void onFailure(String e) {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Failed to clear chat: " + e, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void highlightMentions(SpannableStringBuilder builder, String text) {
        if (text == null || text.isEmpty()) return;

        int i = 0;
        while (i < text.length()) {
            int atIndex = text.indexOf('@', i);
            if (atIndex < 0) break;

            // Find the end of the mention (space, or end of string)
            int endIndex = text.indexOf(' ', atIndex + 1);
            if (endIndex < 0) endIndex = text.length();

            String mention = text.substring(atIndex + 1, endIndex);
            // Only highlight if mention looks like a name (letters, not empty)
            if (!mention.isEmpty() && mention.matches("[a-zA-Z\\s]+")) {
                builder.setSpan(new android.text.style.ForegroundColorSpan(Color.parseColor("#38bdf8")),
                    atIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new StyleSpan(Typeface.BOLD),
                    atIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            i = endIndex;
        }
    }

    // ==================== CLEANUP ====================

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove listeners to prevent memory leaks
        if (chatListener != null) {
            chatListener.remove();
            chatListener = null;
        }
        if (usersListener != null) {
            usersListener.remove();
            usersListener = null;
        }
        allPlayers.clear();
    }
}
