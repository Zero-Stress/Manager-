package com.zerostress.manager.fragments;

import android.graphics.Color;
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
import com.zerostress.manager.models.MatchRecord;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {
    private FirestoreRepository repo;
    private String userName = "";
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
            userRole = getArguments().getString("userRole", "player");
        }

        TextView nameTv = view.findViewById(R.id.profile_name);
        TextView roleTv = view.findViewById(R.id.profile_role);
        if (nameTv != null) nameTv.setText(userName);
        if (roleTv != null) roleTv.setText(userRole.equals("admin") ? "Administrator" : "Player");

        repo.listenDailyLogs(records -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> calculateStats(records));
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
        int score = Math.round((tKills * 10) + (tDamage / 100f) + (tWins * 50));

        statsContainer.removeAllViews();
        addStatCard("Lifetime Matches", String.valueOf(tMatches));
        addStatCard("Lifetime Wins", String.valueOf(tWins));
        addStatCard("Win Rate", winRate + "%");
        addStatCard("Total Kills", String.valueOf(tKills));
        addStatCard("Total Assists", String.valueOf(tAssists));
        addStatCard("Total Damage", String.valueOf(tDamage));
        addStatCard("Avg Damage/Match", String.valueOf(avgDamage));
        addStatCard("Lifetime Score", score + " pts");
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
        valueTv.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(valueTv);

        statsContainer.addView(card);
    }
}
