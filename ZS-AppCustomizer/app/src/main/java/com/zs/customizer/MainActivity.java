package com.zs.customizer;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "ZSCustomizer";
    private EditText inputAppName, inputWelcomeMsg;
    private SeekBar seekBtnHeight, seekBtnRadius, seekIconSize, seekHeaderTextSize;
    private SeekBar seekBodyTextSize, seekInputHeight, seekPadding;
    private TextView btnHeightVal, btnRadiusVal, iconSizeVal, headerTextSizeVal;
    private TextView bodyTextSizeVal, inputHeightVal, paddingValue;
    private LinearLayout presetContainer, colorGrid;

    // Current theme values
    private String primaryColor = "#38bdf8";
    private String secondaryColor = "#a855f7";
    private String bgColor = "#090d16";
    private String cardColor = "#0f1729";
    private String inputColor = "#0f1729";
    private String navBarColor = "#0a0f1c";
    private String textColor = "#f1f5f9";
    private String secondaryTextColor = "#94a3b8";
    private String dangerColor = "#ef4444";

    private String[] presetColors = {
        "#38bdf8", "#a855f7", "#10b981", "#f59e0b", "#ef4444",
        "#ec4899", "#6366f1", "#14b8a6", "#f97316", "#06b6d4"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupSeekBars();
        setupColorPickers();
        setupPresets();
        loadFromFirestore();
    }

    private void initViews() {
        inputAppName = findViewById(R.id.input_app_name);
        inputWelcomeMsg = findViewById(R.id.input_welcome_msg);
        seekBtnHeight = findViewById(R.id.seek_btn_height);
        seekBtnRadius = findViewById(R.id.seek_btn_radius);
        seekIconSize = findViewById(R.id.seek_icon_size);
        seekHeaderTextSize = findViewById(R.id.seek_header_text_size);
        seekBodyTextSize = findViewById(R.id.seek_body_text_size);
        seekInputHeight = findViewById(R.id.seek_input_height);
        seekPadding = findViewById(R.id.seek_padding);
        btnHeightVal = findViewById(R.id.btn_height_val);
        btnRadiusVal = findViewById(R.id.btn_radius_val);
        iconSizeVal = findViewById(R.id.icon_size_val);
        headerTextSizeVal = findViewById(R.id.header_text_size_val);
        bodyTextSizeVal = findViewById(R.id.body_text_size_val);
        inputHeightVal = findViewById(R.id.input_height_val);
        paddingValue = findViewById(R.id.padding_value);
        presetContainer = findViewById(R.id.preset_container);
        colorGrid = findViewById(R.id.color_grid);

        findViewById(R.id.save_btn).setOnClickListener(v -> saveToFirestore());
    }

    private void setupSeekBars() {
        setupSeekBar(seekBtnHeight, btnHeightVal, "dp");
        setupSeekBar(seekBtnRadius, btnRadiusVal, "dp");
        setupSeekBar(seekIconSize, iconSizeVal, "dp");
        setupSeekBar(seekHeaderTextSize, headerTextSizeVal, "sp");
        setupSeekBar(seekBodyTextSize, bodyTextSizeVal, "sp");
        setupSeekBar(seekInputHeight, inputHeightVal, "dp");
        setupSeekBar(seekPadding, paddingValue, "dp");
    }

    private void setupSeekBar(SeekBar seek, TextView val, String suffix) {
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                val.setText(p + suffix);
            }
            @Override
            public void onStartTrackingTouch(SeekBar sb) {}
            @Override
            public void onStopTrackingTouch(SeekBar sb) {}
        });
    }

    private void setupColorPickers() {
        String[] colorNames = {"Primary", "Secondary", "Background", "Card", "Input", "NavBar", "Text", "Secondary Text", "Danger"};
        String[] colorValues = {primaryColor, secondaryColor, bgColor, cardColor, inputColor, navBarColor, textColor, secondaryTextColor, dangerColor};

        for (int i = 0; i < colorNames.length; i++) {
            final int idx = i;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, 8, 0, 8);

            TextView label = new TextView(this);
            label.setText(colorNames[i]);
            label.setTextColor(Color.parseColor("#94a3b8"));
            label.setTextSize(13);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            label.setLayoutParams(labelParams);
            row.addView(label);

            // Current color circle
            TextView colorCircle = new TextView(this);
            colorCircle.setWidth(dp(32));
            colorCircle.setHeight(dp(32));
            android.graphics.drawable.GradientDrawable circle = new android.graphics.drawable.GradientDrawable();
            circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            circle.setColor(Color.parseColor(colorValues[i]));
            circle.setStroke(2, Color.parseColor("#1e3a5f"));
            colorCircle.setBackground(circle);
            row.addView(colorCircle);

            // Tap to cycle colors
            colorCircle.setOnClickListener(v -> {
                String current = getColorByIndex(idx);
                int pos = 0;
                for (int j = 0; j < presetColors.length; j++) {
                    if (presetColors[j].equals(current)) { pos = j; break; }
                }
                String next = presetColors[(pos + 1) % presetColors.length];
                setColorByIndex(idx, next);
                circle.setColor(Color.parseColor(next));
            });

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            colorGrid.addView(row, rowParams);
        }
    }

    private void setupPresets() {
        String[] presetNames = {"Default", "Neon", "Fire", "Forest", "Purple"};
        String[][] presetValues = {
            {"#38bdf8", "#a855f7", "#090d16", "#0f1729", "#0f1729", "#0a0f1c", "#f1f5f9", "#94a3b8", "#ef4444"},
            {"#00ff88", "#ff00ff", "#000011", "#001122", "#001122", "#000033", "#ffffff", "#88ff88", "#ff0044"},
            {"#ff6600", "#ff0044", "#1a0000", "#330000", "#330000", "#220000", "#ffe0cc", "#ff9966", "#ff0000"},
            {"#00cc66", "#339966", "#0a1a0a", "#0f2f0f", "#0f2f0f", "#0a150a", "#e0ffe0", "#88cc88", "#cc3333"},
            {"#cc66ff", "#9933cc", "#0d001a", "#1a0033", "#1a0033", "#110022", "#f0e0ff", "#cc99ff", "#ff3366"}
        };

        for (int i = 0; i < presetNames.length; i++) {
            final int idx = i;
            TextView btn = new TextView(this);
            btn.setText(presetNames[i]);
            btn.setTextColor(Color.WHITE);
            btn.setTextSize(12);
            btn.setPadding(dp(16), dp(8), dp(16), dp(8));
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setCornerRadius(dp(8));
            bg.setColor(Color.parseColor(presetValues[i][0]));
            btn.setBackground(bg);
            btn.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(dp(8));
            btn.setLayoutParams(params);

            btn.setOnClickListener(v -> {
                primaryColor = presetValues[idx][0];
                secondaryColor = presetValues[idx][1];
                bgColor = presetValues[idx][2];
                cardColor = presetValues[idx][3];
                inputColor = presetValues[idx][4];
                navBarColor = presetValues[idx][5];
                textColor = presetValues[idx][6];
                secondaryTextColor = presetValues[idx][7];
                dangerColor = presetValues[idx][8];
                refreshColorGrid();
                Toast.makeText(this, presetNames[idx] + " preset applied", Toast.LENGTH_SHORT).show();
            });

            presetContainer.addView(btn);
        }
    }

    private void refreshColorGrid() {
        colorGrid.removeAllViews();
        setupColorPickers();
    }

    private String getColorByIndex(int idx) {
        switch (idx) {
            case 0: return primaryColor;
            case 1: return secondaryColor;
            case 2: return bgColor;
            case 3: return cardColor;
            case 4: return inputColor;
            case 5: return navBarColor;
            case 6: return textColor;
            case 7: return secondaryTextColor;
            case 8: return dangerColor;
            default: return "#ffffff";
        }
    }

    private void setColorByIndex(int idx, String color) {
        switch (idx) {
            case 0: primaryColor = color; break;
            case 1: secondaryColor = color; break;
            case 2: bgColor = color; break;
            case 3: cardColor = color; break;
            case 4: inputColor = color; break;
            case 5: navBarColor = color; break;
            case 6: textColor = color; break;
            case 7: secondaryTextColor = color; break;
            case 8: dangerColor = color; break;
        }
    }

    private void loadFromFirestore() {
        try {
            FirebaseFirestore.getInstance()
                .collection("appSettings").document("theme")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String json = new Gson().toJson(doc.getData());
                        Map data = doc.getData();
                        if (data != null) {
                            if (data.containsKey("primaryColor")) primaryColor = (String) data.get("primaryColor");
                            if (data.containsKey("secondaryColor")) secondaryColor = (String) data.get("secondaryColor");
                            if (data.containsKey("backgroundColor")) bgColor = (String) data.get("backgroundColor");
                            if (data.containsKey("cardColor")) cardColor = (String) data.get("cardColor");
                            if (data.containsKey("inputColor")) inputColor = (String) data.get("inputColor");
                            if (data.containsKey("navBarColor")) navBarColor = (String) data.get("navBarColor");
                            if (data.containsKey("textColor")) textColor = (String) data.get("textColor");
                            if (data.containsKey("secondaryTextColor")) secondaryTextColor = (String) data.get("secondaryTextColor");
                            if (data.containsKey("dangerColor")) dangerColor = (String) data.get("dangerColor");
                            if (data.containsKey("appName")) inputAppName.setText((String) data.get("appName"));
                            if (data.containsKey("welcomeMessage")) inputWelcomeMsg.setText((String) data.get("welcomeMessage"));
                            if (data.containsKey("buttonHeight")) seekBtnHeight.setProgress(toInt(data.get("buttonHeight"), 48));
                            if (data.containsKey("buttonCornerRadius")) seekBtnRadius.setProgress(toInt(data.get("buttonCornerRadius"), 12));
                            if (data.containsKey("iconSize")) seekIconSize.setProgress(toInt(data.get("iconSize"), 24));
                            if (data.containsKey("headerTextSize")) seekHeaderTextSize.setProgress(toInt(data.get("headerTextSize"), 18));
                            if (data.containsKey("bodyTextSize")) seekBodyTextSize.setProgress(toInt(data.get("bodyTextSize"), 14));
                            if (data.containsKey("inputHeight")) seekInputHeight.setProgress(toInt(data.get("inputHeight"), 48));
                            if (data.containsKey("contentPadding")) seekPadding.setProgress(toInt(data.get("contentPadding"), 16));
                            refreshColorGrid();
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Load failed", e));
        } catch (Exception e) {
            Log.e(TAG, "Firestore not available", e);
            Toast.makeText(this, "Firebase not configured", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveToFirestore() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("primaryColor", primaryColor);
            data.put("secondaryColor", secondaryColor);
            data.put("backgroundColor", bgColor);
            data.put("cardColor", cardColor);
            data.put("inputColor", inputColor);
            data.put("navBarColor", navBarColor);
            data.put("textColor", textColor);
            data.put("secondaryTextColor", secondaryTextColor);
            data.put("dangerColor", dangerColor);
            data.put("appName", inputAppName.getText().toString().trim());
            data.put("welcomeMessage", inputWelcomeMsg.getText().toString().trim());
            data.put("buttonHeight", seekBtnHeight.getProgress());
            data.put("buttonCornerRadius", seekBtnRadius.getProgress());
            data.put("iconSize", seekIconSize.getProgress());
            data.put("headerTextSize", seekHeaderTextSize.getProgress());
            data.put("bodyTextSize", seekBodyTextSize.getProgress());
            data.put("inputHeight", seekInputHeight.getProgress());
            data.put("contentPadding", seekPadding.getProgress());
            data.put("timestamp", System.currentTimeMillis());

            FirebaseFirestore.getInstance()
                .collection("appSettings").document("theme")
                .set(data)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Theme saved!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private int toInt(Object val, int def) {
        if (val instanceof Long) return ((Long) val).intValue();
        if (val instanceof Number) return ((Number) val).intValue();
        return def;
    }

    private int dp(int val) {
        return (int) (val * getResources().getDisplayMetrics().density);
    }
}
