package com.zs.admin.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.zs.admin.FirestoreRepository;
import com.zs.admin.models.MatchRecord;
import com.zs.admin.models.Player;

import java.util.ArrayList;
import java.util.List;

public class DailyInputFragment extends Fragment {
    private FirestoreRepository repo;
    private Spinner playerSpinner;
    private EditText matchesInput, winsInput, killsInput, assistsInput, damageInput, minutesInput, secondsInput;
    private LinearLayout logContainer;
    private List<MatchRecord> dailyLogs = new ArrayList<>();
    private List<Player> confirmedPlayers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try { repo = new FirestoreRepository(); } catch (Exception e) { return new View(getContext()); }

        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));

        TextView header = new TextView(getContext());
        header.setText("DAILY MATCH INPUT");
        header.setTextColor(Color.parseColor("#38bdf8"));
        header.setTextSize(16);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(header);

        // Form fields
        playerSpinner = new Spinner(getContext());
        root.addView(playerSpinner);

        matchesInput = addField(root, "Matches Played", "1");
        winsInput = addField(root, "Wins", "0");
        killsInput = addField(root, "Total Kills", "0");
        assistsInput = addField(root, "Total Assists", "0");
        damageInput = addField(root, "Total Damage", "0");

        LinearLayout timeRow = new LinearLayout(getContext());
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        minutesInput = addFieldTo(timeRow, "Min", "0");
        secondsInput = addFieldTo(timeRow, "Sec", "0");
        root.addView(timeRow);

        Button submitBtn = new Button(getContext());
        submitBtn.setText("SAVE DAILY ENTRY");
        submitBtn.setTextColor(Color.WHITE);
        submitBtn.setBackgroundColor(Color.parseColor("#10b981"));
        submitBtn.setOnClickListener(v -> submitDailyRecord());
        LinearLayout.LayoutParams submitParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        submitParams.setMargins(0, dp(12), 0, dp(12));
        submitBtn.setLayoutParams(submitParams);
        root.addView(submitBtn);

        // Daily log list
        TextView logHeader = new TextView(getContext());
        logHeader.setText("RECENT ENTRIES");
        logHeader.setTextColor(Color.parseColor("#38bdf8"));
        logHeader.setTextSize(14);
        logHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(logHeader);

        logContainer = new LinearLayout(getContext());
        logContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(logContainer);

        ScrollView scroll = new ScrollView(getContext());
        scroll.addView(root);

        // Listen to users for spinner
        repo.listenUsers(players -> {
            if (!isAdded()) return;
            confirmedPlayers.clear();
            for (Player p : players) {
                if ("confirmed".equals(p.getStatus())) confirmedPlayers.add(p);
            }
            if (getActivity() != null) getActivity().runOnUiThread(this::updateSpinner);
        });

        // Listen to daily logs
        repo.listenDailyLogs(records -> {
            dailyLogs = records;
            if (getActivity() != null) getActivity().runOnUiThread(this::renderLog);
        });

        return scroll;
    }

    private EditText addField(LinearLayout parent, String hint, String defaultVal) {
        EditText et = new EditText(getContext());
        et.setHint(hint);
        et.setText(defaultVal);
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        et.setTextColor(Color.WHITE);
        et.setTextSize(14);
        et.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
        params.setMargins(0, dp(4), 0, dp(4));
        et.setLayoutParams(params);
        parent.addView(et);
        return et;
    }

    private EditText addFieldTo(LinearLayout parent, String hint, String defaultVal) {
        EditText et = new EditText(getContext());
        et.setHint(hint);
        et.setText(defaultVal);
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        et.setTextColor(Color.WHITE);
        et.setTextSize(14);
        et.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            0, dp(44), 1f);
        params.setMargins(0, dp(4), dp(4), dp(4));
        et.setLayoutParams(params);
        parent.addView(et);
        return et;
    }

    private void updateSpinner() {
        if (!isAdded() || getContext() == null) return;
        List<String> names = new ArrayList<>();
        names.add("-- Select Player --");
        for (Player p : confirmedPlayers) names.add(p.getName());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        playerSpinner.setAdapter(adapter);
    }

    private void submitDailyRecord() {
        if (!isAdded() || getContext() == null) return;
        int pos = playerSpinner.getSelectedItemPosition();
        if (pos <= 0) {
            Toast.makeText(getContext(), "Select a player!", Toast.LENGTH_SHORT).show();
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
            @Override public void onSuccess() {
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Saved!", Toast.LENGTH_SHORT).show();
                    matchesInput.setText("1");
                    winsInput.setText("0");
                    killsInput.setText("0");
                    assistsInput.setText("0");
                    damageInput.setText("0");
                    minutesInput.setText("0");
                    secondsInput.setText("0");
                    playerSpinner.setSelection(0);
                });
            }
            @Override public void onFailure(String error) {
                if (getActivity() != null) getActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void renderLog() {
        if (!isAdded() || getContext() == null) return;
        logContainer.removeAllViews();
        for (MatchRecord r : dailyLogs) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(4), 0, dp(4));

            TextView nameTv = new TextView(getContext());
            nameTv.setText(r.getPlayerName());
            nameTv.setTextColor(Color.WHITE);
            nameTv.setTextSize(12);
            nameTv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(nameTv);

            int mins = r.getSurvivalSeconds() / 60;
            int secs = r.getSurvivalSeconds() % 60;
            TextView statsTv = new TextView(getContext());
            statsTv.setText("K:" + r.getKills() + " A:" + r.getAssists() + " D:" + r.getDamage() + " " + mins + "m" + secs + "s");
            statsTv.setTextColor(Color.parseColor("#94a3b8"));
            statsTv.setTextSize(11);
            row.addView(statsTv);

            Button delBtn = new Button(getContext());
            delBtn.setText("X");
            delBtn.setTextSize(10);
            delBtn.setTextColor(Color.WHITE);
            delBtn.setBackgroundColor(Color.parseColor("#ef4444"));
            delBtn.setOnClickListener(v -> repo.deleteDailyRecord(r.getId(), new FirestoreRepository.OnResultCallback() {
                @Override public void onSuccess() {}
                @Override public void onFailure(String e) {}
            }));
            row.addView(delBtn);

            logContainer.addView(row);
        }
    }

    private int parseInt(EditText et) {
        try { return Integer.parseInt(et.getText().toString().trim()); } catch (Exception e) { return 0; }
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
