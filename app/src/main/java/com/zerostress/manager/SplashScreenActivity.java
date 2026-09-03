package com.zerostress.manager;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashScreenActivity extends AppCompatActivity {

    private TextView tvLogo, tvTitle, tvSubtitle, tvLoadingStatus;
    private View loadingBar;
    private LinearLayout loadingContainer;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private final String[] loadingMessages = {
        "Initializing system...",
        "Loading player data...",
        "Syncing leaderboards...",
        "Connecting to voice servers...",
        "Preparing battle arena...",
        "Almost ready..."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        startLoadingAnimation();
    }

    private void initViews() {
        tvLogo = findViewById(R.id.tvLogo);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvLoadingStatus = findViewById(R.id.tvLoadingStatus);
        loadingBar = findViewById(R.id.loadingBar);
        loadingContainer = (LinearLayout) loadingBar.getParent().getParent();
    }

    private void startLoadingAnimation() {
        // Animate logo
        ObjectAnimator logoFadeIn = ObjectAnimator.ofFloat(tvLogo, "alpha", 0f, 1f);
        logoFadeIn.setDuration(500);

        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(tvLogo, "scaleX", 0.5f, 1f);
        logoScaleX.setDuration(500);

        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(tvLogo, "scaleY", 0.5f, 1f);
        logoScaleY.setDuration(500);

        AnimatorSet logoSet = new AnimatorSet();
        logoSet.playTogether(logoFadeIn, logoScaleX, logoScaleY);
        logoSet.setInterpolator(new AccelerateDecelerateInterpolator());
        logoSet.start();

        // Animate title after logo
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            ObjectAnimator titleFadeIn = ObjectAnimator.ofFloat(tvTitle, "alpha", 0f, 1f);
            titleFadeIn.setDuration(400);
            titleFadeIn.start();

            // Animate subtitle
            ObjectAnimator subtitleFadeIn = ObjectAnimator.ofFloat(tvSubtitle, "alpha", 0f, 1f);
            subtitleFadeIn.setDuration(400);
            subtitleFadeIn.start();

            // Show loading container
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                ObjectAnimator loadingFadeIn = ObjectAnimator.ofFloat(loadingContainer, "alpha", 0f, 1f);
                loadingFadeIn.setDuration(300);
                loadingFadeIn.start();

                // Start loading simulation
                startLoadingSimulation();
            }, 300);
        }, 600);
    }

    private void startLoadingSimulation() {
        final int[] progress = {0};
        final int[] messageIndex = {0};

        Handler handler = new Handler(Looper.getMainLooper());

        Runnable loadingRunnable = new Runnable() {
            @Override
            public void run() {
                if (progress[0] <= 100) {
                    // Update loading bar
                    float width = (progress[0] / 100f) * loadingBar.getParent() != null ? 
                        ((View) loadingBar.getParent()).getWidth() : 200;
                    loadingBar.getLayoutParams().width = (int) (width * (progress[0] / 100f));
                    loadingBar.requestLayout();

                    // Update loading message
                    if (messageIndex[0] < loadingMessages.length && progress[0] % 20 == 0) {
                        tvLoadingStatus.setText(loadingMessages[messageIndex[0]]);
                        messageIndex[0]++;
                    }

                    progress[0] += 5;
                    handler.postDelayed(this, 100);
                } else {
                    // Loading complete - check auth and navigate
                    navigateToMain();
                }
            }
        };

        handler.post(loadingRunnable);
    }

    private void navigateToMain() {
        tvLoadingStatus.setText("Ready!");

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent;

            if (auth.getCurrentUser() != null) {
                // User is logged in - check role
                String userId = auth.getCurrentUser().getUid();
                
                db.collection("players").document(userId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String role = doc.getString("role");
                            if ("admin".equals(role)) {
                                intent = new Intent(SplashScreenActivity.this, AdminDashboardActivity.class);
                            } else {
                                intent = new Intent(SplashScreenActivity.this, PlayerDashboardActivity.class);
                            }
                        } else {
                            intent = new Intent(SplashScreenActivity.this, LoginActivity.class);
                        }
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        intent = new Intent(SplashScreenActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    });
            } else {
                // User not logged in
                intent = new Intent(SplashScreenActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        }, 500);
    }

    @Override
    public void onBackPressed() {
        // Disable back button on splash screen
    }
}
