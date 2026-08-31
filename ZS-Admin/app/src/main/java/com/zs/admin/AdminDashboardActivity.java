package com.zs.admin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.zs.admin.fragments.AdminAnnouncementsFragment;
import com.zs.admin.fragments.AppCustomizerFragment;
import com.zs.admin.fragments.DailyInputFragment;
import com.zs.admin.fragments.PlayerManagementFragment;
import com.zs.admin.fragments.PushUpdateFragment;
import com.zs.admin.fragments.WeeklySummaryFragment;

public class AdminDashboardActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private TextView[] tabs;
    private int selectedTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        prefs = getSharedPreferences("zs_admin_prefs", MODE_PRIVATE);

        String adminName = getIntent().getStringExtra("adminName");
        if (adminName == null) adminName = prefs.getString("admin_name", "Admin");

        TextView adminNameText = findViewById(R.id.adminNameText);
        if (adminNameText != null) adminNameText.setText(adminName);

        View logoutBtn = findViewById(R.id.logoutBtn);
        if (logoutBtn != null) {
            logoutBtn.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure?")
                    .setPositiveButton("Logout", (d, w) -> {
                        prefs.edit().remove("admin_name").apply();
                        startActivity(new Intent(this, AdminLoginActivity.class));
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        }

        buildBottomNav();

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new PlayerManagementFragment());
            selectTab(0);
        }
    }

    private void buildBottomNav() {
        LinearLayout bar = findViewById(R.id.bottom_bar);
        if (bar == null) return;

        String[] labels = {"Players", "Input", "Weekly", "Push", "Theme", "Announce"};
        tabs = new TextView[labels.length];

        LinearLayout root = findViewById(R.id.root_layout);

        // Divider
        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#1e3a5f"));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        root.addView(divider, dividerParams);

        for (int i = 0; i < labels.length; i++) {
            tabs[i] = createTab(labels[i], i);
            bar.addView(tabs[i]);
        }
    }

    private TextView createTab(String label, int index) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(Color.parseColor("#475569"));
        tv.setTextSize(9);
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(null, Typeface.NORMAL);
        tv.setPadding(dp(6), dp(10), dp(6), dp(10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(params);
        tv.setOnClickListener(v -> {
            selectTab(index);
            onTabClicked(index);
        });
        return tv;
    }

    private void selectTab(int index) {
        selectedTab = index;
        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i] != null) {
                if (i == index) {
                    tabs[i].setTextColor(Color.parseColor("#38bdf8"));
                    tabs[i].setTypeface(null, Typeface.BOLD);
                } else {
                    tabs[i].setTextColor(Color.parseColor("#475569"));
                    tabs[i].setTypeface(null, Typeface.NORMAL);
                }
            }
        }
    }

    private void onTabClicked(int index) {
        Fragment fragment = null;
        switch (index) {
            case 0: fragment = new PlayerManagementFragment(); break;
            case 1: fragment = new DailyInputFragment(); break;
            case 2: fragment = new WeeklySummaryFragment(); break;
            case 3: fragment = new PushUpdateFragment(); break;
            case 4: fragment = new AppCustomizerFragment(); break;
            case 5: fragment = new AdminAnnouncementsFragment(); break;
        }
        if (fragment != null) loadFragment(fragment);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commitAllowingStateLoss();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            value, getResources().getDisplayMetrics());
    }
}
