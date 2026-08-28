package com.zerostress.manager;

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

import com.google.gson.Gson;
import com.zerostress.manager.models.Player;

public class LoginActivity extends AppCompatActivity {
    private EditText phoneInput, passwordInput, regNameInput, regPhoneInput, regPasswordInput;
    private Button loginBtn, registerBtn;
    private TextView toggleText;
    private ProgressBar progressBar;
    private boolean isLoginMode = true;
    private FirestoreRepository repo;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        repo = new FirestoreRepository();
        prefs = getSharedPreferences("zerostress_prefs", MODE_PRIVATE);

        // Check if already logged in
        String savedUser = prefs.getString("current_user", null);
        if (savedUser != null) {
            navigateToMain();
            return;
        }

        phoneInput = findViewById(R.id.phone_input);
        passwordInput = findViewById(R.id.password_input);
        regNameInput = findViewById(R.id.reg_name_input);
        regPhoneInput = findViewById(R.id.reg_phone_input);
        regPasswordInput = findViewById(R.id.reg_password_input);
        loginBtn = findViewById(R.id.login_btn);
        registerBtn = findViewById(R.id.register_btn);
        toggleText = findViewById(R.id.toggle_auth_mode);
        progressBar = findViewById(R.id.progress_bar);

        // Login form views
        View loginForm = findViewById(R.id.login_form);
        View registerForm = findViewById(R.id.register_form);

        loginBtn.setOnClickListener(v -> {
            String phone = phoneInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            loginBtn.setEnabled(false);

            repo.login(phone, password, new FirestoreRepository.AuthCallback() {
                @Override
                public void onSuccess(Player player) {
                    runOnUiThread(() -> {
                        saveSession(player);
                        progressBar.setVisibility(View.GONE);
                        loginBtn.setEnabled(true);
                        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    });
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        loginBtn.setEnabled(true);
                        Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });

        registerBtn.setOnClickListener(v -> {
            String name = regNameInput.getText().toString().trim();
            String phone = regPhoneInput.getText().toString().trim();
            String password = regPasswordInput.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 4) {
                Toast.makeText(this, "Password must be at least 4 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            registerBtn.setEnabled(false);

            repo.register(name, phone, password, new FirestoreRepository.AuthCallback() {
                @Override
                public void onSuccess(Player player) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        registerBtn.setEnabled(true);
                        Toast.makeText(LoginActivity.this, "Registration successful! Waiting for admin approval.", Toast.LENGTH_LONG).show();
                        toggleToLogin();
                    });
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        registerBtn.setEnabled(true);
                        Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });

        toggleText.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            if (isLoginMode) {
                toggleToLogin();
            } else {
                loginForm.setVisibility(View.GONE);
                registerForm.setVisibility(View.VISIBLE);
                toggleText.setText("Already have an account? Login");
                loginBtn.setVisibility(View.GONE);
                registerBtn.setVisibility(View.VISIBLE);
            }
        });
    }

    private void toggleToLogin() {
        View loginForm = findViewById(R.id.login_form);
        View registerForm = findViewById(R.id.register_form);
        isLoginMode = true;
        loginForm.setVisibility(View.VISIBLE);
        registerForm.setVisibility(View.GONE);
        toggleText.setText("Don't have an account? Register");
        loginBtn.setVisibility(View.VISIBLE);
        registerBtn.setVisibility(View.GONE);
    }

    private void saveSession(Player player) {
        Gson gson = new Gson();
        prefs.edit().putString("current_user", gson.toJson(player)).apply();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
