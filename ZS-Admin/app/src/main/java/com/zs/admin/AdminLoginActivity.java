package com.zs.admin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AdminLoginActivity extends AppCompatActivity {
    private EditText phoneInput, passwordInput;
    private Button loginBtn;
    private ProgressBar progressBar;
    private TextView statusText;
    private SharedPreferences prefs;

    private static final String ADMIN_PHONE = "1757261781";
    private static final String ADMIN_PASSWORD = "adminpassword123";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        prefs = getSharedPreferences("zs_admin_prefs", MODE_PRIVATE);
        String savedAdmin = prefs.getString("admin_name", null);
        if (savedAdmin != null) {
            goToDashboard(savedAdmin);
            return;
        }

        phoneInput = findViewById(R.id.phoneInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginBtn = findViewById(R.id.loginBtn);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);

        loginBtn.setOnClickListener(v -> doLogin());
    }

    private void doLogin() {
        String phone = phoneInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (phone.isEmpty() || password.isEmpty()) {
            showStatus("Fill in all fields");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        loginBtn.setEnabled(false);

        // Hardcoded admin check
        if (phone.equals(ADMIN_PHONE) && password.equals(ADMIN_PASSWORD)) {
            prefs.edit().putString("admin_name", "Admin Master").apply();
            goToDashboard("Admin Master");
        } else {
            progressBar.setVisibility(View.GONE);
            loginBtn.setEnabled(true);
            showStatus("Invalid admin credentials");
        }
    }

    private void goToDashboard(String adminName) {
        Intent intent = new Intent(this, AdminDashboardActivity.class);
        intent.putExtra("adminName", adminName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showStatus(String msg) {
        statusText.setText(msg);
        statusText.setVisibility(View.VISIBLE);
    }
}
