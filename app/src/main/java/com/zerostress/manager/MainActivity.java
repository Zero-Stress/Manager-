package com.zerostress.manager;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.zerostress.manager.fragments.AnnouncementsFragment;
import com.zerostress.manager.fragments.AttendanceFragment;
import com.zerostress.manager.fragments.ChatFragment;
import com.zerostress.manager.fragments.DailyInputFragment;
import com.zerostress.manager.fragments.LeaderboardFragment;
import com.zerostress.manager.fragments.ProfileFragment;
import com.zerostress.manager.fragments.RegistrationFragment;
import com.zerostress.manager.fragments.ScheduleFragment;
import com.zerostress.manager.fragments.SquadFragment;
import com.zerostress.manager.fragments.TournamentFragment;
import com.zerostress.manager.fragments.AnalyticsFragment;
import com.zerostress.manager.models.Player;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "ZS_Main";
    private FirestoreRepository repo;
    private Player currentUser;
    private SharedPreferences prefs;
    private boolean repoFailed = false;
    private int selectedTab = 0;

    // Navigation buttons
    private TextView tabRanks, tabAnalytics, tabChat, tabSchedule, tabMore;
    private TextView[] tabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("zerostress_prefs", MODE_PRIVATE);

        String userJson = prefs.getString("current_user", null);
        if (userJson == null) {
            goLogin();
            return;
        }

        try {
            currentUser = new Gson().fromJson(userJson, Player.class);
        } catch (Throwable t) {
            Log.e(TAG, "Gson parse failed", t);
            prefs.edit().remove("current_user").apply();
            goLogin();
            return;
        }
        if (currentUser == null) {
            prefs.edit().remove("current_user").apply();
            goLogin();
            return;
        }

        // Inflate layout
        try {
            setContentView(R.layout.activity_main);
        } catch (Throwable t) {
            Log.e(TAG, "Layout inflate FAILED", t);
            Toast.makeText(this, "App error. Please reinstall.", Toast.LENGTH_LONG).show();
            prefs.edit().remove("current_user").apply();
            goLogin();
            return;
        }

        // Init Firestore (non-fatal)
        try {
            repo = new FirestoreRepository();
        } catch (Throwable t) {
            Log.e(TAG, "Firestore init failed (non-fatal)", t);
            repoFailed = true;
        }

        // Action bar
        try {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Zero Stress");
                getSupportActionBar().setSubtitle(currentUser.getRole() != null ? currentUser.getRole().toUpperCase() : "PLAYER");
            }
        } catch (Throwable ignored) {}

        // Build bottom navigation bar programmatically (no XML inflation issues)
        try {
            buildBottomNav();
        } catch (Throwable t) {
            Log.e(TAG, "Bottom nav build failed (non-fatal)", t);
        }

        // Notifications (non-fatal)
        try {
            NotificationHelper nh = new NotificationHelper(this);
            nh.saveUserPhone(currentUser.getPhone());
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
                }
            }
            nh.startLeaderboardListener();
        } catch (Throwable t) {
            Log.e(TAG, "Notification init failed (non-fatal)", t);
        }

        // App updates (non-fatal)
        try {
            AppUpdateManager um = new AppUpdateManager(this);
            boolean isAdmin = "admin".equals(currentUser.getRole());
            um.checkForUpdates(com.zerostress.manager.BuildConfig.VERSION_CODE, isAdmin);
        } catch (Throwable t) {
            Log.e(TAG, "Update manager failed (non-fatal)", t);
        }

        // Load default fragment
        if (savedInstanceState == null) {
            try {
                loadFragment(new LeaderboardFragment(), currentUser);
                selectTab(0);
            } catch (Throwable t) {
                Log.e(TAG, "Fragment load failed (non-fatal)", t);
            }
        }
    }

    private void buildBottomNav() {
        LinearLayout container = (LinearLayout) findViewById(R.id.fragment_container);
        if (container == null) return;
        LinearLayout root = (LinearLayout) container.getParent();
        if (root == null) return;

        // Create bottom bar
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setBackgroundColor(Color.parseColor("#0a0f1c"));

        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        bar.setLayoutParams(barParams);

        // Divider line above bar
        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#1e3a5f"));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        root.addView(divider, dividerParams);

        // Tab data
        String[] labels = {"Ranks", "Analytics", "Chat", "Schedule", "More"};
        tabRanks = createTab(labels[0], 0);
        tabAnalytics = createTab(labels[1], 1);
        tabChat = createTab(labels[2], 2);
        tabSchedule = createTab(labels[3], 3);
        tabMore = createTab(labels[4], 4);
        tabs = new TextView[]{tabRanks, tabAnalytics, tabChat, tabSchedule, tabMore};

        bar.addView(tabRanks);
        bar.addView(tabAnalytics);
        bar.addView(tabChat);
        bar.addView(tabSchedule);
        bar.addView(tabMore);

        root.addView(bar);
    }

    private TextView createTab(String label, int index) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(Color.parseColor("#475569"));
        tv.setTextSize(10);
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(null, Typeface.NORMAL);
        tv.setPadding(dp(8), dp(10), dp(8), dp(10));
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
            case 0: fragment = new LeaderboardFragment(); break;
            case 1: fragment = new AnalyticsFragment(); break;
            case 2: fragment = new ChatFragment(); break;
            case 3: fragment = new ScheduleFragment(); break;
            case 4: showMoreMenu(); return;
        }

        if (fragment != null) {
            loadFragment(fragment, currentUser);
        }
    }

    private void showMoreMenu() {
        PopupMenu popup = new PopupMenu(this, tabs[4]);
        popup.getMenu().add(0, 1, 0, "My Profile");
        popup.getMenu().add(0, 2, 1, "My Squad");

        if ("admin".equals(currentUser.getRole())) {
            popup.getMenu().add(0, 10, 10, "--- ADMIN ---").setEnabled(false);
            popup.getMenu().add(0, 3, 2, "Players");
            popup.getMenu().add(0, 4, 3, "Daily Input");
            popup.getMenu().add(0, 5, 4, "Announcements");
            popup.getMenu().add(0, 6, 5, "Tournaments");
            popup.getMenu().add(0, 7, 6, "Attendance");
            popup.getMenu().add(0, 8, 7, "Squad Manager");
            popup.getMenu().add(0, 9, 8, "App Customizer");
            popup.getMenu().add(0, 11, 9, "Push Update");
        }

        popup.setOnMenuItemClickListener(item -> {
            Fragment fragment = null;
            int id = item.getItemId();
            if (id == 1) fragment = new ProfileFragment();
            else if (id == 2) fragment = new SquadFragment();
            else if (id == 3) fragment = new RegistrationFragment();
            else if (id == 4) fragment = new DailyInputFragment();
            else if (id == 5) fragment = new AnnouncementsFragment();
            else if (id == 6) fragment = new TournamentFragment();
            else if (id == 7) fragment = new AttendanceFragment();
            else if (id == 8) fragment = new SquadFragment();
            else if (id == 9) {
                try {
                    startActivity(new Intent(MainActivity.this, AdminCustomizerActivity.class));
                } catch (Throwable e) {
                    Toast.makeText(this, "Failed to open customizer", Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (id == 11) {
                showPushUpdateDialog();
                return true;
            }

            if (fragment != null) loadFragment(fragment, currentUser);
            return true;
        });
        popup.show();
    }

    private void showPushUpdateDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Push App Update");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        TextView infoTv = new TextView(this);
        infoTv.setText("Set a new version. All players will be prompted to update.");
        infoTv.setTextColor(Color.parseColor("#94a3b8"));
        infoTv.setTextSize(12);
        layout.addView(infoTv);

        EditText versionCodeInput = new EditText(this);
        versionCodeInput.setHint("Version Code (number)");
        versionCodeInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(versionCodeInput);

        EditText versionNameInput = new EditText(this);
        versionNameInput.setHint("Version Name");
        layout.addView(versionNameInput);

        EditText downloadUrlInput = new EditText(this);
        downloadUrlInput.setHint("APK Download URL");
        downloadUrlInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI);
        layout.addView(downloadUrlInput);

        EditText changelogInput = new EditText(this);
        changelogInput.setHint("Changelog");
        changelogInput.setMinLines(3);
        layout.addView(changelogInput);

        SwitchCompat forceSwitch = new SwitchCompat(this);
        forceSwitch.setText("Force Update");
        forceSwitch.setTextColor(Color.parseColor("#f1f5f9"));
        forceSwitch.setTextSize(13);
        layout.addView(forceSwitch);

        builder.setView(layout);

        builder.setPositiveButton("Push Update", (d, w) -> {
            String vc = versionCodeInput.getText().toString().trim();
            String vn = versionNameInput.getText().toString().trim();
            String url = downloadUrlInput.getText().toString().trim();
            String cl = changelogInput.getText().toString().trim();
            boolean force = forceSwitch.isChecked();

            if (vc.isEmpty() || vn.isEmpty() || url.isEmpty()) {
                Toast.makeText(this, "Fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int versionCode = Integer.parseInt(vc);
                AppUpdateManager.setUpdateInfo(versionCode, vn, url, cl.isEmpty() ? "Bug fixes" : cl, force,
                    new AppUpdateManager.OnUpdateSetCallback() {
                        @Override public void onSuccess() {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this,
                                "Update pushed!", Toast.LENGTH_LONG).show());
                        }
                        @Override public void onFailure(String e) {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this,
                                "Failed: " + e, Toast.LENGTH_SHORT).show());
                        }
                    });
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Version code must be a number", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void loadFragment(Fragment fragment, Player user) {
        if (user == null) return;
        Bundle args = new Bundle();
        args.putString("userName", user.getName() != null ? user.getName() : "");
        args.putString("userPhone", user.getPhone() != null ? user.getPhone() : "");
        args.putString("userRole", user.getRole() != null ? user.getRole() : "player");
        fragment.setArguments(args);

        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commitAllowingStateLoss();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        try {
            getMenuInflater().inflate(R.menu.main_menu, menu);
        } catch (Throwable ignored) {}
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure?")
                .setPositiveButton("Logout", (d, w) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        if (currentUser != null && !repoFailed && repo != null) {
            try { repo.updatePresence(currentUser.getPhone(), false); } catch (Throwable ignored) {}
        }
        prefs.edit().remove("current_user").apply();
        goLogin();
    }

    private void goLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            value, getResources().getDisplayMetrics());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentUser != null && !repoFailed && repo != null) {
            try { repo.updatePresence(currentUser.getPhone(), false); } catch (Throwable ignored) {}
        }
    }
}
