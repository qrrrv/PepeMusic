package com.musicplayer;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class StatisticsActivity extends AppCompatActivity {

    private MusicDatabaseHelper dbHelper;
    private TextView totalTimeText, totalTracksText, favoriteTrackText, favoriteArtistText;
    private RecyclerView topTracksRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Статистика");
        }

        dbHelper = new MusicDatabaseHelper(this);

        totalTimeText = findViewById(R.id.totalTimeText);
        totalTracksText = findViewById(R.id.totalTracksText);
        favoriteTrackText = findViewById(R.id.favoriteTrackText);
        favoriteArtistText = findViewById(R.id.favoriteArtistText);
        topTracksRecyclerView = findViewById(R.id.topTracksRecyclerView);

        topTracksRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadStatistics();
    }

    private void loadStatistics() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Общее время прослушивания
        long totalPlayTime = 0;
        Cursor cursor = db.rawQuery("SELECT SUM(play_time) FROM " + MusicDatabaseHelper.TABLE_TRACKS, null);
        if (cursor.moveToFirst()) {
            totalPlayTime = cursor.getLong(0);
        }
        cursor.close();

        // Общее количество треков
        int totalTracks = 0;
        cursor = db.rawQuery("SELECT COUNT(*) FROM " + MusicDatabaseHelper.TABLE_TRACKS, null);
        if (cursor.moveToFirst()) {
            totalTracks = cursor.getInt(0);
        }
        cursor.close();

        // Самый прослушиваемый трек
        String favoriteTrack = "—";
        cursor = db.rawQuery(
            "SELECT title FROM " + MusicDatabaseHelper.TABLE_TRACKS + 
            " ORDER BY play_count DESC LIMIT 1", null);
        if (cursor.moveToFirst()) {
            favoriteTrack = cursor.getString(0);
        }
        cursor.close();

        // Любимый исполнитель
        String favoriteArtist = "—";
        Map<String, Integer> artistCounts = new HashMap<>();
        cursor = db.rawQuery(
            "SELECT artist, SUM(play_count) as total FROM " + MusicDatabaseHelper.TABLE_TRACKS + 
            " GROUP BY artist ORDER BY total DESC LIMIT 1", null);
        if (cursor.moveToFirst()) {
            favoriteArtist = cursor.getString(0);
        }
        cursor.close();

        // Топ треков
        List<TrackStats> topTracks = new ArrayList<>();
        cursor = db.rawQuery(
            "SELECT title, artist, play_count FROM " + MusicDatabaseHelper.TABLE_TRACKS + 
            " ORDER BY play_count DESC LIMIT 10", null);
        while (cursor.moveToNext()) {
            String title = cursor.getString(0);
            String artist = cursor.getString(1);
            int playCount = cursor.getInt(2);
            topTracks.add(new TrackStats(title, artist, playCount));
        }
        cursor.close();

        // Отображение данных
        totalTimeText.setText(formatTime(totalPlayTime));
        totalTracksText.setText(String.valueOf(totalTracks));
        favoriteTrackText.setText(favoriteTrack);
        favoriteArtistText.setText(favoriteArtist);

        TopTracksAdapter adapter = new TopTracksAdapter(topTracks);
        topTracksRecyclerView.setAdapter(adapter);
    }

    private String formatTime(long milliseconds) {
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60;
        
        if (hours > 0) {
            return String.format("%d ч %d мин", hours, minutes);
        } else {
            return String.format("%d мин", minutes);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    static class TrackStats {
        String title;
        String artist;
        int playCount;

        TrackStats(String title, String artist, int playCount) {
            this.title = title;
            this.artist = artist;
            this.playCount = playCount;
        }
    }
}
