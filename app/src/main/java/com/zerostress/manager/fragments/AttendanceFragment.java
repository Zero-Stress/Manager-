package com.zerostress.manager.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.zerostress.manager.FirestoreRepository;
import com.zerostress.manager.R;
import com.zerostress.manager.models.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AttendanceFragment extends Fragment {
    private FirestoreRepository repo;
    private LinearLayout attendanceContainer;
    private String userRole = "player";
    private String userName = "";
    private List<Player> allPlayers = new ArrayList<>();
    private List<Map<String, Object>> attendanceRecords = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_attendance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            repo = new FirestoreRepository();
        } catch (Exception e) {
            android.util.Log.e(getClass().getSimpleName(), "Firestore init failed", e);
            return;
        }
        attendanceContainer = view.findViewById(R.id.attendance_container);

        if (getArguments() != null) {
            userRole = getArguments().getString("userRole", "player");
            userName = getArguments().getString("userName", "");
        }

        repo.listenUsers(players -> {
            allPlayers.clear();
            for (Player p : players) {
                if ("confirmed".equals(p.getStatus())) allPlayers.add(p);
            }
            if (getActivity() != null) getActivity().runOnUiThread(this::renderAttendance);
        });

        repo.listenAttendance(records -> {
            attendanceRecords = records;
            if (getActivity() != null) getActivity().runOnUiThread(this::renderAttendance);
        });
    }

    private void renderAttendance() {
        if (!isAdded() || getContext() == null) return;
        attendanceContainer.removeAllViews();

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Today's attendance section
        addSectionHeader("📅 Today's Attendance (" + today + ")");

        if ("admin".equals(userRole)) {
            for (Player p : allPlayers) {
                LinearLayout row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 4, 0, 4);

                TextView nameTv = new TextView(getContext());
                nameTv.setText(p.getName() + (p.isCurrentlyOnline() ? " 🟢" : ""));
                nameTv.setTextColor(Color.WHITE);
                nameTv.setTextSize(13);
                nameTv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                row.addView(nameTv);

                Button presentBtn = new Button(getContext());
                presentBtn.setText("✅");
                presentBtn.setTextSize(10);
                presentBtn.setBackgroundColor(Color.parseColor("#10b981"));
                presentBtn.setTextColor(Color.WHITE);
                presentBtn.setOnClickListener(v -> {
                    repo.markAttendance(p.getName(), today, true);
                    Toast.makeText(getContext(), p.getName() + " marked present", Toast.LENGTH_SHORT).show();
                });
                row.addView(presentBtn);

                Button absentBtn = new Button(getContext());
                absentBtn.setText("❌");
                absentBtn.setTextSize(10);
                absentBtn.setBackgroundColor(Color.parseColor("#ef4444"));
                absentBtn.setTextColor(Color.WHITE);
                absentBtn.setOnClickListener(v -> {
                    repo.markAttendance(p.getName(), today, false);
                    Toast.makeText(getContext(), p.getName() + " marked absent", Toast.LENGTH_SHORT).show();
                });
                row.addView(absentBtn);

                attendanceContainer.addView(row);
            }
        } else {
            // Player sees their own attendance status
            TextView myStatus = new TextView(getContext());
            myStatus.setText("Tap 'Mark Attendance' to record your presence");
            myStatus.setTextColor(Color.parseColor("#94a3b8"));
            myStatus.setTextSize(13);
            myStatus.setPadding(0, 8, 0, 8);
            attendanceContainer.addView(myStatus);

            Button markBtn = new Button(getContext());
            markBtn.setText("✅ Mark My Attendance");
            markBtn.setBackgroundColor(Color.parseColor("#10b981"));
            markBtn.setTextColor(Color.WHITE);
            markBtn.setOnClickListener(v -> {
                repo.markAttendance(userName, today, true);
                Toast.makeText(getContext(), "Attendance recorded! ✅", Toast.LENGTH_SHORT).show();
            });
            attendanceContainer.addView(markBtn);
        }

        // Attendance History
        addSectionHeader("📊 Attendance History");

        if (attendanceRecords.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("No attendance records yet");
            empty.setTextColor(Color.parseColor("#94a3b8"));
            empty.setPadding(0, 8, 0, 8);
            attendanceContainer.addView(empty);
        } else {
            // Group by date
            Map<String, List<Map<String, Object>>> byDate = new HashMap<>();
            for (Map<String, Object> record : attendanceRecords) {
                String date = (String) record.get("date");
                if (date != null) {
                    if (!byDate.containsKey(date)) byDate.put(date, new ArrayList<>());
                    byDate.get(date).add(record);
                }
            }

            for (Map.Entry<String, List<Map<String, Object>>> entry : byDate.entrySet()) {
                TextView dateHeader = new TextView(getContext());
                dateHeader.setText("📆 " + entry.getKey());
                dateHeader.setTextColor(Color.parseColor("#38bdf8"));
                dateHeader.setTextSize(13);
                dateHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                dateHeader.setPadding(0, 12, 0, 4);
                attendanceContainer.addView(dateHeader);

                int present = 0, absent = 0;
                for (Map<String, Object> rec : entry.getValue()) {
                    Boolean isPresent = (Boolean) rec.get("present");
                    String name = (String) rec.get("playerName");
                    if (isPresent != null && isPresent) present++;
                    else absent++;

                    TextView recTv = new TextView(getContext());
                    recTv.setText((isPresent != null && isPresent ? "✅" : "❌") + " " + name);
                    recTv.setTextColor(isPresent != null && isPresent ? Color.parseColor("#10b981") : Color.parseColor("#ef4444"));
                    recTv.setTextSize(12);
                    recTv.setPadding(16, 2, 0, 2);
                    attendanceContainer.addView(recTv);
                }

                TextView summary = new TextView(getContext());
                summary.setText("Present: " + present + " | Absent: " + absent);
                summary.setTextColor(Color.parseColor("#94a3b8"));
                summary.setTextSize(11);
                summary.setPadding(16, 4, 0, 4);
                attendanceContainer.addView(summary);
            }
        }
    }

    private void addSectionHeader(String title) {
        TextView header = new TextView(getContext());
        header.setText(title);
        header.setTextColor(Color.parseColor("#38bdf8"));
        header.setTextSize(15);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setPadding(0, 16, 0, 8);
        attendanceContainer.addView(header);
    }
}
