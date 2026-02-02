package com.musicplayer;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.widget.RemoteViews;

public class MusicWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_PLAY_PAUSE = "com.musicplayer.WIDGET_PLAY_PAUSE";
    public static final String ACTION_NEXT = "com.musicplayer.WIDGET_NEXT";
    public static final String ACTION_PREVIOUS = "com.musicplayer.WIDGET_PREVIOUS";
    public static final String ACTION_UPDATE = "com.musicplayer.WIDGET_UPDATE";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        
        String action = intent.getAction();
        if (action == null) return;

        switch (action) {
            case ACTION_PLAY_PAUSE:
                Intent playPauseIntent = new Intent(context, MusicService.class);
                playPauseIntent.setAction("com.musicplayer.PAUSE");
                context.sendBroadcast(playPauseIntent);
                break;
                
            case ACTION_NEXT:
                Intent nextIntent = new Intent(context, MusicService.class);
                nextIntent.setAction("com.musicplayer.NEXT");
                context.sendBroadcast(nextIntent);
                break;
                
            case ACTION_PREVIOUS:
                Intent prevIntent = new Intent(context, MusicService.class);
                prevIntent.setAction("com.musicplayer.PREVIOUS");
                context.sendBroadcast(prevIntent);
                break;
                
            case ACTION_UPDATE:
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                    new ComponentName(context, MusicWidgetProvider.class));
                onUpdate(context, appWidgetManager, appWidgetIds);
                break;
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences("MusicWidget_" + appWidgetId, Context.MODE_PRIVATE);
        SharedPreferences musicPrefs = context.getSharedPreferences("CurrentTrack", Context.MODE_PRIVATE);
        
        // Получаем настройки виджета
        boolean showAlbumArt = prefs.getBoolean("show_album_art", true);
        boolean showProgressBar = prefs.getBoolean("show_progress", true);
        int widgetTheme = prefs.getInt("theme", 0); // 0 = фиолетовая, 1 = темная, 2 = светлая
        int transparency = prefs.getInt("transparency", 200);
        
        // Выбираем layout в зависимости от темы
        int layoutId = getLayoutForTheme(widgetTheme);
        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);
        
        // Получаем информацию о текущем треке
        String trackName = musicPrefs.getString("track_name", "Pepe Музыка");
        String artistName = musicPrefs.getString("artist_name", "Нажмите для воспроизведения");
        String trackPath = musicPrefs.getString("track_path", "");
        boolean isPlaying = musicPrefs.getBoolean("is_playing", false);
        
        // Устанавливаем текст
        views.setTextViewText(R.id.widget_track_name, trackName);
        views.setTextViewText(R.id.widget_artist_name, artistName);
        
        // Устанавливаем иконку Play/Pause
        views.setImageViewResource(R.id.widget_play_pause, 
            isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
        
        // Загружаем обложку альбома если нужно
        if (showAlbumArt && !trackPath.isEmpty()) {
            loadAlbumArt(context, views, trackPath);
        } else {
            views.setImageViewResource(R.id.widget_album_art, android.R.drawable.ic_lock_silent_mode_off);
        }
        
        // Показать/скрыть прогресс-бар
        views.setViewVisibility(R.id.widget_progress_bar, showProgressBar ? android.view.View.VISIBLE : android.view.View.GONE);
        
        // Настройка прозрачности фона (если поддерживается темой)
        try {
            views.setInt(R.id.widget_background, "setBackgroundColor", 
                android.graphics.Color.argb(transparency, 72, 9, 183));
        } catch (Exception ignored) {}
        
        // Настройка кнопок
        setupButton(context, views, R.id.widget_play_pause, ACTION_PLAY_PAUSE);
        setupButton(context, views, R.id.widget_next, ACTION_NEXT);
        setupButton(context, views, R.id.widget_previous, ACTION_PREVIOUS);
        
        // Клик на виджет открывает приложение
        Intent openAppIntent = new Intent(context, MainActivity.class);
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(context, 0, openAppIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_container, openAppPendingIntent);
        
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
    
    private static int getLayoutForTheme(int theme) {
        switch (theme) {
            case 1: return R.layout.widget_music_dark;
            case 2: return R.layout.widget_music_light;
            default: return R.layout.widget_music_purple;
        }
    }
    
    private static void setupButton(Context context, RemoteViews views, int buttonId, String action) {
        Intent intent = new Intent(context, MusicWidgetProvider.class);
        intent.setAction(action);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(buttonId, pendingIntent);
    }
    
    private static void loadAlbumArt(Context context, RemoteViews views, String trackPath) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(context, Uri.parse(trackPath));
            byte[] art = retriever.getEmbeddedPicture();
            
            if (art != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
                views.setImageViewBitmap(R.id.widget_album_art, bitmap);
            } else {
                views.setImageViewResource(R.id.widget_album_art, android.R.drawable.ic_lock_silent_mode_off);
            }
            
            retriever.release();
        } catch (Exception e) {
            views.setImageViewResource(R.id.widget_album_art, android.R.drawable.ic_lock_silent_mode_off);
        }
    }
    
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            SharedPreferences prefs = context.getSharedPreferences("MusicWidget_" + appWidgetId, Context.MODE_PRIVATE);
            prefs.edit().clear().apply();
        }
    }

    public static void updateAllWidgets(Context context) {
        Intent intent = new Intent(context, MusicWidgetProvider.class);
        intent.setAction(ACTION_UPDATE);
        context.sendBroadcast(intent);
    }
}
