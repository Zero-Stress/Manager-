package com.zerostress.manager.fragments;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.zerostress.manager.FirestoreRepository;
import com.zerostress.manager.R;
import com.zerostress.manager.models.LeaderboardEntry;
import com.zerostress.manager.models.MatchRecord;
import com.zerostress.manager.models.Player;

import java.util.ArrayList;
import java.util.List;

public class DailyInputFragment extends Fragment {
    private FirestoreRepository repo;
    private Spinner playerSpinner;
    private EditText matchesInput, winsInput, killsInput, assistsInput, damageInput, minutesInput, secondsInput;
    private TableLayout dailyTable;
    private List<MatchRecord> dailyLogs = new ArrayList<>();
    private List<Player> confirmedPlayers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_daily_input, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new FirestoreRepository();

        playerSpinner = view.findViewById(R.id.player_spinner);
        matchesInput = view.findViewById(R.id.matches_input);
        winsInput = view.findViewById(R.id.wins_input);
        killsInput = view.findViewById(R.id.kills_input);
        assistsInput = view.findViewById(R.id.assists_input);
        damageInput = view.findViewById(R.id.damage_input);
        minutesInput = view.findViewById(R.id.minutes_input);
        secondsInput = view.findViewById(R.id.seconds_input);
        dailyTable = view.findViewById(R.id.daily_log_table);
        Button submitBtn = view.findViewById(R.id.submit_daily_btn);
        Button resetBtn = view.findViewById(R.id.reset_daily_btn);

        submitBtn.setOnClickListener(v -> submitDailyRecord());
        resetBtn.setOnClickListener(v -> confirmResetDaily());

        // Listen to users for spinner
        repo.listenUsers(players -> {
            confirmedPlayers.clear();
            for (Player p : players) {
                if ("confirmed".equals(p.getStatus())) confirmedPlayers.add(p);
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> updateSpinner());
            }
        });

        // Listen to daily logs
        repo.listenDailyLogs(records -> {
            dailyLogs = records;
            if (getActivity() != null) {
                getActivity().runOnUiThread(this::renderDailyTable);
            }
        });
    }

    private void updateSpinner() {
        List<String> names = new ArrayList<>();
        names.add("-- Select Player --");
        for (Player p : confirmedPlayers) names.add(p.getName() + " (+880 " + p.getPhone() + ")");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        playerSpinner.setAdapter(adapter);
    }

    private void submitDailyRecord() {
        int pos = playerSpinner.getSelectedItemPosition();
        if (pos <= 0) {
            Toast.makeText(getContext(), "Please select a player!", Toast.LENGTH_SHORT).show();
            return;
        }

        String playerName = confirmedPlayers.get(pos - 1).getName();
        int matches = parseInt(matchesInput);
        int wins = parseInt(winsInput);
        int kills = parseInt(killsInput);
        int assists = parseInt(assistsInput);
        int damage = parseInt(damageInput);
        int minutes = parseInt(minutesInput);
        int seconds = parseInt(secondsInput);
        int survivalSeconds = (minutes * 60) + seconds;
        int avgDamage = matches > 0 ? Math.round((float) damage / matches) : 0;

        MatchRecord record = new MatchRecord(playerName, matches, wins, kills, assists,
                damage, avgDamage, survivalSeconds, System.currentTimeMillis());

        repo.addDailyRecord(record, new FirestoreRepository.OnResultCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Daily record saved!", Toast.LENGTH_SHORT).show();
                        clearForm();
                    });
                }
            }

            @Override
            public void onFailure(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void confirmResetDaily() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Reset Daily Logs")
            .setMessage("This will clear all daily records. Continue?")
            .setPositiveButton("Reset", (d, w) -> {
                repo.resetDailyLogs(backup -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Daily logs cleared!", Toast.LENGTH_SHORT).show()
                        );
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void renderDailyTable() {
        dailyTable.removeAllViews();
        if (dailyLogs.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("No daily entries yet");
            empty.setTextColor(Color.parseColor("#94a3b8"));
            empty.setPadding(16, 32, 16, 32);
            dailyTable.addView(empty);
            return;
        }

        for (MatchRecord r : dailyLogs) {
            TableRow row = new TableRow(requireContext());
            TextView nameTv = new TextView(requireContext());
            nameTv.setText(r.getPlayerName());
            nameTv.setTextColor(Color.WHITE);
            nameTv.setTextSize(12);
            nameTv.setPadding(8, 8, 8, 8);
            row.addView(nameTv);

            TextView statsTv = new TextView(requireContext());
            int mins = r.getSurvivalSeconds() / 60;
            int secs = r.getSurvivalSeconds() % 60;
            statsTv.setText("K:" + r.getKills() + " A:" + r.getAssists() + " D:" + r.getDamage() + " | " + mins + "m" + secs + "s");
            statsTv.setTextColor(Color.parseColor("#94a3b8"));
            statsTv.setTextSize(11);
            statsTv.setPadding(8, 8, 8, 8);
            row.addView(statsTv);

            Button delBtn = new Button(requireContext());
            delBtn.setText("X");
            delBtn.setTextSize(10);
            delBtn.setBackgroundColor(Color.parseColor("#ef4444"));
            delBtn.setTextColor(Color.WHITE);
            delBtn.setOnClickListener(v -> {
                repo.deleteDailyRecord(r.getId(), new FirestoreRepository.OnResultCallback() {
                    @Override public void onSuccess() {
                        if (getActivity() != null) getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Deleted!", Toast.LENGTH_SHORT).show());
                    }
                    @Override public void onFailure(String e) {}
                });
            });
            row.addView(delBtn);

            dailyTable.addView(row);
        }
    }

    private void clearForm() {
        matchesInput.setText("1");
        winsInput.setText("0");
        killsInput.setText("");
        assistsInput.setText("");
        damageInput.setText("");
        minutesInput.setText("0");
        secondsInput.setText("0");
        playerSpinner.setSelection(0);
    }

    private int parseInt(EditText et) {
        try { return Integer.parseInt(et.getText().toString().trim()); }
        catch (Exception e) { return 0; }
    }
}
