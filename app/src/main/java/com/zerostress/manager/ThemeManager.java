package com.zerostress.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.zerostress.manager.models.AppCustomizer;

/**
 * Singleton that manages the app's dynamic theme.
 * Loads from Firestore, caches locally, provides colors to all fragments.
 */
public class ThemeManager {
    private static ThemeManager instance;
    private AppCustomizer customizer;
    private SharedPreferences prefs;
    private final FirebaseFirestore db;
    private ThemeChangeListener listener;

    public interface ThemeChangeListener {
        void onThemeChanged(AppCustomizer newTheme);
    }

    private ThemeManager(Context context) {
        prefs = context.getSharedPreferences("zerostress_prefs", Context.MODE_PRIVATE);
        db = FirebaseFirestore.getInstance();

        // Load cached theme first (instant)
        String cached = prefs.getString("app_theme", null);
        if (cached != null) {
            try {
                customizer = new Gson().fromJson(cached, AppCustomizer.class);
            } catch (Exception e) {
                customizer = new AppCustomizer();
            }
        } else {
            customizer = new AppCustomizer();
        }

        // Then load from Firestore (latest)
        loadFromFirestore();
    }

    public static synchronized ThemeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeManager(context.getApplicationContext());
        }
        return instance;
    }

    public void setThemeChangeListener(ThemeChangeListener l) {
        this.listener = l;
    }

    public AppCustomizer getTheme() {
        return customizer;
    }

    // ========== FIRESTORE ==========

    public void loadFromFirestore() {
        db.collection("appSettings").document("theme")
            .addSnapshotListener((doc, error) -> {
                if (error != null || doc == null || !doc.exists()) return;

                try {
                    AppCustomizer loaded = doc.toObject(AppCustomizer.class);
                    if (loaded != null) {
                        customizer = loaded;
                        saveLocal();
                        if (listener != null) listener.onThemeChanged(customizer);
                    }
                } catch (Exception e) {
                    // Keep cached version
                }
            });
    }

    public void saveToFirestore(AppCustomizer newTheme, OnSaveCallback callback) {
        db.collection("appSettings").document("theme")
            .set(newTheme)
            .addOnSuccessListener(aVoid -> {
                customizer = newTheme;
                saveLocal();
                if (listener != null) listener.onThemeChanged(newTheme);
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onFailure(e.getMessage());
            });
    }

    public interface OnSaveCallback {
        void onSuccess();
        void onFailure(String error);
    }

    private void saveLocal() {
        prefs.edit().putString("app_theme", new Gson().toJson(customizer)).apply();
    }

    // ========== COLOR GETTERS (convenience) ==========

    public int getPrimaryColorInt() { return Color.parseColor(customizer.getPrimaryColor()); }
    public int getSecondaryColorInt() { return Color.parseColor(customizer.getSecondaryColor()); }
    public int getBackgroundColorInt() { return Color.parseColor(customizer.getBackgroundColor()); }
    public int getCardColorInt() { return Color.parseColor(customizer.getCardColor()); }
    public int getInputColorInt() { return Color.parseColor(customizer.getInputColor()); }
    public int getNavBarColorInt() { return Color.parseColor(customizer.getNavBarColor()); }
    public int getTextColorInt() { return Color.parseColor(customizer.getTextColor()); }
    public int getSecondaryTextColorInt() { return Color.parseColor(customizer.getSecondaryTextColor()); }
    public int getDangerColorInt() { return Color.parseColor(customizer.getDangerColor()); }
    public int getWarningColorInt() { return Color.parseColor(customizer.getWarningColor()); }
    public int getBorderColorInt() { return Color.parseColor(customizer.getBorderColor()); }

    public String getPrimaryColor() { return customizer.getPrimaryColor(); }
    public String getSecondaryColor() { return customizer.getSecondaryColor(); }
    public String getBackgroundColor() { return customizer.getBackgroundColor(); }
    public String getCardColor() { return customizer.getCardColor(); }
    public String getInputColor() { return customizer.getInputColor(); }
    public String getTextColor() { return customizer.getTextColor(); }

    public Typeface getTypeface() {
        switch (customizer.getFontFamily()) {
            case "serif": return Typeface.create("serif", Typeface.NORMAL);
            case "monospace": return Typeface.create("monospace", Typeface.NORMAL);
            default: return Typeface.create("sans-serif", Typeface.NORMAL);
        }
    }

    public Typeface getBoldTypeface() {
        switch (customizer.getFontFamily()) {
            case "serif": return Typeface.create("serif", Typeface.BOLD);
            case "monospace": return Typeface.create("monospace", Typeface.BOLD);
            default: return Typeface.create("sans-serif", Typeface.BOLD);
        }
    }
}
