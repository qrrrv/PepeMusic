package com.musicplayer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.slider.Slider;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private static final String PREFS_NAME = "MusicPlayerSettings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        setupStatistics();
        setupDarkMode();
        setupAudioQuality();
        setupEqualizer();
        setupSleepTimer();
        setupClearCache();
        setupDeveloperLink();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void applyTheme() {
        boolean isDarkMode = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean("dark_mode", true);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void setupStatistics() {
        LinearLayout statisticsLayout = findViewById(R.id.statisticsLayout);
        statisticsLayout.setOnClickListener(v -> {
            Intent intent = new Intent(this, StatisticsActivity.class);
            startActivity(intent);
        });
    }

    private void setupDarkMode() {
        Switch darkModeSwitch = findViewById(R.id.darkModeSwitch);
        boolean isDarkMode = prefs.getBoolean("dark_mode", true);
        darkModeSwitch.setChecked(isDarkMode);

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }

            recreate();
        });
    }

    private void setupAudioQuality() {
        Slider qualitySlider = findViewById(R.id.qualitySlider);
        TextView qualityText = findViewById(R.id.qualityText);

        float quality = prefs.getFloat("audio_quality", 1.0f);
        qualitySlider.setValue(quality);
        updateQualityText(qualityText, quality);

        qualitySlider.addOnChangeListener((slider, value, fromUser) -> {
            prefs.edit().putFloat("audio_quality", value).apply();
            updateQualityText(qualityText, value);
        });
    }

    private void updateQualityText(TextView textView, float value) {
        String quality;
        if (value == 0.0f) quality = "Низкое";
        else if (value == 0.5f) quality = "Ниже среднего";
        else if (value == 1.0f) quality = "Среднее";
        else if (value == 1.5f) quality = "Высокое";
        else quality = "Максимальное";

        textView.setText("Качество: " + quality);
    }

    private void setupEqualizer() {
        Switch equalizerSwitch = findViewById(R.id.equalizerSwitch);
        boolean isEqualizerEnabled = prefs.getBoolean("equalizer_enabled", false);
        equalizerSwitch.setChecked(isEqualizerEnabled);

        equalizerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("equalizer_enabled", isChecked).apply();
            Toast.makeText(this, isChecked ? "Эквалайзер включен" : "Эквалайзер выключен",
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void setupSleepTimer() {
        LinearLayout sleepTimerLayout = findViewById(R.id.sleepTimerLayout);
        sleepTimerLayout.setOnClickListener(v -> {
            Toast.makeText(this, "Функция в разработке", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupClearCache() {
        LinearLayout clearCacheLayout = findViewById(R.id.clearCacheLayout);
        clearCacheLayout.setOnClickListener(v -> {
            Toast.makeText(this, "Кэш очищен", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupDeveloperLink() {
        TextView developerLink = findViewById(R.id.developerLink);
        developerLink.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://t.me/JavaScript_XD"));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
