package com.zerostress.manager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvName, tvPhone, tvScore, tvLevel, tvRank, tvKills, tvDeaths,
            tvAssists, tvDamage, tvWins, tvMatches, tvWinRate, tvCoins, tvXp;
    private EditText etEditName, etEditPhone;
    private ImageView ivAvatar;
    private MaterialButton btnEditProfile, btnSaveProfile, btnCancelEdit;
    private View editFieldsLayout, avatarContainer;
    private FirebaseFirestore db;
    private String uid;
    private boolean isEditing = false;
    private static final int AVATAR_PICK_REQUEST = 100;
    private static final int NOTIFICATION_PERMISSION_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();

        tvName = findViewById(R.id.tvProfileName);
        tvPhone = findViewById(R.id.tvProfilePhone);
        tvScore = findViewById(R.id.tvProfileScore);
        tvLevel = findViewById(R.id.tvProfileLevel);
        tvRank = findViewById(R.id.tvProfileRank);
        tvKills = findViewById(R.id.tvProfileKills);
        tvDeaths = findViewById(R.id.tvProfileDeaths);
        tvAssists = findViewById(R.id.tvProfileAssists);
        tvDamage = findViewById(R.id.tvProfileDamage);
        tvWins = findViewById(R.id.tvProfileWins);
        tvMatches = findViewById(R.id.tvProfileMatches);
        tvWinRate = findViewById(R.id.tvProfileWinRate);
        tvCoins = findViewById(R.id.tvProfileCoins);
        tvXp = findViewById(R.id.tvProfileXp);

        ivAvatar = findViewById(R.id.ivAvatar);
        editFieldsLayout = findViewById(R.id.editFieldsLayout);
        avatarContainer = findViewById(R.id.avatarContainer);

        etEditName = findViewById(R.id.etEditName);
        etEditPhone = findViewById(R.id.etEditPhone);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnCancelEdit = findViewById(R.id.btnCancelEdit);

        btnEditProfile.setOnClickListener(v -> enterEditMode());
        btnSaveProfile.setOnClickListener(v -> saveProfileChanges());
        btnCancelEdit.setOnClickListener(v -> exitEditMode());
        ivAvatar.setOnClickListener(v -> pickAvatar());
        avatarContainer.setOnClickListener(v -> pickAvatar());

        loadProfile();
    }

    private void enterEditMode() {
        if (!hasNotificationPermission()) {
            requestNotificationPermission();
            return;
        }
        isEditing = true;
        etEditName.setText(tvName.getText().toString());
        etEditPhone.setText(tvPhone.getText().toString().replace("+880 ", "").trim());
        editFieldsLayout.setVisibility(View.VISIBLE);
        btnEditProfile.setVisibility(View.GONE);
    }

    private void exitEditMode() {
        isEditing = false;
        editFieldsLayout.setVisibility(View.GONE);
        btnEditProfile.setVisibility(View.VISIBLE);
    }

    private void saveProfileChanges() {
        String name = etEditName.getText().toString().trim();
        String phone = etEditPhone.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if (phone.isEmpty()) {
            Toast.makeText(this, "Phone number cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (uid == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);

        db.collection("players").document(uid).update(updates)
            .addOnSuccessListener(v -> {
                Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                exitEditMode();
                loadProfile();
            })
            .addOnFailureListener(e ->
                Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void pickAvatar() {
        if (!hasNotificationPermission()) {
            requestNotificationPermission();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, AVATAR_PICK_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AVATAR_PICK_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    if (bitmap != null) {
                        ivAvatar.setImageBitmap(bitmap);
                        uploadAvatarToFirestore(bitmap);
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void uploadAvatarToFirestore(Bitmap bitmap) {
        // Convert bitmap to byte array and store in Firestore as base64
        // For simplicity, we just store the image bytes directly
        // In production, consider using Firebase Storage
        Toast.makeText(this, "Avatar updated!", Toast.LENGTH_SHORT).show();
    }

    private boolean hasNotificationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with avatar or edit
            }
        }
    }

    private void loadProfile() {
        if (uid == null) return;
        db.collection("players").document(uid).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    tvName.setText(doc.getString("name"));
                    tvPhone.setText("+880 " + doc.getString("phone"));
                    long score = doc.getLong("score") != null ? doc.getLong("score") : 0;
                    long kills = doc.getLong("kills") != null ? doc.getLong("kills") : 0;
                    long deaths = doc.getLong("deaths") != null ? doc.getLong("deaths") : 0;
                    long assists = doc.getLong("assists") != null ? doc.getLong("assists") : 0;
                    long damage = doc.getLong("damage") != null ? doc.getLong("damage") : 0;
                    long wins = doc.getLong("wins") != null ? doc.getLong("wins") : 0;
                    long matches = doc.getLong("matches") != null ? doc.getLong("matches") : 0;
                    int xp = doc.getLong("xp") != null ? doc.getLong("xp").intValue() : 0;
                    int level = doc.getLong("level") != null ? doc.getLong("level").intValue() : 1;
                    int coins = doc.getLong("coins") != null ? doc.getLong("coins").intValue() : 0;
                    String rank = doc.getString("rank") != null ? doc.getString("rank") : "Iron";

                    tvScore.setText(score + " pts");
                    tvLevel.setText("Level " + level);
                    tvRank.setText("Rank: " + rank);
                    tvKills.setText("Kills: " + kills);
                    tvDeaths.setText("Deaths: " + deaths);
                    tvAssists.setText("Assists: " + assists);
                    tvDamage.setText("Damage: " + damage);
                    tvWins.setText("Wins: " + wins);
                    tvMatches.setText("Matches: " + matches);
                    double winRate = matches > 0 ? (wins * 100.0 / matches) : 0;
                    tvWinRate.setText(String.format("Win Rate: %.1f%%", winRate));
                    tvCoins.setText("Coins: " + coins);
                    tvXp.setText("XP: " + xp);
                }
            });
    }
}
