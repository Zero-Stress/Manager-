package com.zs.admin.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.zs.admin.FirestoreRepository;

import java.util.Map;

public class WeeklySummaryFragment extends Fragment {
    private FirestoreRepository repo;
    private LinearLayout tableContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try { repo = new FirestoreRepository(); } catch (Exception e) { return new View(getContext()); }

        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));

        TextView header = new TextView(getContext());
        header.setText("WEEKLY SUMMARY");
        header.setTextColor(Color.parseColor("#38bdf8"));
        header.setTextSize(16);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(header);

        TextView subtitle = new TextView(getContext());
        subtitle.setText("Auto-aggregated from daily logs");
        subtitle.setTextColor(Color.parseColor("#94a3b8"));
        subtitle.setTextSize(12);
        subtitle.setPadding(0, dp(4), 0, dp(12));
        root.addView(subtitle);

        // Column headers
        LinearLayout colHeaders = new LinearLayout(getContext());
        colHeaders.setOrientation(LinearLayout.HORIZONTAL);
        addHeaderText(colHeaders, "Player", 1f);
        addHeaderText(colHeaders, "Matches", 0.7f);
        addHeaderText(colHeaders, "Wins", 0.5f);
        addHeaderText(colHeaders, "Kills", 0.5f);
        addHeaderText(colHeaders, "Damage", 0.7f);
        addHeaderText(colHeaders, "Win%", 0.5f);
        root.addView(colHeaders);

        tableContainer = new LinearLayout(getContext());
        tableContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(tableContainer);

        ScrollView scroll = new ScrollView(getContext());
        scroll.addView(root);

        repo.loadWeeklyData(weeklyData -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> renderWeeklyData(weeklyData));
            }
        });

        return scroll;
    }

    private void renderWeeklyData(Map<String, Map<String, Object>> weeklyData) {
        if (!isAdded() || getContext() == null) return;
        tableContainer.removeAllViews();

        if (weeklyData.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("No weekly data yet. Data accumulates from daily entries.");
            empty.setTextColor(Color.parseColor("#94a3b8"));
            empty.setPadding(0, dp(24), 0, 0);
            tableContainer.addView(empty);
            return;
        }

        for (String name : weeklyData.keySet()) {
            Map<String, Object> data = weeklyData.get(name);
            if (data == null) continue;

            long matches = getLong(data, "matches");
            long wins = getLong(data, "wins");
            long kills = getLong(data, "kills");
            long assists = getLong(data, "assists");
            long damage = getLong(data, "damage");
            long survival = getLong(data, "survivalSeconds");

            double avgDmg = matches > 0 ? Math.round((double) damage / matches) : 0;
            double winRate = matches > 0 ? Math.round((double) wins / matches * 1000) / 10.0 : 0;

            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(8), 0, dp(8));

            TextView nameTv = new TextView(getContext());
            nameTv.setText(name);
            nameTv.setTextColor(Color.WHITE);
            nameTv.setTextSize(12);
            nameTv.setTypeface(null, android.graphics.Typeface.BOLD);
            nameTv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(nameTv);

            addCell(row, String.valueOf(matches), 0.7f);
            addCell(row, String.valueOf(wins), 0.5f);
            addCell(row, String.valueOf(kills), 0.5f);
            addCell(row, String.valueOf(damage), 0.7f);

            TextView wrTv = new TextView(getContext());
            wrTv.setText(winRate + "%");
            wrTv.setTextColor(winRate >= 50 ? Color.parseColor("#10b981") : Color.parseColor("#f59e0b"));
            wrTv.setTextSize(11);
            wrTv.setPadding(dp(4), 0, 0, 0);
            wrTv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f));
            row.addView(wrTv);

            tableContainer.addView(row);

            // Divider
            View divider = new View(getContext());
            divider.setBackgroundColor(Color.parseColor("#1e293b"));
            divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
            tableContainer.addView(divider);
        }
    }

    private void addHeaderText(LinearLayout parent, String text, float weight) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#38bdf8"));
        tv.setTextSize(11);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight));
        tv.setPadding(dp(4), 0, 0, dp(4));
        parent.addView(tv);
    }

    private void addCell(LinearLayout parent, String text, float weight) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#94a3b8"));
        tv.setTextSize(11);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight));
        tv.setPadding(dp(4), 0, 0, 0);
        parent.addView(tv);
    }

    private long getLong(Map<String, Object> data, String key) {
        Object val = data.get(key);
        if (val instanceof Number) return ((Number) val).longValue();
        return 0;
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
