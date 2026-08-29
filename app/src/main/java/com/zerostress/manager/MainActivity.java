package com.zerostress.manager;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
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
    private FirestoreRepository repo;
    private Player currentUser;
    private SharedPreferences prefs;
    private NotificationHelper notificationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repo = new FirestoreRepository();
        prefs = getSharedPreferences("zerostress_prefs", MODE_PRIVATE);

        String userJson = prefs.getString("current_user", null);
        if (userJson == null) { navigateToLogin(); return; }

        try { currentUser = new Gson().fromJson(userJson, Player.class); }
        catch (Exception e) { navigateToLogin(); return; }
        if (currentUser == null) { navigateToLogin(); return; }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Zero Stress");
            getSupportActionBar().setSubtitle(currentUser.getRole().toUpperCase() +
                ("admin".equals(currentUser.getRole()) ? "" : " \u2022 " + currentUser.getRoleLabel()));
        }

        // Setup presence
        repo.updatePresence(currentUser.getPhone(), true);

        // Setup notification polling - monitors all Firestore changes
        setupNotifications();

        // Setup bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        setupNavigation(bottomNav);

        if (savedInstanceState == null) {
            loadFragment(new LeaderboardFragment(), currentUser);
        }
    }

    private void setupNotifications() {
        notificationHelper = new NotificationHelper(this);
        notificationHelper.saveUserPhone(currentUser.getPhone());
        notificationHelper.setLeaderboardUpdateCallback((type, message) -> {
            // Show in-app toast when data changes
            runOnUiThread(() -> {
                android.widget.Toast.makeText(this, "\uD83D\uDD14 " + message, android.widget.Toast.LENGTH_SHORT).show();
            });
        });

        // Request notification permission on Android 13+
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }

        // Start polling - checks Firestore every 30 seconds
        notificationHelper.startLeaderboardListener();
    }

    private void setupNavigation(BottomNavigationView bottomNav) {
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_leaderboard) {
                fragment = new LeaderboardFragment();
            } else if (id == R.id.nav_analytics) {
                fragment = new AnalyticsFragment();
            } else if (id == R.id.nav_chat) {
                fragment = new ChatFragment();
            } else if (id == R.id.nav_schedule) {
                fragment = new ScheduleFragment();
            } else if (id == R.id.nav_more) {
                showMoreMenu();
                return true;
            }

            if (fragment != null) {
                loadFragment(fragment, currentUser);
                return true;
            }
            return false;
        });
    }

    private void showMoreMenu() {
        PopupMenu popup = new PopupMenu(this, findViewById(R.id.nav_more));
        popup.getMenu().add(0, 1, 0, "\uD83D\uDC64 My Profile");
        popup.getMenu().add(0, 2, 1, "\u2694\uFE0F My Squad");

        if ("admin".equals(currentUser.getRole())) {
            popup.getMenu().add(0, 10, 10, "\u2500\u2500\u2500 ADMIN \u2500\u2500\u2500").setEnabled(false);
            popup.getMenu().add(0, 3, 2, "\uD83D\uDCCB Players");
            popup.getMenu().add(0, 4, 3, "\uD83D\uDCCA Daily Input");
            popup.getMenu().add(0, 5, 4, "\uD83D\uDCE2 Announcements");
            popup.getMenu().add(0, 6, 5, "\uD83C\uDFC6 Tournaments");
            popup.getMenu().add(0, 7, 6, "\u23F0 Attendance");
            popup.getMenu().add(0, 8, 7, "\u2694\uFE0F Squad Manager");
            popup.getMenu().add(0, 9, 8, "\u2699\uFE0F App Customizer");
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
                startActivity(new Intent(MainActivity.this, AdminCustomizerActivity.class));
                return true;
            }

            if (fragment != null) loadFragment(fragment, currentUser);
            return true;
        });
        popup.show();
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
            .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (d, w) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        if (currentUser != null) repo.updatePresence(currentUser.getPhone(), false);
        if (notificationHelper != null) notificationHelper.cleanup();
        prefs.edit().remove("current_user").apply();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentUser != null) repo.updatePresence(currentUser.getPhone(), false);
        if (notificationHelper != null) notificationHelper.cleanup();
    }
}
