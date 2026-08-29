package com.zerostress.manager;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
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

import com.google.android.material.color.MaterialColors;
import com.zerostress.manager.models.AppCustomizer;

import yuku.ambilwarna.AmbilWarnaDialog;

public class AdminCustomizerActivity extends AppCompatActivity {

    private ThemeManager themeManager;
    private AppCustomizer current;

    // Views
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

    // Color buttons
    private View btnPrimary, btnSecondary, btnBackground, btnCard;
    private View btnInput, btnNav, btnText, btnSecondaryText;
    private View btnDanger, btnWarning, btnBorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_customizer);

        themeManager = ThemeManager.getInstance(this);
        current = themeManager.getTheme();

        initViews();
        loadCurrentTheme();
        setupPresetThemes();

        setupListeners();
        updatePreview();
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

        // Back button
        findViewById(R.id.back_btn).setOnClickListener(v -> finish());

        // Save button
        findViewById(R.id.save_btn).setOnClickListener(v -> saveTheme());

        // Font spinner
        String[] fonts = {"Sans Serif", "Serif", "Monospace"};
        ArrayAdapter<String> fontAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, fonts);
        fontAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFont.setAdapter(fontAdapter);
    }

    private void loadCurrentTheme() {
        inputAppName.setText(current.getAppName());
        inputWelcomeMsg.setText(current.getWelcomeMessage());

        // Font
        String[] fonts = {"sans-serif", "serif", "monospace"};
        for (int i = 0; i < fonts.length; i++) {
            if (fonts[i].equals(current.getFontFamily())) {
                spinnerFont.setSelection(i);
                break;
            }
        }

        // SeekBars
        seekRadius.setProgress(current.getCardCornerRadius());
        radiusValue.setText(current.getCardCornerRadius() + "dp");
        seekPadding.setProgress(current.getCardPadding());
        paddingValue.setText(current.getCardPadding() + "dp");

        // Switches
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

        // Color grid
        setupColorButtons();
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

            // Color circle
            TextView circle = new TextView(this);
            circle.setText("\u25CF");
            circle.setTextSize(28);
            circle.setTextColor(Color.parseColor(preset[2]));
            circle.setGravity(Gravity.CENTER);
            item.addView(circle);

            // Label
            TextView label = new TextView(this);
            label.setText(preset[1]);
            label.setTextColor(Color.parseColor("#94a3b8"));
            label.setTextSize(10);
            label.setGravity(Gravity.CENTER);
            item.addView(label);

            item.setOnClickListener(v -> applyPreset(preset[0]));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            item.setLayoutParams(params);
            presetContainer.addView(item);
        }
    }

    private void applyPreset(String name) {
        current = AppCustomizer.getPreset(name);
        loadCurrentTheme();
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

            // Color circle
            TextView circle = new TextView(this);
            circle.setText("\u25CF");
            circle.setTextSize(32);
            circle.setTextColor(Color.parseColor(color[1]));
            circle.setGravity(Gravity.CENTER);
            item.addView(circle);

            // Label
            TextView label = new TextView(this);
            label.setText(color[0]);
            label.setTextColor(Color.parseColor("#94a3b8"));
            label.setTextSize(9);
            label.setGravity(Gravity.CENTER);
            item.addView(label);

            item.setOnClickListener(v -> showColorPicker(color[0], color[1], newColor -> {
                applyColor(color[0], newColor);
                setupColorButtons(); // Refresh
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

    private void showColorPicker(String title, String currentHex, ColorPickedListener listener) {
        int currentColor = Color.parseColor(currentHex);
        new yuku.ambilwarna.AmbilWarnaDialog(this, currentColor, new yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onOk(yuku.ambilwarna.AmbilWarnaDialog dialog, int color) {
                listener.onColorPicked(String.format("#%06X", 0xFFFFFF & color));
            }
            @Override
            public void onCancel(yuku.ambilwarna.AmbilWarnaDialog dialog) {}
        }).show();
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

        // Switches
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

        // Text changes
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
        previewContainer.removeAllViews();
        previewContainer.setBackgroundColor(Color.parseColor(current.getCardColor()));

        int pad = current.getCardPadding();
        int radius = current.getCardCornerRadius();

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(pad, pad, pad, pad);
        header.setBackgroundColor(Color.parseColor(current.getPrimaryColor()));
        if (current.isRoundedCards()) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor(current.getPrimaryColor()));
            bg.setCornerRadius(radius * 2f);
            header.setBackground(bg);
        }

        TextView headerText = new TextView(this);
        headerText.setText(current.getAppName());
        headerText.setTextColor(Color.parseColor(current.getBackgroundColor()));
        headerText.setTextSize(current.getHeaderTextSize());
        headerText.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(headerText);

        TextView subtitle = new TextView(this);
        subtitle.setText(current.getWelcomeMessage());
        subtitle.setTextColor(Color.parseColor(current.getBackgroundColor()));
        subtitle.setTextSize(12);
        subtitle.setAlpha(0.8f);
        header.addView(subtitle);

        previewContainer.addView(header);

        // Card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(pad, pad, pad, pad);
        card.setBackgroundColor(Color.parseColor(current.getCardColor()));
        if (current.isRoundedCards()) {
            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setColor(Color.parseColor(current.getCardColor()));
            cardBg.setCornerRadius(radius);
            cardBg.setStroke(2, Color.parseColor(current.getBorderColor()));
            card.setBackground(cardBg);
        }
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 8, 0, 0);
        card.setLayoutParams(cardParams);

        // Player row
        TextView player = new TextView(this);
        player.setText("\uD83D\uDC64 Player Name  \uD83D\uDD34 Offline");
        player.setTextColor(Color.parseColor(current.getTextColor()));
        player.setTextSize(current.getBodyTextSize());
        card.addView(player);

        // Stats
        TextView stats = new TextView(this);
        stats.setText("Kills: 45 | Wins: 12 | Damage: 8500");
        stats.setTextColor(Color.parseColor(current.getSecondaryTextColor()));
        stats.setTextSize(12);
        stats.setPadding(0, 4, 0, 0);
        card.addView(stats);

        // Button row
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, 8, 0, 0);

        Button primaryBtn = new Button(this);
        primaryBtn.setText("Primary");
        primaryBtn.setTextSize(11);
        primaryBtn.setBackgroundColor(Color.parseColor(current.getPrimaryColor()));
        primaryBtn.setTextColor(Color.WHITE);
        btnRow.addView(primaryBtn);

        Button dangerBtn = new Button(this);
        dangerBtn.setText("Danger");
        dangerBtn.setTextSize(11);
        dangerBtn.setBackgroundColor(Color.parseColor(current.getDangerColor()));
        dangerBtn.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams dangerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dangerParams.setMarginStart(8);
        btnRow.addView(dangerBtn, dangerParams);

        card.addView(btnRow);
        previewContainer.addView(card);
    }

    // ==================== SAVE ====================

    private void saveTheme() {
        // Collect all values
        current.setAppName(inputAppName.getText().toString().trim());
        current.setWelcomeMessage(inputWelcomeMsg.getText().toString().trim());

        String[] fonts = {"sans-serif", "serif", "monospace"};
        current.setFontFamily(fonts[spinnerFont.getSelectedItemPosition()]);
        current.setCardCornerRadius(seekRadius.getProgress());
        current.setCardPadding(seekPadding.getProgress());

        // Save to Firestore + local
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
}
