package com.zerostress.manager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.zerostress.manager.fragments.AnnouncementsFragment;
import com.zerostress.manager.fragments.DailyInputFragment;
import com.zerostress.manager.fragments.LeaderboardFragment;
import com.zerostress.manager.fragments.ProfileFragment;
import com.zerostress.manager.fragments.RegistrationFragment;
import com.zerostress.manager.models.Player;

public class MainActivity extends AppCompatActivity {
    private FirestoreRepository repo;
    private Player currentUser;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repo = new FirestoreRepository();
        prefs = getSharedPreferences("zerostress_prefs", MODE_PRIVATE);

        // Load current user
        String userJson = prefs.getString("current_user", null);
        if (userJson == null) {
            navigateToLogin();
            return;
        }

        try {
            currentUser = new Gson().fromJson(userJson, Player.class);
        } catch (Exception e) {
            navigateToLogin();
            return;
        }

        if (currentUser == null) {
            navigateToLogin();
            return;
        }

        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Zero Stress");
            getSupportActionBar().setSubtitle(currentUser.getRole().toUpperCase());
        }

        // Setup presence
        repo.updatePresence(currentUser.getPhone(), true);

        // Setup bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        setupNavigation(bottomNav);

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new LeaderboardFragment(), currentUser);
        }
    }

    private void setupNavigation(BottomNavigationView bottomNav) {
        Menu menu = bottomNav.getMenu();

        // Hide admin-only items for players
        if (!"admin".equals(currentUser.getRole())) {
            menu.findItem(R.id.nav_register).setVisible(false);
            menu.findItem(R.id.nav_daily_input).setVisible(false);
            menu.findItem(R.id.nav_announcements).setVisible(false);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_leaderboard) {
                fragment = new LeaderboardFragment();
            } else if (id == R.id.nav_register) {
                fragment = new RegistrationFragment();
            } else if (id == R.id.nav_daily_input) {
                fragment = new DailyInputFragment();
            } else if (id == R.id.nav_profile) {
                fragment = new ProfileFragment();
            } else if (id == R.id.nav_announcements) {
                fragment = new AnnouncementsFragment();
            }

            if (fragment != null) {
                loadFragment(fragment, currentUser);
                return true;
            }
            return false;
        });
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
        if (currentUser != null) {
            repo.updatePresence(currentUser.getPhone(), false);
        }
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
        if (currentUser != null) {
            repo.updatePresence(currentUser.getPhone(), false);
        }
    }
}
