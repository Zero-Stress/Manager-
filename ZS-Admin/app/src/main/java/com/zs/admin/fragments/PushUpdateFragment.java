package com.zs.admin.fragments;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
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

import com.zs.admin.FirestoreRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PushUpdateFragment extends Fragment {
    private FirestoreRepository repo;
    private EditText titleInput, messageInput;
    private LinearLayout updateList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try { repo = new FirestoreRepository(); } catch (Exception e) { return new View(getContext()); }

        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));

        // Header
        TextView header = new TextView(getContext());
        header.setText("PUSH UPDATES");
        header.setTextColor(Color.parseColor("#38bdf8"));
        header.setTextSize(16);
        header.setTypeface(null, Typeface.BOLD);
        root.addView(header);

        TextView subtitle = new TextView(getContext());
        subtitle.setText("Send update notifications to all players");
        subtitle.setTextColor(Color.parseColor("#94a3b8"));
        subtitle.setTextSize(12);
        subtitle.setPadding(0, dp(4), 0, dp(8));
        root.addView(subtitle);

        // Title input
        titleInput = new EditText(getContext());
        titleInput.setHint("Update title (e.g. v2.3 New Features)");
        titleInput.setTextColor(Color.WHITE);
        titleInput.setTextSize(14);
        titleInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(0, dp(8), 0, dp(4));
        titleInput.setLayoutParams(titleParams);
        root.addView(titleInput);

        // Message input
        messageInput = new EditText(getContext());
        messageInput.setHint("What's new in this update...");
        messageInput.setTextColor(Color.WHITE);
        messageInput.setTextSize(14);
        messageInput.setMinLines(4);
        messageInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams msgParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        msgParams.setMargins(0, 0, 0, dp(8));
        messageInput.setLayoutParams(msgParams);
        root.addView(messageInput);

        // Send button
        Button sendBtn = new Button(getContext());
        sendBtn.setText("SEND UPDATE TO ALL PLAYERS");
        sendBtn.setTextColor(Color.WHITE);
        sendBtn.setBackgroundColor(Color.parseColor("#10b981"));
        sendBtn.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(50));
        sendParams.setMargins(0, 0, 0, dp(16));
        sendBtn.setLayoutParams(sendParams);
        sendBtn.setOnClickListener(v -> sendUpdate());
        root.addView(sendBtn);

        // History header
        TextView historyHeader = new TextView(getContext());
        historyHeader.setText("UPDATE HISTORY");
        historyHeader.setTextColor(Color.parseColor("#38bdf8"));
        historyHeader.setTextSize(14);
        historyHeader.setTypeface(null, Typeface.BOLD);
        root.addView(historyHeader);

        // Update list
        updateList = new LinearLayout(getContext());
        updateList.setOrientation(LinearLayout.VERTICAL);
        root.addView(updateList);

        ScrollView scroll = new ScrollView(getContext());
        scroll.addView(root);

        // Listen to push updates
        repo.listenPushUpdates(updates -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> renderUpdates(updates));
            }
        });

        return scroll;
    }

    private void sendUpdate() {
        if (!isAdded() || getContext() == null) return;
        String title = titleInput.getText().toString().trim();
        String message = messageInput.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(getContext(), "Enter a title", Toast.LENGTH_SHORT).show();
            return;
        }
        if (message.isEmpty()) {
            Toast.makeText(getContext(), "Enter update message", Toast.LENGTH_SHORT).show();
            return;
        }

        repo.sendPushUpdate(title, message, new FirestoreRepository.OnResultCallback() {
            @Override public void onSuccess() {
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    titleInput.setText("");
                    messageInput.setText("");
                    Toast.makeText(getContext(), "Update sent to all players!", Toast.LENGTH_SHORT).show();
                });
            }
            @Override public void onFailure(String error) {
                if (getActivity() != null) getActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void renderUpdates(List<Map<String, Object>> updates) {
        if (!isAdded() || getContext() == null) return;
        updateList.removeAllViews();

        if (updates.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("No updates sent yet");
            empty.setTextColor(Color.parseColor("#94a3b8"));
            empty.setPadding(0, dp(16), 0, 0);
            updateList.addView(empty);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

        for (Map<String, Object> item : updates) {
            LinearLayout card = new LinearLayout(getContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(Color.parseColor("#0f1729"));
            card.setPadding(dp(16), dp(12), dp(16), dp(12));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, dp(8));
            card.setLayoutParams(cardParams);

            // Title
            Object titleObj = item.get("title");
            TextView titleTv = new TextView(getContext());
            titleTv.setText(titleObj != null ? titleObj.toString() : "Update");
            titleTv.setTextColor(Color.parseColor("#10b981"));
            titleTv.setTextSize(14);
            titleTv.setTypeface(null, Typeface.BOLD);
            card.addView(titleTv);

            // Date
            Object tsObj = item.get("timestamp");
            if (tsObj instanceof Number) {
                TextView dateTv = new TextView(getContext());
                dateTv.setText(sdf.format(new Date(((Number) tsObj).longValue())));
                dateTv.setTextColor(Color.parseColor("#94a3b8"));
                dateTv.setTextSize(11);
                dateTv.setPadding(0, dp(4), 0, 0);
                card.addView(dateTv);
            }

            // Message
            Object msgObj = item.get("message");
            TextView msgTv = new TextView(getContext());
            msgTv.setText(msgObj != null ? msgObj.toString() : "");
            msgTv.setTextColor(Color.WHITE);
            msgTv.setTextSize(13);
            msgTv.setPadding(0, dp(6), 0, 0);
            card.addView(msgTv);

            // Delete button
            Button delBtn = new Button(getContext());
            delBtn.setText("Delete");
            delBtn.setTextSize(10);
            delBtn.setTextColor(Color.WHITE);
            delBtn.setBackgroundColor(Color.parseColor("#ef4444"));
            delBtn.setPadding(dp(12), dp(4), dp(12), dp(4));
            LinearLayout.LayoutParams delParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(32));
            delParams.setMargins(0, dp(8), 0, 0);
            delBtn.setLayoutParams(delParams);
            Object idObj = item.get("id");
            if (idObj != null) {
                String id = idObj.toString();
                delBtn.setOnClickListener(v -> repo.deletePushUpdate(id, new FirestoreRepository.OnResultCallback() {
                    @Override public void onSuccess() {}
                    @Override public void onFailure(String e) {}
                }));
            }
            card.addView(delBtn);

            updateList.addView(card);
        }
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
