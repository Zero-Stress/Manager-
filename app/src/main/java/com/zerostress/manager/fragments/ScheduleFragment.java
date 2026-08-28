package com.zerostress.manager.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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
import com.zerostress.manager.models.MatchSchedule;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScheduleFragment extends Fragment {
    private FirestoreRepository repo;
    private LinearLayout scheduleContainer;
    private String userRole = "player";
    private String userName = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new FirestoreRepository();
        scheduleContainer = view.findViewById(R.id.schedule_container);

        if (getArguments() != null) {
            userRole = getArguments().getString("userRole", "player");
            userName = getArguments().getString("userName", "");
        }

        Button createBtn = view.findViewById(R.id.create_schedule_btn);
        if ("admin".equals(userRole)) {
            createBtn.setVisibility(View.VISIBLE);
            createBtn.setOnClickListener(v -> showCreateScheduleDialog());
        } else {
            createBtn.setVisibility(View.GONE);
        }

        repo.listenSchedules(schedules -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> renderSchedules(schedules));
            }
        });
    }

    private void renderSchedules(List<MatchSchedule> schedules) {
        if (!isAdded() || getContext() == null) return;
        scheduleContainer.removeAllViews();

        if (schedules.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("No scheduled matches");
            empty.setTextColor(Color.parseColor("#94a3b8"));
            empty.setPadding(16, 32, 16, 32);
            scheduleContainer.addView(empty);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());

        for (MatchSchedule s : schedules) {
            LinearLayout card = new LinearLayout(getContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(Color.parseColor("#0f1729"));
            card.setPadding(20, 16, 20, 16);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 12);
            card.setLayoutParams(params);

            // Status emoji
            String emoji;
            int statusColor;
            switch (s.getStatus()) {
                case "ongoing": emoji = "🔴"; statusColor = Color.parseColor("#ef4444"); break;
                case "completed": emoji = "✅"; statusColor = Color.parseColor("#10b981"); break;
                case "cancelled": emoji = "❌"; statusColor = Color.parseColor("#64748b"); break;
                default: emoji = "📅"; statusColor = Color.parseColor("#f59e0b"); break;
            }

            TextView titleTv = new TextView(getContext());
            titleTv.setText(emoji + " " + s.getTitle());
            titleTv.setTextColor(Color.WHITE);
            titleTv.setTextSize(15);
            titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
            card.addView(titleTv);

            TextView timeTv = new TextView(getContext());
            timeTv.setText("⏰ " + sdf.format(new Date(s.getScheduledTime())));
            timeTv.setTextColor(Color.parseColor("#38bdf8"));
            timeTv.setTextSize(12);
            timeTv.setPadding(0, 4, 0, 0);
            card.addView(timeTv);

            if (s.getMapName() != null && !s.getMapName().isEmpty()) {
                TextView mapTv = new TextView(getContext());
                mapTv.setText("🗺️ " + s.getMapName());
                mapTv.setTextColor(Color.parseColor("#94a3b8"));
                mapTv.setTextSize(12);
                card.addView(mapTv);
            }

            TextView confirmedTv = new TextView(getContext());
            confirmedTv.setText("✅ Confirmed: " + s.getConfirmedCount() + " players");
            confirmedTv.setTextColor(Color.parseColor("#10b981"));
            confirmedTv.setTextSize(12);
            confirmedTv.setPadding(0, 8, 0, 0);
            card.addView(confirmedTv);

            // Confirm attendance button (for players)
            if (!"admin".equals(userRole) && "scheduled".equals(s.getStatus())) {
                boolean alreadyConfirmed = s.getConfirmedPlayers().contains(userName);
                Button confirmBtn = new Button(getContext());
                confirmBtn.setText(alreadyConfirmed ? "✅ Confirmed" : "Join Match");
                confirmBtn.setTextSize(11);
                confirmBtn.setBackgroundColor(alreadyConfirmed ? Color.parseColor("#10b981") : Color.parseColor("#38bdf8"));
                confirmBtn.setTextColor(Color.WHITE);
                if (!alreadyConfirmed) {
                    confirmBtn.setOnClickListener(v -> {
                        repo.confirmSchedule(s.getId(), userName);
                        Toast.makeText(getContext(), "You're in! ✅", Toast.LENGTH_SHORT).show();
                    });
                }
                card.addView(confirmBtn);
            }

            // Admin actions
            if ("admin".equals(userRole)) {
                if ("scheduled".equals(s.getStatus())) {
                    Button startBtn = new Button(getContext());
                    startBtn.setText("Start Match");
                    startBtn.setTextSize(11);
                    startBtn.setBackgroundColor(Color.parseColor("#10b981"));
                    startBtn.setTextColor(Color.WHITE);
                    startBtn.setOnClickListener(v -> {
                        s.setStatus("ongoing");
                        repo.updateSchedule(s);
                    });
                    card.addView(startBtn);
                }
            }

            scheduleContainer.addView(card);
        }
    }

    private void showCreateScheduleDialog() {
        if (!isAdded() || getContext() == null) return;

        Calendar cal = Calendar.getInstance();

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Schedule Match");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        EditText titleInput = new EditText(requireContext());
        titleInput.setHint("Match Title (e.g. Scrim vs Team-X)");
        layout.addView(titleInput);

        EditText mapInput = new EditText(requireContext());
        mapInput.setHint("Map Name (e.g. Erangel)");
        layout.addView(mapInput);

        TextView dateLabel = new TextView(requireContext());
        dateLabel.setText("📅 Tap to select date & time");
        dateLabel.setTextColor(Color.parseColor("#38bdf8"));
        dateLabel.setTextSize(14);
        dateLabel.setPadding(0, 16, 0, 8);
        layout.addView(dateLabel);

        final long[] selectedTime = {cal.getTimeInMillis()};

        dateLabel.setOnClickListener(v -> {
            new DatePickerDialog(requireContext(), (view, year, month, day) -> {
                cal.set(year, month, day);
                new TimePickerDialog(requireContext(), (view2, hour, minute) -> {
                    cal.set(Calendar.HOUR_OF_DAY, hour);
                    cal.set(Calendar.MINUTE, minute);
                    selectedTime[0] = cal.getTimeInMillis();
                    SimpleDateFormat display = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                    dateLabel.setText("📅 " + display.format(new Date(selectedTime[0])));
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        builder.setView(layout);
        builder.setPositiveButton("Schedule", (d, w) -> {
            String title = titleInput.getText().toString().trim();
            String map = mapInput.getText().toString().trim();
            if (!title.isEmpty()) {
                MatchSchedule schedule = new MatchSchedule(title, selectedTime[0], map.isEmpty() ? "TBD" : map, userName);
                repo.createSchedule(schedule, new FirestoreRepository.OnResultCallback() {
                    @Override public void onSuccess() {
                        if (getContext() != null) Toast.makeText(getContext(), "Match scheduled!", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onFailure(String e) {}
                });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
