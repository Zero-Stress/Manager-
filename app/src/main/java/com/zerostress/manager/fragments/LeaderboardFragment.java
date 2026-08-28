package com.zerostress.manager.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.TableLayout;
import android.widget.TableRow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.zerostress.manager.FirestoreRepository;
import com.zerostress.manager.R;
import com.zerostress.manager.models.LeaderboardEntry;
import com.zerostress.manager.models.MatchRecord;
import com.zerostress.manager.models.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeaderboardFragment extends Fragment {
    private FirestoreRepository repo;
    private TableLayout dailyTable, weeklyTable;
    private TextView dailyEmpty, weeklyEmpty;
    private List<MatchRecord> allDailyLogs = new ArrayList<>();
    private List<Player> allPlayers = new ArrayList<>();
    private String userRole = "player";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_leaderboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new FirestoreRepository();

        dailyTable = view.findViewById(R.id.daily_leaderboard_table);
        weeklyTable = view.findViewById(R.id.weekly_leaderboard_table);
        dailyEmpty = view.findViewById(R.id.daily_empty);
        weeklyEmpty = view.findViewById(R.id.weekly_empty);

        if (getArguments() != null) {
            userRole = getArguments().getString("userRole", "player");
        }

        // Listen to daily logs
        repo.listenDailyLogs(records -> {
            allDailyLogs = records;
            requireActivity().runOnUiThread(this::updateLeaderboards);
        });

        // Listen to users for online status
        repo.listenUsers(players -> {
            allPlayers = players;
            requireActivity().runOnUiThread(this::updateLeaderboards);
        });

        // Load weekly data from Firestore
        loadWeeklyData();
    }

    private void loadWeeklyData() {
        repo.loadWeeklyData(weeklyData -> {
            requireActivity().runOnUiThread(() -> {
                // Combine daily logs with saved weekly data for weekly leaderboard
                updateLeaderboards();
            });
        });
    }

    private void updateLeaderboards() {
        // Daily leaderboard
        List<LeaderboardEntry> dailyEntries = FirestoreRepository.calculateLeaderboard(allDailyLogs, allPlayers);
        renderTable(dailyTable, dailyEmpty, dailyEntries, true);

        // Weekly leaderboard (same as daily since weekly is accumulated)
        renderTable(weeklyTable, weeklyEmpty, dailyEntries, false);
    }

    private void renderTable(TableLayout table, TextView emptyView, List<LeaderboardEntry> entries, boolean showAssists) {
        table.removeAllViews();
        if (entries.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            table.setVisibility(View.GONE);
            return;
        }
        emptyView.setVisibility(View.GONE);
        table.setVisibility(View.VISIBLE);

        // Header row
        TableRow header = new TableRow(requireContext());
        header.setBackgroundColor(Color.parseColor("#090d16"));
        addHeaderCell(header, "Rank");
        addHeaderCell(header, "Player");
        addHeaderCell(header, "Matches");
        addHeaderCell(header, "Wins");
        addHeaderCell(header, "Kills");
        if (showAssists) addHeaderCell(header, "Assists");
        addHeaderCell(header, "Damage");
        addHeaderCell(header, "Avg DMG");
        addHeaderCell(header, "Win%");
        addHeaderCell(header, "Score");
        table.addView(header);

        int maxShow = 10;
        for (int i = 0; i < Math.min(entries.size(), maxShow); i++) {
            LeaderboardEntry e = entries.get(i);
            TableRow row = new TableRow(requireContext());

            // Highlight top 3
            if (i == 0) row.setBackgroundColor(Color.parseColor("#1a2940"));
            else if (i == 1) row.setBackgroundColor(Color.parseColor("#1a2535"));
            else if (i == 2) row.setBackgroundColor(Color.parseColor("#1a2230"));

            String medal = i == 0 ? "👑" : (i == 1 ? "🥈" : (i == 2 ? "🥉" : ""));
            addCell(row, medal + " " + e.getRank());
            addCell(row, e.getPlayerName() + (e.isOnline() ? " 🟢" : ""));
            addCell(row, String.valueOf(e.getMatches()));
            addCell(row, String.valueOf(e.getWins()));
            addCell(row, String.valueOf(e.getKills()));
            if (showAssists) addCell(row, String.valueOf(e.getAssists()));
            addCell(row, String.valueOf(e.getDamage()));
            addCell(row, String.valueOf(e.getAvgDamage()));
            addCell(row, e.getWinRate() + "%");

            TextView scoreCell = new TextView(requireContext());
            scoreCell.setText(e.getScorePoints() + " pts");
            scoreCell.setTextColor(Color.parseColor("#38bdf8"));
            scoreCell.setTextSize(12);
            scoreCell.setPadding(8, 12, 8, 12);
            row.addView(scoreCell);

            table.addView(row);
        }
    }

    private void addHeaderCell(TableRow row, String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#38bdf8"));
        tv.setTextSize(11);
        tv.setPadding(8, 12, 8, 12);
        row.addView(tv);
    }

    private void addCell(TableRow row, String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#f1f5f9"));
        tv.setTextSize(12);
        tv.setPadding(8, 12, 8, 12);
        row.addView(tv);
    }
}
