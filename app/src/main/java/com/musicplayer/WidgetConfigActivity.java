package com.musicplayer;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public class WidgetConfigActivity extends Activity {

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private SharedPreferences prefs;
    
    private Switch showAlbumArtSwitch;
    private Switch showProgressSwitch;
    private RadioGroup themeRadioGroup;
    private SeekBar transparencySeekBar;
    private TextView transparencyValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_config);
        
        // Устанавливаем результат как CANCELED по умолчанию
        setResult(RESULT_CANCELED);
        
        // Получаем ID виджета из Intent
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        
        // Если ID невалидный - закрываем активность
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }
        
        prefs = getSharedPreferences("MusicWidget_" + appWidgetId, MODE_PRIVATE);
        
        initViews();
        loadSettings();
        setupListeners();
    }
    
    private void initViews() {
        showAlbumArtSwitch = findViewById(R.id.config_show_album_art);
        showProgressSwitch = findViewById(R.id.config_show_progress);
        themeRadioGroup = findViewById(R.id.config_theme_group);
        transparencySeekBar = findViewById(R.id.config_transparency);
        transparencyValue = findViewById(R.id.config_transparency_value);
        
        Button saveButton = findViewById(R.id.config_save_button);
        saveButton.setOnClickListener(v -> saveSettings());
    }
    
    private void loadSettings() {
        showAlbumArtSwitch.setChecked(prefs.getBoolean("show_album_art", true));
        showProgressSwitch.setChecked(prefs.getBoolean("show_progress", true));
        
        int theme = prefs.getInt("theme", 0);
        switch (theme) {
            case 0:
                themeRadioGroup.check(R.id.config_theme_purple);
                break;
            case 1:
                themeRadioGroup.check(R.id.config_theme_dark);
                break;
            case 2:
                themeRadioGroup.check(R.id.config_theme_light);
                break;
        }
        
        int transparency = prefs.getInt("transparency", 200);
        transparencySeekBar.setProgress(transparency);
        updateTransparencyValue(transparency);
    }
    
    private void setupListeners() {
        transparencySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateTransparencyValue(progress);
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
    
    private void updateTransparencyValue(int value) {
        int percent = (int) ((value / 255.0) * 100);
        transparencyValue.setText(percent + "%");
    }
    
    private void saveSettings() {
        SharedPreferences.Editor editor = prefs.edit();
        
        // Сохраняем настройки
        editor.putBoolean("show_album_art", showAlbumArtSwitch.isChecked());
        editor.putBoolean("show_progress", showProgressSwitch.isChecked());
        
        // Сохраняем тему
        int theme = 0;
        int checkedId = themeRadioGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.config_theme_dark) theme = 1;
        else if (checkedId == R.id.config_theme_light) theme = 2;
        editor.putInt("theme", theme);
        
        // Сохраняем прозрачность
        editor.putInt("transparency", transparencySeekBar.getProgress());
        
        editor.apply();
        
        // Обновляем виджет
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        MusicWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId);
        
        // Устанавливаем результат как OK
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }
}
