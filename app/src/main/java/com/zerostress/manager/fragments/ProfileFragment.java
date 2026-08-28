package com.zerostress.manager.fragments;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.zerostress.manager.FirestoreRepository;
import com.zerostress.manager.R;
import com.zerostress.manager.models.Achievement;
import com.zerostress.manager.models.MatchRecord;
import com.zerostress.manager.models.Player;

import java.util.List;

public class ProfileFragment extends Fragment {
    private FirestoreRepository repo;
    private String userName = "";
    private String userPhone = "";
    private String userRole = "player";
    private LinearLayout statsContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new FirestoreRepository();
        statsContainer = view.findViewById(R.id.stats_container);

        if (getArguments() != null) {
            userName = getArguments().getString("userName", "");
            userPhone = getArguments().getString("userPhone", "");
            userRole = getArguments().getString("userRole", "player");
        }

        TextView nameTv = view.findViewById(R.id.profile_name);
        TextView roleTv = view.findViewById(R.id.profile_role);
        if (nameTv != null) nameTv.setText(userName);
        if (roleTv != null) {
            String roleLabel = userRole.equals("admin") ? "Administrator" : "Player";
            roleTv.setText(roleLabel);
        }

        repo.listenDailyLogs(records -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    calculateStats(records);
                    checkAchievements(records);
                });
            }
        });
    }

    private void calculateStats(List<MatchRecord> records) {
        if (!isAdded() || getContext() == null) return;

        int tMatches = 0, tWins = 0, tKills = 0, tAssists = 0, tDamage = 0;
        for (MatchRecord r : records) {
            if (userRole.equals("admin") || (r.getPlayerName() != null && r.getPlayerName().equals(userName))) {
                tMatches += r.getMatches();
                tWins += r.getWins();
                tKills += r.getKills();
                tAssists += r.getAssists();
                tDamage += r.getDamage();
            }
        }

        double winRate = tMatches > 0 ? Math.round((float) tWins / tMatches * 1000) / 10.0 : 0;
        int avgDamage = tMatches > 0 ? Math.round((float) tDamage / tMatches) : 0;
        double avgKills = tMatches > 0 ? Math.round((float) tKills / tMatches * 10) / 10.0 : 0;
        int score = Math.round((tKills * 10) + (tDamage / 100f) + (tWins * 50));

        statsContainer.removeAllViews();

        if (!userRole.equals("admin")) {
            addRoleBadge();
        }

        addSectionHeader("Lifetime Stats");
        addStatCard("Lifetime Matches", String.valueOf(tMatches));
        addStatCard("Lifetime Wins", String.valueOf(tWins));
        addStatCard("Win Rate", winRate + "%");
        addStatCard("Total Kills", String.valueOf(tKills));
        addStatCard("Total Assists", String.valueOf(tAssists));
        addStatCard("Total Damage", String.valueOf(tDamage));
        addStatCard("Avg Damage/Match", String.valueOf(avgDamage));
        addStatCard("Avg Kills/Match", String.valueOf(avgKills));
        addStatCard("Lifetime Score", score + " pts");
    }

    private void addRoleBadge() {
        if (getContext() == null) return;

        LinearLayout badge = new LinearLayout(getContext());
        badge.setOrientation(LinearLayout.HORIZONTAL);
        badge.setBackgroundColor(Color.parseColor("#0f1729"));
        badge.setPadding(16, 12, 16, 12);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        badge.setLayoutParams(params);

        TextView emojiTv = new TextView(getContext());
        emojiTv.setText("\uD83E\uDDE0");
        emojiTv.setTextSize(18);
        badge.addView(emojiTv);

        TextView roleTv = new TextView(getContext());
        roleTv.setText(" Role: Player");
        roleTv.setTextColor(Color.parseColor("#38bdf8"));
        roleTv.setTextSize(14);
        roleTv.setTypeface(null, Typeface.BOLD);
        badge.addView(roleTv);

        statsContainer.addView(badge);
    }

    private void checkAchievements(List<MatchRecord> records) {
        if (!isAdded() || getContext() == null || userPhone.isEmpty()) return;

        // Store stats in final array so lambdas can capture them
        final int[] stats = {0, 0, 0, 0, 0}; // matches, wins, kills, assists, damage
        for (MatchRecord r : records) {
            if (r.getPlayerName() != null && r.getPlayerName().equals(userName)) {
                stats[0] += r.getMatches();
                stats[1] += r.getWins();
                stats[2] += r.getKills();
                stats[3] += r.getAssists();
                stats[4] += r.getDamage();
            }
        }

        Achievement[] all = Achievement.getAllAchievements();

        repo.loadAchievements(userPhone, unlocked -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    addSectionHeader("Achievements");

                    int unlockedCount = 0;
                    for (Achievement a : all) {
                        boolean fromFirestore = unlocked.containsKey(a.getId()) && unlocked.get(a.getId());
                        boolean fromStats = Achievement.checkAchievement(
                            a, stats[2], stats[1], stats[0], stats[4], stats[3]);
                        final boolean isUnlocked = fromFirestore || fromStats;
                        if (!fromFirestore && fromStats) {
                            repo.unlockAchievement(userPhone, a.getId());
                        }
                        if (isUnlocked) unlockedCount++;
                        addAchievementBadge(a, isUnlocked);
                    }

                    TextView summary = new TextView(getContext());
                    summary.setText("Unlocked: " + unlockedCount + "/" + all.length);
                    summary.setTextColor(Color.parseColor("#94a3b8"));
                    summary.setTextSize(12);
                    summary.setPadding(0, 8, 0, 16);
                    statsContainer.addView(summary);
                });
            }
        });
    }

    private void addSectionHeader(String title) {
        TextView header = new TextView(getContext());
        header.setText(title);
        header.setTextColor(Color.parseColor("#38bdf8"));
        header.setTextSize(16);
        header.setTypeface(null, Typeface.BOLD);
        header.setPadding(0, 20, 0, 8);
        statsContainer.addView(header);
    }

    private void addStatCard(String label, String value) {
        if (getContext() == null) return;

        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#090d16"));
        card.setPadding(24, 20, 24, 20);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 12);
        card.setLayoutParams(params);

        TextView labelTv = new TextView(getContext());
        labelTv.setText(label);
        labelTv.setTextColor(Color.parseColor("#94a3b8"));
        labelTv.setTextSize(11);
        card.addView(labelTv);

        TextView valueTv = new TextView(getContext());
        valueTv.setText(value);
        valueTv.setTextColor(Color.parseColor("#38bdf8"));
        valueTv.setTextSize(22);
        valueTv.setTypeface(null, Typeface.BOLD);
        card.addView(valueTv);

        statsContainer.addView(card);
    }

    private void addAchievementBadge(Achievement a, boolean unlocked) {
        if (getContext() == null) return;

        LinearLayout badge = new LinearLayout(getContext());
        badge.setOrientation(LinearLayout.HORIZONTAL);
        badge.setBackgroundColor(unlocked ? Color.parseColor("#0f2940") : Color.parseColor("#0f1729"));
        badge.setPadding(16, 10, 16, 10);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 6);
        badge.setLayoutParams(params);

        TextView iconTv = new TextView(getContext());
        iconTv.setText(a.getIcon());
        iconTv.setTextSize(18);
        badge.addView(iconTv);

        LinearLayout textLayout = new LinearLayout(getContext());
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        textLayout.setLayoutParams(textParams);
        textLayout.setPadding(12, 0, 0, 0);

        TextView nameTv = new TextView(getContext());
        nameTv.setText(a.getName());
        nameTv.setTextColor(unlocked ? Color.parseColor("#f1f5f9") : Color.parseColor("#64748b"));
        nameTv.setTextSize(13);
        nameTv.setTypeface(null, Typeface.BOLD);
        textLayout.addView(nameTv);

        TextView descTv = new TextView(getContext());
        descTv.setText(a.getDescription());
        descTv.setTextColor(unlocked ? Color.parseColor("#94a3b8") : Color.parseColor("#475569"));
        descTv.setTextSize(11);
        textLayout.addView(descTv);

        badge.addView(textLayout);

        TextView statusTv = new TextView(getContext());
        statusTv.setText(unlocked ? "\u2705" : "\uD83D\uDD12");
        statusTv.setTextSize(14);
        badge.addView(statusTv);

        statsContainer.addView(badge);
    }
}
