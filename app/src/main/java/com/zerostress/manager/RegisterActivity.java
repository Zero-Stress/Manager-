package com.zerostress.manager;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etPhone, etPassword;
    private ProgressBar progressBar;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnRegister).setOnClickListener(v -> doRegister());
        findViewById(R.id.tvLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void doRegister() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) { etName.setError("Enter name"); return; }
        if (TextUtils.isEmpty(phone)) { etPhone.setError("Enter phone"); return; }
        if (TextUtils.isEmpty(password)) { etPassword.setError("Enter password"); return; }
        if (password.length() < 6) { etPassword.setError("Min 6 characters"); return; }

        String email = phone + "@zerostress.local";
        progressBar.setVisibility(View.VISIBLE);

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(result -> {
                String uid = result.getUser().getUid();
                Map<String, Object> playerData = new HashMap<>();
                playerData.put("uid", uid);
                playerData.put("name", name);
                playerData.put("phone", phone);
                playerData.put("role", "player");
                playerData.put("status", "pending");
                playerData.put("score", 0);
                playerData.put("kills", 0);
                playerData.put("deaths", 0);
                playerData.put("assists", 0);
                playerData.put("damage", 0L);
                playerData.put("wins", 0);
                playerData.put("matches", 0);
                playerData.put("xp", 0);
                playerData.put("level", 1);
                playerData.put("coins", 0);
                playerData.put("rank", "Iron");

                FirebaseFirestore.getInstance().collection("players")
                    .document(uid)
                    .set(playerData)
                    .addOnSuccessListener(v -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Registered! Wait for admin approval.", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }
}
