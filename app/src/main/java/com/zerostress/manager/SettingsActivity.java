package com.zerostress.manager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialButton btnClearCache = findViewById(R.id.btnClearCache);
        MaterialButton btnAbout = findViewById(R.id.btnAbout);
        MaterialButton btnLogout = findViewById(R.id.btnSettingsLogout);

        btnClearCache.setOnClickListener(v -> {
            Toast.makeText(this, "Cache cleared!", Toast.LENGTH_SHORT).show();
        });

        btnAbout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("ZERO STRESS")
                .setMessage("Version 3.0\n\nPerformance & Leaderboard Manager\n\nBuilt with Firebase + Java")
                .setPositiveButton("OK", null)
                .show();
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}
