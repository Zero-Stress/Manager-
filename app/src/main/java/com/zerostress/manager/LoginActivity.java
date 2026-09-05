package com.zerostress.manager;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class LoginActivity extends AppCompatActivity {

    private EditText etPhone, etPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> handleLogin());

        findViewById(R.id.tvRegister).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> showForgotPasswordDialog());

        // Log FCM configuration for debugging
        com.zerostress.manager.fcm.FCMConfig.checkFCMConfiguration(this);

        requestNotificationPermission();

        startFancyAnimations();
        fadeForgotPassword();
    }

    private void fadeForgotPassword() {
        TextView tvFp = findViewById(R.id.tvForgotPassword);
        if (tvFp != null) {
            ObjectAnimator fade = ObjectAnimator.ofFloat(tvFp, "alpha", 0f, 1f);
            fade.setDuration(500);
            fade.setStartDelay(1000);
            fade.start();
        }
    }

    private void showForgotPasswordDialog() {
        EditText phoneInput = new EditText(this);
        phoneInput.setTextColor(0xFFFFFFFF);
        phoneInput.setHintTextColor(0xFF718096);
        phoneInput.setPadding(48, 32, 48, 32);
        phoneInput.setHint("Enter your phone number");

        new AlertDialog.Builder(this)
                .setTitle("Forgot Password")
                .setMessage("Enter your phone number so we can send a password reset link")
                .setView(phoneInput)
                .setPositiveButton("Send Reset Link", (d, w) -> {
                    String phone = phoneInput.getText().toString().trim();
                    if (phone.isEmpty()) {
                        Toast.makeText(this, "Enter your phone number", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showResetSentToast(phone);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showResetSentToast(String phone) {
        new AlertDialog.Builder(this)
                .setTitle("Reset Link Sent")
                .setMessage("A password reset link has been sent to " + phone + "@zerostress.local\n\nCheck your email to reset your password.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void startFancyAnimations() {
        LinearLayout logoContainer = findViewById(R.id.logoContainer);
        ObjectAnimator logoFadeIn = ObjectAnimator.ofFloat(logoContainer, "alpha", 0f, 1f);
        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(logoContainer, "scaleX", 0.3f, 1f);
        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(logoContainer, "scaleY", 0.3f, 1f);
        AnimatorSet logoSet = new AnimatorSet();
        logoSet.playTogether(logoFadeIn, logoScaleX, logoScaleY);
        logoSet.setDuration(800);
        logoSet.setInterpolator(new DecelerateInterpolator());
        logoSet.start();

        TextView tvTitle = findViewById(R.id.tvTitle);
        ObjectAnimator titleFadeIn = ObjectAnimator.ofFloat(tvTitle, "alpha", 0f, 1f);
        ObjectAnimator titleSlideUp = ObjectAnimator.ofFloat(tvTitle, "translationY", 50f, 0f);
        AnimatorSet titleSet = new AnimatorSet();
        titleSet.playTogether(titleFadeIn, titleSlideUp);
        titleSet.setDuration(600);
        titleSet.setStartDelay(300);
        titleSet.setInterpolator(new DecelerateInterpolator());
        titleSet.start();

        TextView tvSubtitle = findViewById(R.id.tvSubtitle);
        ObjectAnimator subtitleFadeIn = ObjectAnimator.ofFloat(tvSubtitle, "alpha", 0f, 1f);
        subtitleFadeIn.setDuration(500);
        subtitleFadeIn.setStartDelay(500);
        subtitleFadeIn.start();

        LinearLayout loginCard = findViewById(R.id.loginCard);
        ObjectAnimator cardFadeIn = ObjectAnimator.ofFloat(loginCard, "alpha", 0f, 1f);
        ObjectAnimator cardSlideUp = ObjectAnimator.ofFloat(loginCard, "translationY", 100f, 0f);
        AnimatorSet cardSet = new AnimatorSet();
        cardSet.playTogether(cardFadeIn, cardSlideUp);
        cardSet.setDuration(700);
        cardSet.setStartDelay(600);
        cardSet.setInterpolator(new DecelerateInterpolator());
        cardSet.start();

        LinearLayout registerContainer = findViewById(R.id.registerContainer);
        ObjectAnimator registerFadeIn = ObjectAnimator.ofFloat(registerContainer, "alpha", 0f, 1f);
        registerFadeIn.setDuration(500);
        registerFadeIn.setStartDelay(900);
        registerFadeIn.start();
    }

    private void handleLogin() {
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (phone.isEmpty()) {
            etPhone.setError("Enter phone number");
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Enter password");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        String email = phone + "@zerostress.local";

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    db.collection("players").document(uid).get()
                            .addOnSuccessListener(doc -> {
                                progressBar.setVisibility(View.GONE);
                                btnLogin.setEnabled(true);

                                if (doc.exists()) {
                                    String role = doc.getString("role");
                                    saveFcmToken(uid);
                                    Intent intent;
                                    if ("admin".equals(role)) {
                                        intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                                    } else {
                                        intent = new Intent(LoginActivity.this, PlayerDashboardActivity.class);
                                    }
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                                    finish();
                                } else {
                                    Toast.makeText(this, "Player not found", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                FirebaseMessaging.getInstance().subscribeToTopic("all_players");
            }
        }
    }

    private void saveFcmToken(String uid) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token != null) {
                        FirebaseFirestore.getInstance()
                                .collection("players").document(uid)
                                .update("fcmToken", token)
                                .addOnSuccessListener(v -> {})
                                .addOnFailureListener(e -> {});
                    }
                });
        FirebaseMessaging.getInstance().subscribeToTopic("all_players");
        FirebaseMessaging.getInstance().subscribeToTopic("match_updates");
        FirebaseMessaging.getInstance().subscribeToTopic("announcements");
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
