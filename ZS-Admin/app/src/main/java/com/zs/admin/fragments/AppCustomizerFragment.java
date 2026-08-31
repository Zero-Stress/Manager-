package com.zs.admin.fragments;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.zs.admin.FirestoreRepository;

import java.util.HashMap;
import java.util.Map;

public class AppCustomizerFragment extends Fragment {
    private FirestoreRepository repo;

    // Color values (default gaming theme)
    private int primaryColor = Color.parseColor("#38bdf8");
    private int secondaryColor = Color.parseColor("#a855f7");
    private int bgColor = Color.parseColor("#090d16");
    private int cardColor = Color.parseColor("#0f1729");
    private int successColor = Color.parseColor("#10b981");
    private int warningColor = Color.parseColor("#f59e0b");
    private int errorColor = Color.parseColor("#ef4444");
    private int textPrimaryColor = Color.parseColor("#f1f5f9");
    private int textSecondaryColor = Color.parseColor("#94a3b8");

    // Size values
    private int buttonHeight = 48;
    private int buttonRadius = 12;
    private int iconSize = 24;
    private int headerTextSize = 18;
    private int bodyTextSize = 14;
    private int inputHeight = 48;
    private int cardPadding = 16;

    // Text values
    private String appName = "Zero Stress";
    private String welcomeMessage = "Welcome to Zero Stress Manager!";

    // Views
    private LinearLayout previewContainer;

    private static final int[] COLOR_PRESET_KEYS = {0, 1, 2, 3, 4};
    private static final String[] COLOR_PRESET_NAMES = {"Default", "Neon", "Fire", "Forest", "Purple"};
    private static final int[][] COLOR_PRESET_VALUES = {
        {0xFF38bdf8, 0xFFa855f7, 0xFF090d16, 0xFF0f1729, 0xFF10b981, 0xFFf59e0b, 0xFFef4444, 0xFFf1f5f9, 0xFF94a3b8},
        {0xFF00ff88, 0xFF00ccff, 0xFF0a0a1a, 0xFF111122, 0xFF00ff44, 0xFFffff00, 0xFFFF0044, 0xFFFFFFFF, 0xFF88ffcc},
        {0xFFff6633, 0xFFff3300, 0xFF1a0a00, 0xFF2a1100, 0xFFff9900, 0xFFffcc00, 0xFFFF0000, 0xFFfff5ee, 0xFFffbb88},
        {0xFF22c55e, 0xFF14b8a6, 0xFF0a1a0f, 0xFF0f2918, 0xFF4ade80, 0xFFfbbf24, 0xFFf87171, 0xFFf0fdf4, 0xFFa7f3d0},
        {0xFFc084fc, 0xFFe879f9, 0xFF0f0a1a, 0xFF1a1029, 0xFFa78bfa, 0xFFfbbf24, 0xFFf87171, 0xFFfaf5ff, 0xFFd8b4fe}
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try { repo = new FirestoreRepository(); } catch (Exception e) { return new View(getContext()); }

        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));

        // Header
        TextView header = new TextView(getContext());
        header.setText("APP CUSTOMIZER");
        header.setTextColor(Color.parseColor("#38bdf8"));
        header.setTextSize(16);
        header.setTypeface(null, Typeface.BOLD);
        root.addView(header);

        TextView subtitle = new TextView(getContext());
        subtitle.setText("Customize theme, colors, sizes, and app branding");
        subtitle.setTextColor(Color.parseColor("#94a3b8"));
        subtitle.setTextSize(12);
        subtitle.setPadding(0, dp(4), 0, dp(8));
        root.addView(subtitle);

        // ===== COLOR PRESETS =====
        addSectionHeader(root, "COLOR PRESETS");
        LinearLayout presetRow = new LinearLayout(getContext());
        presetRow.setOrientation(LinearLayout.HORIZONTAL);
        presetRow.setGravity(Gravity.CENTER);
        for (int i = 0; i < COLOR_PRESET_NAMES.length; i++) {
            final int idx = i;
            Button presetBtn = new Button(getContext());
            presetBtn.setText(COLOR_PRESET_NAMES[i]);
            presetBtn.setTextSize(10);
            presetBtn.setTextColor(Color.WHITE);
            presetBtn.setBackgroundColor(i == 0 ? Color.parseColor("#38bdf8") : Color.parseColor("#1e293b"));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(36), 1f);
            p.setMargins(dp(2), 0, dp(2), 0);
            presetBtn.setLayoutParams(p);
            presetBtn.setOnClickListener(v -> applyPreset(idx));
            presetRow.addView(presetBtn);
        }
        root.addView(presetRow);

        // ===== APP BRANDING =====
        addSectionHeader(root, "APP BRANDING");

        EditText appNameInput = addTextInput(root, "App Name", appName);
        EditText welcomeInput = addTextInput(root, "Welcome Message", welcomeMessage);

        // ===== COLOR PICKERS =====
        addSectionHeader(root, "COLORS");
        TextView primaryLabel = addColorPicker(root, "Primary", primaryColor, c -> { primaryColor = c; updatePreview(); });
        TextView secondaryLabel = addColorPicker(root, "Secondary", secondaryColor, c -> { secondaryColor = c; updatePreview(); });
        TextView bgLabel = addColorPicker(root, "Background", bgColor, c -> { bgColor = c; updatePreview(); });
        TextView cardLabel = addColorPicker(root, "Card", cardColor, c -> { cardColor = c; updatePreview(); });
        TextView successLabel = addColorPicker(root, "Success", successColor, c -> { successColor = c; updatePreview(); });
        TextView warningLabel = addColorPicker(root, "Warning", warningColor, c -> { warningColor = c; updatePreview(); });
        TextView errorLabel = addColorPicker(root, "Error", errorColor, c -> { errorColor = c; updatePreview(); });
        TextView textPrimaryLabel = addColorPicker(root, "Text Primary", textPrimaryColor, c -> { textPrimaryColor = c; updatePreview(); });
        TextView textSecondaryLabel = addColorPicker(root, "Text Secondary", textSecondaryColor, c -> { textSecondaryColor = c; updatePreview(); });

        // ===== SIZE SLIDERS =====
        addSectionHeader(root, "SIZES");
        addSlider(root, "Button Height", buttonHeight, 32, 72, v -> buttonHeight = v);
        addSlider(root, "Button Radius", buttonRadius, 0, 30, v -> buttonRadius = v);
        addSlider(root, "Icon Size", iconSize, 16, 48, v -> iconSize = v);
        addSlider(root, "Header Text", headerTextSize, 12, 28, v -> headerTextSize = v);
        addSlider(root, "Body Text", bodyTextSize, 10, 20, v -> bodyTextSize = v);
        addSlider(root, "Input Height", inputHeight, 36, 64, v -> inputHeight = v);
        addSlider(root, "Card Padding", cardPadding, 8, 32, v -> cardPadding = v);

        // ===== PREVIEW =====
        addSectionHeader(root, "PREVIEW");
        previewContainer = new LinearLayout(getContext());
        previewContainer.setOrientation(LinearLayout.VERTICAL);
        previewContainer.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        previewContainer.setLayoutParams(previewParams);
        root.addView(previewContainer);
        updatePreview();

        // ===== BUTTONS =====
        Button saveBtn = new Button(getContext());
        saveBtn.setText("SAVE THEME");
        saveBtn.setTextColor(Color.WHITE);
        saveBtn.setBackgroundColor(Color.parseColor("#10b981"));
        saveBtn.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(50));
        saveParams.setMargins(0, dp(16), 0, dp(8));
        saveBtn.setLayoutParams(saveParams);
        saveBtn.setOnClickListener(v -> {
            appName = appNameInput.getText().toString().trim();
            welcomeMessage = welcomeInput.getText().toString().trim();
            saveTheme();
        });
        root.addView(saveBtn);

        Button resetBtn = new Button(getContext());
        resetBtn.setText("RESET TO DEFAULT");
        resetBtn.setTextColor(Color.WHITE);
        resetBtn.setBackgroundColor(Color.parseColor("#ef4444"));
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
        resetBtn.setLayoutParams(resetParams);
        resetBtn.setOnClickListener(v -> {
            repo.resetTheme(new FirestoreRepository.OnResultCallback() {
                @Override public void onSuccess() {
                    if (getActivity() != null) getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Theme reset to default", Toast.LENGTH_SHORT).show());
                }
                @Override public void onFailure(String e) {}
            });
        });
        root.addView(resetBtn);

        ScrollView scroll = new ScrollView(getContext());
        scroll.addView(root);

        // Load current theme from Firestore
        repo.listenTheme(data -> {
            if (getActivity() != null && !data.isEmpty()) {
                getActivity().runOnUiThread(() -> loadThemeFromFirestore(data));
            }
        });

        return scroll;
    }

    private void applyPreset(int index) {
        int[] colors = COLOR_PRESET_VALUES[index];
        primaryColor = colors[0];
        secondaryColor = colors[1];
        bgColor = colors[2];
        cardColor = colors[3];
        successColor = colors[4];
        warningColor = colors[5];
        errorColor = colors[6];
        textPrimaryColor = colors[7];
        textSecondaryColor = colors[8];
        Toast.makeText(getContext(), COLOR_PRESET_NAMES[index] + " preset applied", Toast.LENGTH_SHORT).show();
        updatePreview();
    }

    private void updatePreview() {
        if (previewContainer == null || getContext() == null) return;
        previewContainer.removeAllViews();
        previewContainer.setBackgroundColor(bgColor);

        // Header preview
        TextView hdr = new TextView(getContext());
        hdr.setText(appName);
        hdr.setTextColor(primaryColor);
        hdr.setTextSize(headerTextSize);
        hdr.setTypeface(null, Typeface.BOLD);
        previewContainer.addView(hdr);

        // Body text preview
        TextView body = new TextView(getContext());
        body.setText(welcomeMessage);
        body.setTextColor(textPrimaryColor);
        body.setTextSize(bodyTextSize);
        body.setPadding(0, dp(4), 0, dp(8));
        previewContainer.addView(body);

        // Card preview
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(cardColor);
        card.setPadding(dp(cardPadding), dp(cardPadding), dp(cardPadding), dp(cardPadding));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(cardParams);

        TextView cardTitle = new TextView(getContext());
        cardTitle.setText("Sample Card");
        cardTitle.setTextColor(textPrimaryColor);
        cardTitle.setTextSize(bodyTextSize);
        cardTitle.setTypeface(null, Typeface.BOLD);
        card.addView(cardTitle);

        TextView cardBody = new TextView(getContext());
        cardBody.setText("This is how cards will look with your theme.");
        cardBody.setTextColor(textSecondaryColor);
        cardBody.setTextSize(bodyTextSize - 2);
        card.addView(cardBody);

        previewContainer.addView(card);

        // Buttons preview
        LinearLayout btnRow = new LinearLayout(getContext());
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button primaryBtn = new Button(getContext());
        primaryBtn.setText("Primary");
        primaryBtn.setTextSize(11);
        primaryBtn.setTextColor(Color.WHITE);
        primaryBtn.setBackgroundColor(primaryColor);
        LinearLayout.LayoutParams btnP = new LinearLayout.LayoutParams(0, dp(buttonHeight), 1f);
        btnP.setMargins(0, 0, dp(4), 0);
        primaryBtn.setLayoutParams(btnP);
        btnRow.addView(primaryBtn);

        Button successBtn = new Button(getContext());
        successBtn.setText("Success");
        successBtn.setTextSize(11);
        successBtn.setTextColor(Color.WHITE);
        successBtn.setBackgroundColor(successColor);
        successBtn.setLayoutParams(new LinearLayout.LayoutParams(0, dp(buttonHeight), 1f));
        btnRow.addView(successBtn);

        Button warnBtn = new Button(getContext());
        warnBtn.setText("Warning");
        warnBtn.setTextSize(11);
        warnBtn.setTextColor(Color.WHITE);
        warnBtn.setBackgroundColor(warningColor);
        warnBtn.setLayoutParams(new LinearLayout.LayoutParams(0, dp(buttonHeight), 1f));
        btnRow.addView(warnBtn);

        previewContainer.addView(btnRow);
    }

    private void saveTheme() {
        Map<String, Object> theme = new HashMap<>();
        theme.put("primaryColor", String.format("#%06X", (0xFFFFFF & primaryColor)));
        theme.put("secondaryColor", String.format("#%06X", (0xFFFFFF & secondaryColor)));
        theme.put("bgColor", String.format("#%06X", (0xFFFFFF & bgColor)));
        theme.put("cardColor", String.format("#%06X", (0xFFFFFF & cardColor)));
        theme.put("successColor", String.format("#%06X", (0xFFFFFF & successColor)));
        theme.put("warningColor", String.format("#%06X", (0xFFFFFF & warningColor)));
        theme.put("errorColor", String.format("#%06X", (0xFFFFFF & errorColor)));
        theme.put("textPrimaryColor", String.format("#%06X", (0xFFFFFF & textPrimaryColor)));
        theme.put("textSecondaryColor", String.format("#%06X", (0xFFFFFF & textSecondaryColor)));
        theme.put("buttonHeight", buttonHeight);
        theme.put("buttonRadius", buttonRadius);
        theme.put("iconSize", iconSize);
        theme.put("headerTextSize", headerTextSize);
        theme.put("bodyTextSize", bodyTextSize);
        theme.put("inputHeight", inputHeight);
        theme.put("cardPadding", cardPadding);
        theme.put("appName", appName);
        theme.put("welcomeMessage", welcomeMessage);

        repo.saveTheme(theme, new FirestoreRepository.OnResultCallback() {
            @Override public void onSuccess() {
                if (getActivity() != null) getActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), "Theme saved! Main app will update automatically.", Toast.LENGTH_SHORT).show());
            }
            @Override public void onFailure(String error) {
                if (getActivity() != null) getActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadThemeFromFirestore(Map<String, Object> data) {
        try {
            if (data.containsKey("primaryColor")) primaryColor = Color.parseColor(data.get("primaryColor").toString());
            if (data.containsKey("secondaryColor")) secondaryColor = Color.parseColor(data.get("secondaryColor").toString());
            if (data.containsKey("bgColor")) bgColor = Color.parseColor(data.get("bgColor").toString());
            if (data.containsKey("cardColor")) cardColor = Color.parseColor(data.get("cardColor").toString());
            if (data.containsKey("successColor")) successColor = Color.parseColor(data.get("successColor").toString());
            if (data.containsKey("warningColor")) warningColor = Color.parseColor(data.get("warningColor").toString());
            if (data.containsKey("errorColor")) errorColor = Color.parseColor(data.get("errorColor").toString());
            if (data.containsKey("textPrimaryColor")) textPrimaryColor = Color.parseColor(data.get("textPrimaryColor").toString());
            if (data.containsKey("textSecondaryColor")) textSecondaryColor = Color.parseColor(data.get("textSecondaryColor").toString());
            if (data.containsKey("buttonHeight")) buttonHeight = ((Number) data.get("buttonHeight")).intValue();
            if (data.containsKey("buttonRadius")) buttonRadius = ((Number) data.get("buttonRadius")).intValue();
            if (data.containsKey("iconSize")) iconSize = ((Number) data.get("iconSize")).intValue();
            if (data.containsKey("headerTextSize")) headerTextSize = ((Number) data.get("headerTextSize")).intValue();
            if (data.containsKey("bodyTextSize")) bodyTextSize = ((Number) data.get("bodyTextSize")).intValue();
            if (data.containsKey("inputHeight")) inputHeight = ((Number) data.get("inputHeight")).intValue();
            if (data.containsKey("cardPadding")) cardPadding = ((Number) data.get("cardPadding")).intValue();
            if (data.containsKey("appName")) appName = data.get("appName").toString();
            if (data.containsKey("welcomeMessage")) welcomeMessage = data.get("welcomeMessage").toString();
            updatePreview();
        } catch (Exception e) {
            // Ignore parse errors, keep defaults
        }
    }

    // ===== UI HELPERS =====

    private void addSectionHeader(LinearLayout parent, String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#38bdf8"));
        tv.setTextSize(13);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, dp(16), 0, dp(8));
        parent.addView(tv);
    }

    private EditText addTextInput(LinearLayout parent, String hint, String value) {
        EditText et = new EditText(getContext());
        et.setHint(hint);
        et.setText(value);
        et.setTextColor(Color.WHITE);
        et.setTextSize(14);
        et.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        params.setMargins(0, dp(4), 0, dp(4));
        et.setLayoutParams(params);
        parent.addView(et);
        return et;
    }

    private TextView addColorPicker(LinearLayout parent, String label, int color, ColorListener listener) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));

        // Color circle
        TextView circle = new TextView(getContext());
        circle.setText("  ");
        circle.setBackgroundColor(color);
        LinearLayout.LayoutParams circleParams = new LinearLayout.LayoutParams(dp(32), dp(32));
        circleParams.setMargins(0, 0, dp(12), 0);
        circle.setLayoutParams(circleParams);
        row.addView(circle);

        // Label
        TextView labelTv = new TextView(getContext());
        labelTv.setText(label);
        labelTv.setTextColor(Color.WHITE);
        labelTv.setTextSize(13);
        labelTv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(labelTv);

        // Current value
        TextView valueTv = new TextView(getContext());
        valueTv.setText(String.format("#%06X", (0xFFFFFF & color)));
        valueTv.setTextColor(Color.parseColor("#94a3b8"));
        valueTv.setTextSize(11);
        row.addView(valueTv);

        // Cycle through 10 preset colors on tap
        int[] presetColors = {
            Color.parseColor("#38bdf8"), Color.parseColor("#a855f7"), Color.parseColor("#10b981"),
            Color.parseColor("#f59e0b"), Color.parseColor("#ef4444"), Color.parseColor("#ec4899"),
            Color.parseColor("#06b6d4"), Color.parseColor("#84cc16"), Color.parseColor("#f97316"),
            Color.parseColor("#6366f1")
        };
        final int[] currentIdx = {0};
        for (int i = 0; i < presetColors.length; i++) {
            if (presetColors[i] == color) { currentIdx[0] = i; break; }
        }

        row.setOnClickListener(v -> {
            currentIdx[0] = (currentIdx[0] + 1) % presetColors.length;
            int newColor = presetColors[currentIdx[0]];
            circle.setBackgroundColor(newColor);
            valueTv.setText(String.format("#%06X", (0xFFFFFF & newColor)));
            listener.onColorChanged(newColor);
        });

        parent.addView(row);
        return valueTv;
    }

    private void addSlider(LinearLayout parent, String label, int value, int min, int max, SliderListener listener) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView labelTv = new TextView(getContext());
        labelTv.setText(label);
        labelTv.setTextColor(Color.WHITE);
        labelTv.setTextSize(12);
        labelTv.setLayoutParams(new LinearLayout.LayoutParams(dp(100), LinearLayout.LayoutParams.WRAP_CONTENT));
        row.addView(labelTv);

        TextView valueTv = new TextView(getContext());
        valueTv.setText(String.valueOf(value));
        valueTv.setTextColor(Color.parseColor("#38bdf8"));
        valueTv.setTextSize(12);
        valueTv.setGravity(Gravity.CENTER);
        valueTv.setLayoutParams(new LinearLayout.LayoutParams(dp(40), LinearLayout.LayoutParams.WRAP_CONTENT));
        row.addView(valueTv);

        SeekBar seekBar = new SeekBar(getContext());
        seekBar.setMax(max - min);
        seekBar.setProgress(value - min);
        LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        seekParams.setMargins(dp(8), 0, 0, 0);
        seekBar.setLayoutParams(seekParams);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                int val = progress + min;
                valueTv.setText(String.valueOf(val));
                listener.onValueChanged(val);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) { updatePreview(); }
        });
        row.addView(seekBar);

        parent.addView(row);
    }

    interface ColorListener {
        void onColorChanged(int color);
    }

    interface SliderListener {
        void onValueChanged(int value);
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
