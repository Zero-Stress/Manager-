package com.zerostress.manager;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.zerostress.manager.models.AppCustomizer;

public class AdminCustomizerActivity extends AppCompatActivity {

    private ThemeManager themeManager;
    private AppCustomizer current;

    private EditText inputAppName, inputWelcomeMsg;
    private Spinner spinnerFont;
    private SeekBar seekRadius, seekPadding;
    private TextView radiusValue, paddingValue;
    private SwitchCompat switchRounded, switchGradient;
    private SwitchCompat switchOnline, switchAchievements, switchRewards, switchAnalytics;
    private SwitchCompat switchSquads, switchTournaments, switchSchedule;
    private SwitchCompat switchChat, switchVoiceChat, switchAttendance;
    private LinearLayout previewContainer, presetContainer;
    private GridLayout colorGrid;

    private SeekBar seekBtnHeight, seekBtnRadius, seekBtnTextSize, seekIconSize;
    private SeekBar seekHeaderTextSize, seekBodyTextSize, seekInputHeight, seekNavBarHeight;
    private TextView btnHeightVal, btnRadiusVal, btnTextSizeVal, iconSizeVal;
    private TextView headerTextSizeVal, bodyTextSizeVal, inputHeightVal, navBarHeightVal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_customizer);

        try {
            themeManager = ThemeManager.getInstance(this);
            current = themeManager.getTheme();

            initViews();
            loadCurrentTheme();
            setupPresetThemes();
            setupSizeControls();
            setupListeners();
            tintViews();
            updatePreview();
        } catch (Exception e) {
            Toast.makeText(this, "Customizer error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /** Apply tints programmatically (safe — won't crash if unsupported) */
    private void tintViews() {
        int blue = 0xFF38bdf8;
        int darkInput = 0xFF1e3a5f;

        // Tint SeekBars
        SeekBar[] seeks = {seekBtnHeight, seekBtnRadius, seekBtnTextSize, seekIconSize,
                seekHeaderTextSize, seekBodyTextSize, seekInputHeight, seekNavBarHeight,
                seekRadius, seekPadding};
        for (SeekBar s : seeks) {
            if (s == null) continue;
            try {
                s.getThumb().setTint(blue);
                s.getProgressDrawable().setTint(blue);
            } catch (Exception ignored) {}
        }

        // Tint Save button
        try {
            Button saveBtn = findViewById(R.id.save_btn);
            saveBtn.setBackgroundColor(0xFF10b981);
        } catch (Exception ignored) {}

        // Tint SwitchCompat thumbs
        SwitchCompat[] switches = {switchRounded, switchGradient, switchOnline,
                switchAchievements, switchRewards, switchAnalytics, switchSquads,
                switchTournaments, switchSchedule, switchChat, switchVoiceChat, switchAttendance};
        for (SwitchCompat sw : switches) {
            if (sw == null) continue;
            try {
                sw.getThumbTintList();
            } catch (Exception ignored) {}
        }
    }

    private void initViews() {
        inputAppName = findViewById(R.id.input_app_name);
        inputWelcomeMsg = findViewById(R.id.input_welcome_msg);
        spinnerFont = findViewById(R.id.spinner_font);
        seekRadius = findViewById(R.id.seek_radius);
        seekPadding = findViewById(R.id.seek_padding);
        radiusValue = findViewById(R.id.radius_value);
        paddingValue = findViewById(R.id.padding_value);
        switchRounded = findViewById(R.id.switch_rounded);
        switchGradient = findViewById(R.id.switch_gradient);
        switchOnline = findViewById(R.id.switch_online_status);
        switchAchievements = findViewById(R.id.switch_achievements);
        switchRewards = findViewById(R.id.switch_rewards);
        switchAnalytics = findViewById(R.id.switch_analytics);
        switchSquads = findViewById(R.id.switch_squads);
        switchTournaments = findViewById(R.id.switch_tournaments);
        switchSchedule = findViewById(R.id.switch_schedule);
        switchChat = findViewById(R.id.switch_chat);
        switchVoiceChat = findViewById(R.id.switch_voice_chat);
        switchAttendance = findViewById(R.id.switch_attendance);
        previewContainer = findViewById(R.id.preview_container);
        presetContainer = findViewById(R.id.preset_container);
        colorGrid = findViewById(R.id.color_grid);

        seekBtnHeight = findViewById(R.id.seek_btn_height);
        seekBtnRadius = findViewById(R.id.seek_btn_radius);
        seekBtnTextSize = findViewById(R.id.seek_btn_text_size);
        seekIconSize = findViewById(R.id.seek_icon_size);
        seekHeaderTextSize = findViewById(R.id.seek_header_text_size);
        seekBodyTextSize = findViewById(R.id.seek_body_text_size);
        seekInputHeight = findViewById(R.id.seek_input_height);
        seekNavBarHeight = findViewById(R.id.seek_navbar_height);
        btnHeightVal = findViewById(R.id.btn_height_val);
        btnRadiusVal = findViewById(R.id.btn_radius_val);
        btnTextSizeVal = findViewById(R.id.btn_text_size_val);
        iconSizeVal = findViewById(R.id.icon_size_val);
        headerTextSizeVal = findViewById(R.id.header_text_size_val);
        bodyTextSizeVal = findViewById(R.id.body_text_size_val);
        inputHeightVal = findViewById(R.id.input_height_val);
        navBarHeightVal = findViewById(R.id.navbar_height_val);

        findViewById(R.id.back_btn).setOnClickListener(v -> finish());
        findViewById(R.id.save_btn).setOnClickListener(v -> saveTheme());

        String[] fonts = {"Sans Serif", "Serif", "Monospace"};
        ArrayAdapter<String> fontAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, fonts);
        fontAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFont.setAdapter(fontAdapter);
    }

    private void loadCurrentTheme() {
        inputAppName.setText(nvl(current.getAppName(), "Zero Stress"));
        inputWelcomeMsg.setText(nvl(current.getWelcomeMessage(), "Welcome!"));

        String[] fonts = {"sans-serif", "serif", "monospace"};
        for (int i = 0; i < fonts.length; i++) {
            if (fonts[i].equals(current.getFontFamily())) {
                spinnerFont.setSelection(i);
                break;
            }
        }

        seekRadius.setProgress(safeProgress(current.getCardCornerRadius(), 32, 16));
        radiusValue.setText(current.getCardCornerRadius() + "dp");
        seekPadding.setProgress(safeProgress(current.getCardPadding(), 32, 16));
        paddingValue.setText(current.getCardPadding() + "dp");

        switchRounded.setChecked(current.isRoundedCards());
        switchGradient.setChecked(current.isGradientHeader());
        switchOnline.setChecked(current.isShowOnlineStatus());
        switchAchievements.setChecked(current.isShowAchievements());
        switchRewards.setChecked(current.isShowRewardPoints());
        switchAnalytics.setChecked(current.isShowAnalytics());
        switchSquads.setChecked(current.isShowSquads());
        switchTournaments.setChecked(current.isShowTournaments());
        switchSchedule.setChecked(current.isShowSchedule());
        switchChat.setChecked(current.isShowChat());
        switchVoiceChat.setChecked(current.isShowVoiceChat());
        switchAttendance.setChecked(current.isShowAttendance());

        setupColorButtons();
    }

    private void setupSizeControls() {
        // Button Height (20-80)
        int bh = clamp(current.getButtonHeight(), 20, 80);
        seekBtnHeight.setProgress(bh - 20);
        btnHeightVal.setText(bh + "dp");
        seekBtnHeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean f) {
                current.setButtonHeight(p + 20);
                btnHeightVal.setText((p + 20) + "dp");
                updatePreview();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        // Button Corner Radius (0-30)
        int br = clamp(current.getButtonCornerRadius(), 0, 30);
        seekBtnRadius.setProgress(br);
        btnRadiusVal.setText(br + "dp");
        seekBtnRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean f) {
                current.setButtonCornerRadius(p);
                btnRadiusVal.setText(p + "dp");
                updatePreview();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        // Button Text Size (10-20)
        int bts = clamp((int) current.getButtonTextSize(), 10, 20);
        seekBtnTextSize.setProgress(bts - 10);
        btnTextSizeVal.setText(bts + "sp");
        seekBtnTextSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean f) {
                current.setButtonTextSize(p + 10);
                btnTextSizeVal.setText((p + 10) + "sp");
                updatePreview();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        // Icon Size (12-40)
        int is = clamp(current.getIconSize(), 12, 40);
        seekIconSize.setProgress(is - 12);
        iconSizeVal.setText(is + "dp");
        seekIconSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean f) {
                current.setIconSize(p + 12);
                iconSizeVal.setText((p + 12) + "dp");
                updatePreview();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        // Header Text Size (14-28)
        int hts = clamp((int) current.getHeaderTextSize(), 14, 28);
        seekHeaderTextSize.setProgress(hts - 14);
        headerTextSizeVal.setText(hts + "sp");
        seekHeaderTextSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean f) {
                current.setHeaderTextSize(p + 14);
                headerTextSizeVal.setText((p + 14) + "sp");
                updatePreview();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        // Body Text Size (10-20)
        int bts2 = clamp((int) current.getBodyTextSize(), 10, 20);
        seekBodyTextSize.setProgress(bts2 - 10);
        bodyTextSizeVal.setText(bts2 + "sp");
        seekBodyTextSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean f) {
                current.setBodyTextSize(p + 10);
                bodyTextSizeVal.setText((p + 10) + "sp");
                updatePreview();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        // Input Height (32-72)
        int ih = clamp(current.getInputHeight(), 32, 72);
        seekInputHeight.setProgress(ih - 32);
        inputHeightVal.setText(ih + "dp");
        seekInputHeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean f) {
                current.setInputHeight(p + 32);
                inputHeightVal.setText((p + 32) + "dp");
                updatePreview();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        // Nav Bar Height (40-80)
        int nh = clamp(current.getNavBarHeight(), 40, 80);
        seekNavBarHeight.setProgress(nh - 40);
        navBarHeightVal.setText(nh + "dp");
        seekNavBarHeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean f) {
                current.setNavBarHeight(p + 40);
                navBarHeightVal.setText((p + 40) + "dp");
                updatePreview();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
    }

    private void setupPresetThemes() {
        String[][] presets = {
            {"ocean", "\uD83C\uDF0A Ocean", "#38bdf8"},
            {"neon", "\u2728 Neon", "#a855f7"},
            {"fire", "\uD83D\uDD25 Fire", "#ef4444"},
            {"forest", "\uD83C\uDF32 Forest", "#22c55e"},
            {"gold", "\uD83C\uDFC6 Gold", "#eab308"},
            {"midnight", "\uD83C\uDF19 Midnight", "#6366f1"}
        };

        presetContainer.removeAllViews();

        for (String[] preset : presets) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(8, 8, 8, 8);

            TextView circle = new TextView(this);
            circle.setText("\u25CF");
            circle.setTextSize(28);
            circle.setTextColor(safeColor(preset[2]));
            circle.setGravity(Gravity.CENTER);
            item.addView(circle);

            TextView label = new TextView(this);
            label.setText(preset[1]);
            label.setTextColor(0xFF94a3b8);
            label.setTextSize(10);
            label.setGravity(Gravity.CENTER);
            item.addView(label);

            item.setOnClickListener(v -> applyPreset(preset[0]));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            item.setLayoutParams(params);
            presetContainer.addView(item);
        }
    }

    private void applyPreset(String name) {
        current = AppCustomizer.getPreset(name);
        loadCurrentTheme();
        setupSizeControls();
        updatePreview();
        Toast.makeText(this, "Applied " + name + " theme", Toast.LENGTH_SHORT).show();
    }

    private void setupColorButtons() {
        colorGrid.removeAllViews();

        String[][] colors = {
            {"Primary", current.getPrimaryColor()},
            {"Secondary", current.getSecondaryColor()},
            {"Background", current.getBackgroundColor()},
            {"Card", current.getCardColor()},
            {"Input", current.getInputColor()},
            {"Nav Bar", current.getNavBarColor()},
            {"Text", current.getTextColor()},
            {"Muted Text", current.getSecondaryTextColor()},
            {"Danger", current.getDangerColor()},
            {"Warning", current.getWarningColor()},
            {"Border", current.getBorderColor()}
        };

        for (String[] color : colors) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(4, 4, 4, 4);

            TextView circle = new TextView(this);
            circle.setText("\u25CF");
            circle.setTextSize(32);
            circle.setTextColor(safeColor(color[1]));
            circle.setGravity(Gravity.CENTER);
            item.addView(circle);

            TextView label = new TextView(this);
            label.setText(color[0]);
            label.setTextColor(0xFF94a3b8);
            label.setTextSize(9);
            label.setGravity(Gravity.CENTER);
            item.addView(label);

            final String colorName = color[0];
            final int curColor = safeColor(color[1]);
            item.setOnClickListener(v -> showBuiltInColorPicker(colorName, curColor, newColor -> {
                applyColor(colorName, newColor);
                setupColorButtons();
                updatePreview();
            }));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.columnSpec = GridLayout.spec(0, 1f);
            item.setLayoutParams(params);
            colorGrid.addView(item);
        }
    }

    private void applyColor(String name, String hex) {
        switch (name) {
            case "Primary": current.setPrimaryColor(hex); break;
            case "Secondary": current.setSecondaryColor(hex); break;
            case "Background": current.setBackgroundColor(hex); break;
            case "Card": current.setCardColor(hex); break;
            case "Input": current.setInputColor(hex); break;
            case "Nav Bar": current.setNavBarColor(hex); break;
            case "Text": current.setTextColor(hex); break;
            case "Muted Text": current.setSecondaryTextColor(hex); break;
            case "Danger": current.setDangerColor(hex); break;
            case "Warning": current.setWarningColor(hex); break;
            case "Border": current.setBorderColor(hex); break;
        }
    }

    private void showBuiltInColorPicker(String title, int currentColor, ColorPickedListener listener) {
        final int[] selectedColor = {currentColor};
        final int[] rgb = {Color.red(currentColor), Color.green(currentColor), Color.blue(currentColor)};

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        TextView preview = new TextView(this);
        preview.setText("\u25CF");
        preview.setTextSize(48);
        preview.setTextColor(currentColor);
        preview.setGravity(Gravity.CENTER);
        layout.addView(preview);

        // R slider
        layout.addView(createSlider("Red", rgb[0], (progress, fromUser) -> {
            rgb[0] = progress;
            selectedColor[0] = Color.rgb(rgb[0], rgb[1], rgb[2]);
            preview.setTextColor(selectedColor[0]);
        }));

        // G slider
        layout.addView(createSlider("Green", rgb[1], (progress, fromUser) -> {
            rgb[1] = progress;
            selectedColor[0] = Color.rgb(rgb[0], rgb[1], rgb[2]);
            preview.setTextColor(selectedColor[0]);
        }));

        // B slider
        layout.addView(createSlider("Blue", rgb[2], (progress, fromUser) -> {
            rgb[2] = progress;
            selectedColor[0] = Color.rgb(rgb[0], rgb[1], rgb[2]);
            preview.setTextColor(selectedColor[0]);
        }));

        TextView hexTv = new TextView(this);
        hexTv.setText(String.format("#%06X", 0xFFFFFF & selectedColor[0]));
        hexTv.setTextColor(0xFF38bdf8);
        hexTv.setTextSize(14);
        hexTv.setPadding(0, 8, 0, 0);
        layout.addView(hexTv);

        // Quick presets
        LinearLayout presets = new LinearLayout(this);
        presets.setOrientation(LinearLayout.HORIZONTAL);
        presets.setGravity(Gravity.CENTER);
        int[] presetColors = {0xFF38bdf8, 0xFF10b981, 0xFFef4444, 0xFFa855f7, 0xFFeab308, 0xFFf59e0b, 0xFFf1f5f9, 0xFF94a3b8};
        for (int c : presetColors) {
            TextView dot = new TextView(this);
            dot.setText("\u25CF");
            dot.setTextSize(20);
            dot.setTextColor(c);
            dot.setPadding(8, 4, 8, 4);
            final int presetColor = c;
            dot.setOnClickListener(v -> {
                selectedColor[0] = presetColor;
                rgb[0] = Color.red(presetColor);
                rgb[1] = Color.green(presetColor);
                rgb[2] = Color.blue(presetColor);
                preview.setTextColor(selectedColor[0]);
                hexTv.setText(String.format("#%06X", 0xFFFFFF & selectedColor[0]));
            });
            presets.addView(dot);
        }
        layout.addView(presets);

        new AlertDialog.Builder(this)
            .setTitle("\uD83C\uDFA8 Pick Color: " + title)
            .setView(layout)
            .setPositiveButton("Select", (d, w) -> {
                listener.onColorPicked(String.format("#%06X", 0xFFFFFF & selectedColor[0]));
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private LinearLayout createSlider(String label, int value, SliderListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, 8, 0, 8);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView name = new TextView(this);
        name.setText(label);
        name.setTextColor(0xFF94a3b8);
        name.setTextSize(12);
        name.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        header.addView(name);

        TextView val = new TextView(this);
        val.setText(String.valueOf(value));
        val.setTextColor(0xFF38bdf8);
        val.setTextSize(12);
        header.addView(val);

        row.addView(header);

        SeekBar seek = new SeekBar(this);
        seek.setMax(255);
        seek.setProgress(value);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                listener.onChanged(p, fromUser);
                val.setText(String.valueOf(p));
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
        row.addView(seek);

        return row;
    }

    interface SliderListener {
        void onChanged(int progress, boolean fromUser);
    }

    interface ColorPickedListener {
        void onColorPicked(String hex);
    }

    private void setupListeners() {
        SeekBar.OnSeekBarChangeListener seekListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (seekBar == seekRadius) {
                    radiusValue.setText(progress + "dp");
                    current.setCardCornerRadius(progress);
                } else if (seekBar == seekPadding) {
                    paddingValue.setText(progress + "dp");
                    current.setCardPadding(progress);
                }
                updatePreview();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };
        seekRadius.setOnSeekBarChangeListener(seekListener);
        seekPadding.setOnSeekBarChangeListener(seekListener);

        switchRounded.setOnCheckedChangeListener((b, c) -> { current.setRoundedCards(c); updatePreview(); });
        switchGradient.setOnCheckedChangeListener((b, c) -> { current.setGradientHeader(c); updatePreview(); });
        switchOnline.setOnCheckedChangeListener((b, c) -> current.setShowOnlineStatus(c));
        switchAchievements.setOnCheckedChangeListener((b, c) -> current.setShowAchievements(c));
        switchRewards.setOnCheckedChangeListener((b, c) -> current.setShowRewardPoints(c));
        switchAnalytics.setOnCheckedChangeListener((b, c) -> current.setShowAnalytics(c));
        switchSquads.setOnCheckedChangeListener((b, c) -> current.setShowSquads(c));
        switchTournaments.setOnCheckedChangeListener((b, c) -> current.setShowTournaments(c));
        switchSchedule.setOnCheckedChangeListener((b, c) -> current.setShowSchedule(c));
        switchChat.setOnCheckedChangeListener((b, c) -> current.setShowChat(c));
        switchVoiceChat.setOnCheckedChangeListener((b, c) -> current.setShowVoiceChat(c));
        switchAttendance.setOnCheckedChangeListener((b, c) -> current.setShowAttendance(c));

        inputAppName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                current.setAppName(inputAppName.getText().toString().trim());
                updatePreview();
            }
        });
        inputWelcomeMsg.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) current.setWelcomeMessage(inputWelcomeMsg.getText().toString().trim());
        });
    }

    // ==================== LIVE PREVIEW ====================

    private void updatePreview() {
        try {
            previewContainer.removeAllViews();
            previewContainer.setBackgroundColor(safeColor(current.getCardColor()));

            int pad = Math.max(current.getCardPadding(), 8);
            int radius = Math.max(current.getCardCornerRadius(), 4);

            // Header
            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.VERTICAL);
            header.setPadding(pad, pad, pad, pad);
            header.setBackgroundColor(safeColor(current.getPrimaryColor()));
            if (current.isRoundedCards()) {
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(safeColor(current.getPrimaryColor()));
                bg.setCornerRadius(radius * 2f);
                header.setBackground(bg);
            }

            TextView headerText = new TextView(this);
            headerText.setText(nvl(current.getAppName(), "Zero Stress"));
            headerText.setTextColor(safeColor(current.getBackgroundColor()));
            headerText.setTextSize(Math.max(current.getHeaderTextSize(), 14));
            headerText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            header.addView(headerText);

            TextView subtitle = new TextView(this);
            subtitle.setText(nvl(current.getWelcomeMessage(), "Welcome!"));
            subtitle.setTextColor(safeColor(current.getBackgroundColor()));
            subtitle.setTextSize(12);
            subtitle.setAlpha(0.8f);
            header.addView(subtitle);

            previewContainer.addView(header);

            // Nav bar
            TextView navBar = new TextView(this);
            navBar.setText("   \u2302 Home    \uD83D\uDCCA Stats    \uD83D\uDCAC Chat    \u2699\uFE0F More   ");
            navBar.setTextColor(safeColor(current.getTextColor()));
            navBar.setTextSize(10);
            navBar.setGravity(Gravity.CENTER);
            navBar.setPadding(pad, 8, pad, 8);
            navBar.setBackgroundColor(safeColor(current.getNavBarColor()));
            LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    Math.max(current.getNavBarHeight(), 40));
            navBar.setLayoutParams(navParams);
            previewContainer.addView(navBar);

            // Card
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(pad, pad, pad, pad);
            card.setBackgroundColor(safeColor(current.getCardColor()));
            if (current.isRoundedCards()) {
                GradientDrawable cardBg = new GradientDrawable();
                cardBg.setColor(safeColor(current.getCardColor()));
                cardBg.setCornerRadius(radius);
                cardBg.setStroke(2, safeColor(current.getBorderColor()));
                card.setBackground(cardBg);
            }
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 8, 0, 0);
            card.setLayoutParams(cardParams);

            TextView player = new TextView(this);
            player.setText("\uD83D\uDC64 Player Name  \uD83D\uDD34 Offline");
            player.setTextColor(safeColor(current.getTextColor()));
            player.setTextSize(Math.max(current.getBodyTextSize(), 12));
            card.addView(player);

            TextView stats = new TextView(this);
            stats.setText("Kills: 45 | Wins: 12 | Damage: 8500");
            stats.setTextColor(safeColor(current.getSecondaryTextColor()));
            stats.setTextSize(12);
            stats.setPadding(0, 4, 0, 4);
            card.addView(stats);

            // Input preview
            EditText inputPreview = new EditText(this);
            inputPreview.setHint("Input field preview");
            inputPreview.setTextSize(Math.max(current.getButtonTextSize(), 12));
            inputPreview.setTextColor(safeColor(current.getTextColor()));
            inputPreview.setBackgroundColor(safeColor(current.getInputColor()));
            LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    Math.max(current.getInputHeight(), 32));
            inputParams.setMargins(0, 0, 0, 8);
            inputPreview.setLayoutParams(inputParams);
            card.addView(inputPreview);

            // Button row
            LinearLayout btnRow = new LinearLayout(this);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            btnRow.setPadding(0, 8, 0, 0);

            Button primaryBtn = new Button(this);
            primaryBtn.setText("Primary");
            primaryBtn.setTextSize(Math.max(current.getButtonTextSize(), 12));
            primaryBtn.setBackgroundColor(safeColor(current.getPrimaryColor()));
            primaryBtn.setTextColor(Color.WHITE);
            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setColor(safeColor(current.getPrimaryColor()));
            btnBg.setCornerRadius(Math.max(current.getButtonCornerRadius(), 4));
            primaryBtn.setBackground(btnBg);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    Math.max(current.getButtonHeight(), 32));
            primaryBtn.setLayoutParams(btnParams);
            btnRow.addView(primaryBtn);

            Button dangerBtn = new Button(this);
            dangerBtn.setText("Danger");
            dangerBtn.setTextSize(Math.max(current.getButtonTextSize(), 12));
            dangerBtn.setTextColor(Color.WHITE);
            GradientDrawable dangerBg = new GradientDrawable();
            dangerBg.setColor(safeColor(current.getDangerColor()));
            dangerBg.setCornerRadius(Math.max(current.getButtonCornerRadius(), 4));
            dangerBtn.setBackground(dangerBg);
            LinearLayout.LayoutParams dangerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    Math.max(current.getButtonHeight(), 32));
            dangerParams.setMarginStart(8);
            dangerBtn.setLayoutParams(dangerParams);
            btnRow.addView(dangerBtn);

            card.addView(btnRow);
            previewContainer.addView(card);

        } catch (Exception e) {
            // Preview error — don't crash the activity
        }
    }

    // ==================== SAVE ====================

    private void saveTheme() {
        current.setAppName(inputAppName.getText().toString().trim());
        current.setWelcomeMessage(inputWelcomeMsg.getText().toString().trim());

        String[] fonts = {"sans-serif", "serif", "monospace"};
        current.setFontFamily(fonts[spinnerFont.getSelectedItemPosition()]);
        current.setCardCornerRadius(seekRadius.getProgress());
        current.setCardPadding(seekPadding.getProgress());

        themeManager.saveToFirestore(current, new ThemeManager.OnSaveCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(AdminCustomizerActivity.this,
                            "\u2705 Theme saved! Changes apply to all users.",
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(AdminCustomizerActivity.this,
                            "Failed to save: " + error,
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // ==================== HELPERS ====================

    private int safeColor(String hex) {
        try {
            return Color.parseColor(hex);
        } catch (Exception e) {
            return Color.GRAY;
        }
    }

    private int safeProgress(int value, int max, int def) {
        if (value < 0 || value > max) return def;
        return value;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String nvl(String s, String def) {
        return (s == null || s.isEmpty()) ? def : s;
    }
}
