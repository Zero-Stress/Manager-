package com.zerostress.manager.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.zerostress.manager.FirestoreRepository;
import com.zerostress.manager.GameLaunchHelper;
import com.zerostress.manager.GameLaunchService;
import com.zerostress.manager.R;
import com.zerostress.manager.models.MatchSchedule;
import com.zerostress.manager.models.Squad;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScheduleFragment extends Fragment {
    private FirestoreRepository repo;
    private LinearLayout scheduleContainer;
    private String userRole = "player";
    private String userName = "";
    private String userPhone = "";
    private List<Squad> allSquads = new ArrayList<>();

    private static final String[] GAMES = {"PUBG Mobile", "Free Fire", "Call of Duty"};
    private static final String[] GAME_TYPES = {"pubg", "freefire", "cod"};
    private static final String[] AUTO_LAUNCH_OPTIONS = {"5 minutes before", "10 minutes before", "15 minutes before", "30 minutes before", "No auto-launch"};
    private static final int[] AUTO_LAUNCH_MINUTES = {5, 10, 15, 30, 0};

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
            userPhone = getArguments().getString("userPhone", "");
        }

        Button createBtn = view.findViewById(R.id.create_schedule_btn);
        if ("admin".equals(userRole)) {
            createBtn.setVisibility(View.VISIBLE);
            createBtn.setOnClickListener(v -> showCreateScheduleDialog());
        } else {
            createBtn.setVisibility(View.GONE);
        }

        // Load squads for admin selection
        repo.listenSquads(squads -> {
            allSquads.clear();
            allSquads.addAll(squads);
        });

        repo.listenSchedules(schedules -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> renderSchedules(schedules));
            }
        });

        // Start game launch service if admin
        if ("admin".equals(userRole)) {
            startGameLaunchService();
        }
    }

    private void startGameLaunchService() {
        try {
            Intent serviceIntent = new Intent(requireContext(), GameLaunchService.class);
            requireContext().startForegroundService(serviceIntent);
        } catch (Exception e) {
            // Service might already be running
        }
    }

    private void renderSchedules(List<MatchSchedule> schedules) {
        if (!isAdded() || getContext() == null) return;
        scheduleContainer.removeAllViews();

        if (schedules.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("No scheduled matches\n\nAdmin can create schedules with game selection and auto-launch!");
            empty.setTextColor(Color.parseColor("#94a3b8"));
            empty.setPadding(16, 32, 16, 32);
            empty.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
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
                case "ongoing": emoji = "\uD83D\uDD34"; statusColor = Color.parseColor("#ef4444"); break;
                case "completed": emoji = "\u2705"; statusColor = Color.parseColor("#10b981"); break;
                case "cancelled": emoji = "\u274C"; statusColor = Color.parseColor("#64748b"); break;
                default: emoji = "\uD83D\uDCC5"; statusColor = Color.parseColor("#f59e0b"); break;
            }

            // Title
            TextView titleTv = new TextView(getContext());
            titleTv.setText(emoji + " " + s.getTitle());
            titleTv.setTextColor(Color.WHITE);
            titleTv.setTextSize(15);
            titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
            card.addView(titleTv);

            // Game type
            if (s.getGameType() != null && !s.getGameType().isEmpty()) {
                TextView gameTv = new TextView(getContext());
                String gameEmoji = GameLaunchHelper.getGameEmoji(s.getGameType());
                String gameName = GameLaunchHelper.getDisplayName(s.getGameType());
                boolean installed = GameLaunchHelper.isGameInstalled(requireContext(), s.getGameType());
                String installStatus = installed ? "\u2705" : "\u274C Not installed";
                gameTv.setText(gameEmoji + " " + gameName + " " + installStatus);
                gameTv.setTextColor(installed ? Color.parseColor("#10b981") : Color.parseColor("#ef4444"));
                gameTv.setTextSize(13);
                gameTv.setTypeface(null, android.graphics.Typeface.BOLD);
                gameTv.setPadding(0, 6, 0, 0);
                card.addView(gameTv);
            }

            // Time
            TextView timeTv = new TextView(getContext());
            timeTv.setText("\u23F0 " + sdf.format(new Date(s.getScheduledTime())));
            timeTv.setTextColor(Color.parseColor("#38bdf8"));
            timeTv.setTextSize(12);
            timeTv.setPadding(0, 4, 0, 0);
            card.addView(timeTv);

            // Map
            if (s.getMapName() != null && !s.getMapName().isEmpty()) {
                TextView mapTv = new TextView(getContext());
                mapTv.setText("\uD83D\uDDFA\uFE0F " + s.getMapName());
                mapTv.setTextColor(Color.parseColor("#94a3b8"));
                mapTv.setTextSize(12);
                card.addView(mapTv);
            }

            // Squad
            if (s.getSquadName() != null && !s.getSquadName().isEmpty()) {
                TextView squadTv = new TextView(getContext());
                squadTv.setText("\u2694\uFE0F Squad: " + s.getSquadName());
                squadTv.setTextColor(Color.parseColor("#8b5cf6"));
                squadTv.setTextSize(12);
                card.addView(squadTv);
            }

            // Auto-launch info
            if (s.getMinutesBeforeLaunch() > 0 && s.getGameType() != null) {
                TextView launchTv = new TextView(getContext());
                launchTv.setText("\uD83D\uDE80 Auto-launch " + s.getMinutesBeforeLaunch() + " min before");
                launchTv.setTextColor(Color.parseColor("#f59e0b"));
                launchTv.setTextSize(11);
                card.addView(launchTv);
            }

            // Confirmed count
            TextView confirmedTv = new TextView(getContext());
            confirmedTv.setText("\u2705 Confirmed: " + s.getConfirmedCount() + " players");
            confirmedTv.setTextColor(Color.parseColor("#10b981"));
            confirmedTv.setTextSize(12);
            confirmedTv.setPadding(0, 8, 0, 0);
            card.addView(confirmedTv);

            // Confirm attendance button (for players)
            if (!"admin".equals(userRole) && "scheduled".equals(s.getStatus())) {
                boolean alreadyConfirmed = s.getConfirmedPlayers().contains(userName);
                Button confirmBtn = new Button(getContext());
                confirmBtn.setText(alreadyConfirmed ? "\u2705 Confirmed" : "Join Match");
                confirmBtn.setTextSize(11);
                confirmBtn.setBackgroundColor(alreadyConfirmed ? Color.parseColor("#10b981") : Color.parseColor("#38bdf8"));
                confirmBtn.setTextColor(Color.WHITE);
                if (!alreadyConfirmed) {
                    confirmBtn.setOnClickListener(v -> {
                        repo.confirmSchedule(s.getId(), userName);
                        Toast.makeText(getContext(), "You're in! \u2705", Toast.LENGTH_SHORT).show();
                    });
                }
                card.addView(confirmBtn);
            }

            // Launch game button (if game is installed and time is near)
            if (s.getGameType() != null && !s.getGameType().isEmpty()) {
                boolean installed = GameLaunchHelper.isGameInstalled(requireContext(), s.getGameType());
                if (installed && ("ongoing".equals(s.getStatus()) || isNearStartTime(s))) {
                    Button launchBtn = new Button(getContext());
                    String gameEmoji = GameLaunchHelper.getGameEmoji(s.getGameType());
                    launchBtn.setText(gameEmoji + " Launch " + GameLaunchHelper.getDisplayName(s.getGameType()));
                    launchBtn.setTextSize(11);
                    launchBtn.setBackgroundColor(Color.parseColor("#ef4444"));
                    launchBtn.setTextColor(Color.WHITE);
                    launchBtn.setOnClickListener(v -> {
                        GameLaunchHelper.launchGame(requireContext(), s.getGameType());
                    });
                    card.addView(launchBtn);
                }
            }

            // Admin actions
            if ("admin".equals(userRole)) {
                if ("scheduled".equals(s.getStatus())) {
                    LinearLayout adminRow = new LinearLayout(getContext());
                    adminRow.setOrientation(LinearLayout.HORIZONTAL);

                    Button startBtn = new Button(getContext());
                    startBtn.setText("Start Match");
                    startBtn.setTextSize(11);
                    startBtn.setBackgroundColor(Color.parseColor("#10b981"));
                    startBtn.setTextColor(Color.WHITE);
                    LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(0, 48, 1);
                    startParams.setMarginEnd(4);
                    startBtn.setLayoutParams(startParams);
                    startBtn.setOnClickListener(v -> {
                        s.setStatus("ongoing");
                        repo.updateSchedule(s);
                    });
                    adminRow.addView(startBtn);

                    Button cancelBtn = new Button(getContext());
                    cancelBtn.setText("Cancel");
                    cancelBtn.setTextSize(11);
                    cancelBtn.setBackgroundColor(Color.parseColor("#64748b"));
                    cancelBtn.setTextColor(Color.WHITE);
                    cancelBtn.setOnClickListener(v -> {
                        s.setStatus("cancelled");
                        repo.updateSchedule(s);
                    });
                    adminRow.addView(cancelBtn);

                    card.addView(adminRow);
                }
            }

            scheduleContainer.addView(card);
        }
    }

    private boolean isNearStartTime(MatchSchedule schedule) {
        long now = System.currentTimeMillis();
        long timeUntil = schedule.getScheduledTime() - now;
        // Within 30 minutes
        return timeUntil > 0 && timeUntil < 30 * 60 * 1000;
    }

    private void showCreateScheduleDialog() {
        if (!isAdded() || getContext() == null) return;

        Calendar cal = Calendar.getInstance();
        final int[] selectedGameIndex = {0};
        final int[] selectedSquadIndex = {0};
        final int[] selectedLaunchIndex = {1}; // Default 10 min

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("\uD83D\uDCC5 Schedule Match with Game");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        // Title
        EditText titleInput = new EditText(requireContext());
        titleInput.setHint("Match Title (e.g. Scrim vs Team-X)");
        layout.addView(titleInput);

        // Map
        EditText mapInput = new EditText(requireContext());
        mapInput.setHint("Map Name (e.g. Erangel)");
        layout.addView(mapInput);

        // Game selection
        TextView gameLabel = new TextView(requireContext());
        gameLabel.setText("\uD83C\uDFAE Select Game");
        gameLabel.setTextColor(Color.parseColor("#38bdf8"));
        gameLabel.setTextSize(13);
        gameLabel.setPadding(0, 12, 0, 4);
        layout.addView(gameLabel);

        Spinner gameSpinner = new Spinner(requireContext());
        ArrayAdapter<String> gameAdapter = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_spinner_item, GAMES);
        gameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gameSpinner.setAdapter(gameAdapter);
        gameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                selectedGameIndex[0] = pos;
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        layout.addView(gameSpinner);

        // Squad selection (if squads exist)
        if (!allSquads.isEmpty()) {
            TextView squadLabel = new TextView(requireContext());
            squadLabel.setText("\u2694\uFE0F Select Squad (optional)");
            squadLabel.setTextColor(Color.parseColor("#8b5cf6"));
            squadLabel.setTextSize(13);
            squadLabel.setPadding(0, 12, 0, 4);
            layout.addView(squadLabel);

            List<String> squadNames = new ArrayList<>();
            squadNames.add("No squad (all players)");
            for (Squad sq : allSquads) squadNames.add(sq.getName());

            Spinner squadSpinner = new Spinner(requireContext());
            ArrayAdapter<String> squadAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, squadNames);
            squadAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            squadSpinner.setAdapter(squadAdapter);
            squadSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                    selectedSquadIndex[0] = pos;
                }
                @Override public void onNothingSelected(AdapterView<?> p) {}
            });
            layout.addView(squadSpinner);
        }

        // Auto-launch timing
        TextView launchLabel = new TextView(requireContext());
        launchLabel.setText("\uD83D\uDE80 Auto-Launch Game Before Match");
        launchLabel.setTextColor(Color.parseColor("#f59e0b"));
        launchLabel.setTextSize(13);
        launchLabel.setPadding(0, 12, 0, 4);
        layout.addView(launchLabel);

        Spinner launchSpinner = new Spinner(requireContext());
        ArrayAdapter<String> launchAdapter = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_spinner_item, AUTO_LAUNCH_OPTIONS);
        launchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        launchSpinner.setAdapter(launchAdapter);
        launchSpinner.setSelection(1); // Default 10 min
        launchSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                selectedLaunchIndex[0] = pos;
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        layout.addView(launchSpinner);

        // Date/Time picker
        TextView dateLabel = new TextView(requireContext());
        dateLabel.setText("\uD83D\uDCC5 Tap to select date & time");
        dateLabel.setTextColor(Color.parseColor("#38bdf8"));
        dateLabel.setTextSize(13);
        dateLabel.setPadding(0, 12, 0, 8);
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
                    dateLabel.setText("\uD83D\uDCC5 " + display.format(new Date(selectedTime[0])));
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        builder.setView(layout);
        builder.setPositiveButton("Schedule Match", (d, w) -> {
            String title = titleInput.getText().toString().trim();
            String map = mapInput.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a title", Toast.LENGTH_SHORT).show();
                return;
            }

            MatchSchedule schedule = new MatchSchedule(title, selectedTime[0], map.isEmpty() ? "TBD" : map, userName);

            // Set game type
            schedule.setGameType(GAME_TYPES[selectedGameIndex[0]]);

            // Set squad
            if (!allSquads.isEmpty() && selectedSquadIndex[0] > 0) {
                Squad selectedSquad = allSquads.get(selectedSquadIndex[0] - 1);
                schedule.setSquadId(selectedSquad.getId());
                schedule.setSquadName(selectedSquad.getName());
            }

            // Set auto-launch
            schedule.setMinutesBeforeLaunch(AUTO_LAUNCH_MINUTES[selectedLaunchIndex[0]]);
            schedule.setGameLaunched(false);

            repo.createSchedule(schedule, new FirestoreRepository.OnResultCallback() {
                @Override public void onSuccess() {
                    if (getContext() != null) {
                        String gameName = GameLaunchHelper.getDisplayName(schedule.getGameType());
                        String launchInfo = schedule.getMinutesBeforeLaunch() > 0 ?
                            "\nAuto-launch: " + schedule.getMinutesBeforeLaunch() + " min before" : "";
                        Toast.makeText(getContext(), "Match scheduled!\nGame: " + gameName + launchInfo, Toast.LENGTH_LONG).show();
                    }
                }
                @Override public void onFailure(String e) {}
            });
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
