package com.zerostress.manager.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.zerostress.manager.FirestoreRepository;
import com.zerostress.manager.R;
import com.zerostress.manager.models.Announcement;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AnnouncementsFragment extends Fragment {
    private FirestoreRepository repo;
    private EditText announcementInput;
    private LinearLayout announcementList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_announcements, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new FirestoreRepository();
        announcementInput = view.findViewById(R.id.announcement_input);
        announcementList = view.findViewById(R.id.announcement_list);
        Button postBtn = view.findViewById(R.id.post_announcement_btn);

        postBtn.setOnClickListener(v -> {
            String text = announcementInput.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a message", Toast.LENGTH_SHORT).show();
                return;
            }
            repo.postAnnouncement(text, new FirestoreRepository.OnResultCallback() {
                @Override public void onSuccess() {
                    if (getActivity() != null) getActivity().runOnUiThread(() -> {
                        announcementInput.setText("");
                        Toast.makeText(getContext(), "Announcement posted!", Toast.LENGTH_SHORT).show();
                    });
                }
                @Override public void onFailure(String e) {}
            });
        });

        repo.listenAnnouncements(announcements -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> renderAnnouncements(announcements));
            }
        });
    }

    private void renderAnnouncements(List<Announcement> announcements) {
        announcementList.removeAllViews();
        if (announcements.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("No announcements yet");
            empty.setTextColor(Color.parseColor("#94a3b8"));
            empty.setPadding(16, 32, 16, 32);
            announcementList.addView(empty);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        for (Announcement a : announcements) {
            LinearLayout card = new LinearLayout(requireContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(Color.parseColor("#090d16"));
            card.setPadding(20, 16, 20, 16);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 10);
            card.setLayoutParams(params);

            TextView dateTv = new TextView(requireContext());
            dateTv.setText("📢 " + sdf.format(new Date(a.getTimestamp())));
            dateTv.setTextColor(Color.parseColor("#94a3b8"));
            dateTv.setTextSize(11);
            card.addView(dateTv);

            TextView msgTv = new TextView(requireContext());
            msgTv.setText(a.getMessage());
            msgTv.setTextColor(Color.WHITE);
            msgTv.setTextSize(14);
            msgTv.setPadding(0, 8, 0, 0);
            card.addView(msgTv);

            Button delBtn = new Button(requireContext());
            delBtn.setText("Delete");
            delBtn.setTextSize(11);
            delBtn.setBackgroundColor(Color.parseColor("#ef4444"));
            delBtn.setTextColor(Color.WHITE);
            delBtn.setOnClickListener(v -> {
                repo.deleteAnnouncement(a.getId(), new FirestoreRepository.OnResultCallback() {
                    @Override public void onSuccess() {}
                    @Override public void onFailure(String e) {}
                });
            });
            card.addView(delBtn);

            announcementList.addView(card);
        }
    }
}
