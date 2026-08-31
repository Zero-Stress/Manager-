package com.zs.admin.fragments;

import android.graphics.Color;
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
import com.zs.admin.models.Announcement;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminAnnouncementsFragment extends Fragment {
    private FirestoreRepository repo;
    private EditText announcementInput;
    private LinearLayout announcementList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try { repo = new FirestoreRepository(); } catch (Exception e) { return new View(getContext()); }

        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));

        TextView header = new TextView(getContext());
        header.setText("ANNOUNCEMENTS");
        header.setTextColor(Color.parseColor("#38bdf8"));
        header.setTextSize(16);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(header);

        // Input
        announcementInput = new EditText(getContext());
        announcementInput.setHint("Type announcement message...");
        announcementInput.setTextColor(Color.WHITE);
        announcementInput.setTextSize(14);
        announcementInput.setMinLines(3);
        announcementInput.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        inputParams.setMargins(0, dp(12), 0, dp(8));
        announcementInput.setLayoutParams(inputParams);
        root.addView(announcementInput);

        Button postBtn = new Button(getContext());
        postBtn.setText("PUBLISH TO ALL PLAYERS");
        postBtn.setTextColor(Color.WHITE);
        postBtn.setBackgroundColor(Color.parseColor("#10b981"));
        LinearLayout.LayoutParams postParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        postParams.setMargins(0, 0, 0, dp(16));
        postBtn.setLayoutParams(postParams);
        postBtn.setOnClickListener(v -> postAnnouncement());
        root.addView(postBtn);

        TextView listHeader = new TextView(getContext());
        listHeader.setText("ACTIVE BROADCASTS");
        listHeader.setTextColor(Color.parseColor("#38bdf8"));
        listHeader.setTextSize(14);
        listHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(listHeader);

        announcementList = new LinearLayout(getContext());
        announcementList.setOrientation(LinearLayout.VERTICAL);
        root.addView(announcementList);

        ScrollView scroll = new ScrollView(getContext());
        scroll.addView(root);

        repo.listenAnnouncements(announcements -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> renderAnnouncements(announcements));
            }
        });

        return scroll;
    }

    private void postAnnouncement() {
        if (!isAdded() || getContext() == null) return;
        String text = announcementInput.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(getContext(), "Type a message first", Toast.LENGTH_SHORT).show();
            return;
        }

        repo.postAnnouncement(text, new FirestoreRepository.OnResultCallback() {
            @Override public void onSuccess() {
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    announcementInput.setText("");
                    Toast.makeText(getContext(), "Published!", Toast.LENGTH_SHORT).show();
                });
            }
            @Override public void onFailure(String error) {}
        });
    }

    private void renderAnnouncements(List<Announcement> announcements) {
        if (!isAdded() || getContext() == null) return;
        announcementList.removeAllViews();

        if (announcements.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("No announcements yet");
            empty.setTextColor(Color.parseColor("#94a3b8"));
            empty.setPadding(0, dp(16), 0, 0);
            announcementList.addView(empty);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        for (Announcement a : announcements) {
            LinearLayout card = new LinearLayout(getContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(Color.parseColor("#0f1729"));
            card.setPadding(dp(16), dp(12), dp(16), dp(12));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, dp(8));
            card.setLayoutParams(cardParams);

            TextView dateTv = new TextView(getContext());
            dateTv.setText(sdf.format(new Date(a.getTimestamp())));
            dateTv.setTextColor(Color.parseColor("#94a3b8"));
            dateTv.setTextSize(11);
            card.addView(dateTv);

            TextView msgTv = new TextView(getContext());
            msgTv.setText(a.getMessage());
            msgTv.setTextColor(Color.WHITE);
            msgTv.setTextSize(14);
            msgTv.setPadding(0, dp(4), 0, 0);
            card.addView(msgTv);

            Button delBtn = new Button(getContext());
            delBtn.setText("Delete");
            delBtn.setTextSize(10);
            delBtn.setTextColor(Color.WHITE);
            delBtn.setBackgroundColor(Color.parseColor("#ef4444"));
            delBtn.setOnClickListener(v -> repo.deleteAnnouncement(a.getId(), new FirestoreRepository.OnResultCallback() {
                @Override public void onSuccess() {}
                @Override public void onFailure(String e) {}
            }));
            card.addView(delBtn);

            announcementList.addView(card);
        }
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
