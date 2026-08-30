package com.zerostress.manager.fragments;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

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

public class AnalyticsFragment extends Fragment {
    private FirestoreRepository repo;
    private LinearLayout chartsContainer;
    private String userRole = "player";
    private String userName = "";
    private List<MatchRecord> allLogs = new ArrayList<>();
    private List<Player> allPlayers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analytics, container, false);
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
        chartsContainer = view.findViewById(R.id.charts_container);

        if (getArguments() != null) {
            userRole = getArguments().getString("userRole", "player");
            userName = getArguments().getString("userName", "");
        }

        repo.listenDailyLogs(records -> {
            allLogs = records;
            if (getActivity() != null) getActivity().runOnUiThread(this::renderAnalytics);
        });

        repo.listenUsers(players -> {
            allPlayers = players;
            if (getActivity() != null) getActivity().runOnUiThread(this::renderAnalytics);
        });
    }

    private void renderAnalytics() {
        if (!isAdded() || getContext() == null) return;
        chartsContainer.removeAllViews();

        // Section: Team Overview
        addSectionHeader("📊 Team Overview");

        int totalMatches = 0, totalKills = 0, totalDamage = 0, totalWins = 0, totalAssists = 0;
        for (MatchRecord r : allLogs) {
            totalMatches += r.getMatches();
            totalKills += r.getKills();
            totalDamage += r.getDamage();
            totalWins += r.getWins();
            totalAssists += r.getAssists();
        }

        int activePlayers = 0;
        for (Player p : allPlayers) {
            if ("confirmed".equals(p.getStatus())) activePlayers++;
        }

        addStatBar("Active Players", activePlayers, Math.max(activePlayers, 20), "#38bdf8");
        addStatBar("Total Matches", totalMatches, Math.max(totalMatches, 50), "#10b981");
        addStatBar("Total Kills", totalKills, Math.max(totalKills, 500), "#ef4444");
        addStatBar("Total Damage", totalDamage, Math.max(totalDamage, 50000), "#f59e0b");
        addStatBar("Total Wins", totalWins, Math.max(totalWins, 50), "#8b5cf6");

        // Section: Top Performers
        addSectionHeader("🏆 Top Performers");

        List<LeaderboardEntry> leaderboard = FirestoreRepository.calculateLeaderboard(allLogs, allPlayers);
        int showCount = Math.min(5, leaderboard.size());
        for (int i = 0; i < showCount; i++) {
            LeaderboardEntry e = leaderboard.get(i);
            String medal = i == 0 ? "🥇" : (i == 1 ? "🥈" : (i == 2 ? "🥉" : "  "));
            addPerformerCard(medal + " #" + (i + 1) + " " + e.getPlayerName(),
                "Score: " + e.getScorePoints() + " | K/D: " + e.getAvgKills() + " | Win%: " + e.getWinRate() + "%",
                i < 3 ? Color.parseColor("#38bdf8") : Color.parseColor("#94a3b8"));
        }

        // Section: Team Stats Per Player
        if ("admin".equals(userRole)) {
            addSectionHeader("📋 Per-Player Breakdown");

            for (LeaderboardEntry e : leaderboard) {
                addPlayerBreakdownCard(e);
            }
        }

        // Section: Damage Distribution
        addSectionHeader("💥 Damage Distribution");
        if (!leaderboard.isEmpty()) {
            int maxDamage = 0;
            for (LeaderboardEntry e : leaderboard) {
                if (e.getDamage() > maxDamage) maxDamage = e.getDamage();
            }
            for (LeaderboardEntry e : leaderboard) {
                int pct = maxDamage > 0 ? (int) ((float) e.getDamage() / maxDamage * 100) : 0;
                addStatBar(e.getPlayerName(), e.getDamage(), maxDamage, getColorForIndex(leaderboard.indexOf(e)));
            }
        }

        // Section: Win Rate Distribution
        addSectionHeader("📈 Win Rate Distribution");
        for (LeaderboardEntry e : leaderboard) {
            addWinRateBar(e.getPlayerName(), e.getWinRate());
        }
    }

    private void addSectionHeader(String title) {
        TextView header = new TextView(getContext());
        header.setText(title);
        header.setTextColor(Color.parseColor("#38bdf8"));
        header.setTextSize(16);
        header.setTypeface(null, Typeface.BOLD);
        header.setPadding(0, 24, 0, 12);
        chartsContainer.addView(header);
    }

    private void addStatBar(String label, int value, int max, String color) {
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, 4, 0, 8);

        TextView labelTv = new TextView(getContext());
        labelTv.setText(label + ": " + value);
        labelTv.setTextColor(Color.parseColor("#f1f5f9"));
        labelTv.setTextSize(12);
        container.addView(labelTv);

        ProgressBar bar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(max);
        bar.setProgress(Math.min(value, max));
        bar.getProgressDrawable().setColorFilter(Color.parseColor(color), android.graphics.PorterDuff.Mode.SRC_IN);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 24);
        bar.setLayoutParams(params);
        container.addView(bar);

        chartsContainer.addView(container);
    }

    private void addWinRateBar(String name, double winRate) {
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, 4, 0, 8);

        TextView labelTv = new TextView(getContext());
        labelTv.setText(name + ": " + winRate + "%");
        labelTv.setTextColor(Color.parseColor("#f1f5f9"));
        labelTv.setTextSize(12);
        container.addView(labelTv);

        ProgressBar bar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress((int) winRate);
        bar.getProgressDrawable().setColorFilter(Color.parseColor("#10b981"), android.graphics.PorterDuff.Mode.SRC_IN);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 24);
        bar.setLayoutParams(params);
        container.addView(bar);

        chartsContainer.addView(container);
    }

    private void addPerformerCard(String name, String stats, int color) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#0f1729"));
        card.setPadding(16, 12, 16, 12);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 8);
        card.setLayoutParams(params);

        TextView nameTv = new TextView(getContext());
        nameTv.setText(name);
        nameTv.setTextColor(color);
        nameTv.setTextSize(14);
        nameTv.setTypeface(null, Typeface.BOLD);
        card.addView(nameTv);

        TextView statsTv = new TextView(getContext());
        statsTv.setText(stats);
        statsTv.setTextColor(Color.parseColor("#94a3b8"));
        statsTv.setTextSize(11);
        card.addView(statsTv);

        chartsContainer.addView(card);
    }

    private void addPlayerBreakdownCard(LeaderboardEntry entry) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#0f1729"));
        card.setPadding(16, 12, 16, 12);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 8);
        card.setLayoutParams(params);

        TextView nameTv = new TextView(getContext());
        nameTv.setText(entry.getPlayerName());
        nameTv.setTextColor(Color.parseColor("#38bdf8"));
        nameTv.setTextSize(14);
        nameTv.setTypeface(null, Typeface.BOLD);
        card.addView(nameTv);

        TextView statsTv = new TextView(getContext());
        statsTv.setText("Matches: " + entry.getMatches() + " | Wins: " + entry.getWins() + " | Kills: " + entry.getKills());
        statsTv.setTextColor(Color.parseColor("#f1f5f9"));
        statsTv.setTextSize(11);
        card.addView(statsTv);

        TextView moreTv = new TextView(getContext());
        moreTv.setText("Assists: " + entry.getAssists() + " | Damage: " + entry.getDamage() + " | Score: " + entry.getScorePoints());
        moreTv.setTextColor(Color.parseColor("#94a3b8"));
        moreTv.setTextSize(11);
        card.addView(moreTv);

        chartsContainer.addView(card);
    }

    private String getColorForIndex(int index) {
        String[] colors = {"#38bdf8", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#ec4899"};
        return colors[index % colors.length];
    }
}
