package com.musicplayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;

public class MusicDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "music_player.db";
    private static final int DATABASE_VERSION = 2;
    public static final String TABLE_TRACKS = "tracks";

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_ARTIST = "artist";
    private static final String COLUMN_PATH = "path";
    private static final String COLUMN_DURATION = "duration";
    private static final String COLUMN_PLAY_COUNT = "play_count";
    private static final String COLUMN_PLAY_TIME = "play_time";
    private static final String COLUMN_LAST_PLAYED = "last_played";

    public MusicDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_TRACKS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_ARTIST + " TEXT, " +
                COLUMN_PATH + " TEXT UNIQUE, " +
                COLUMN_DURATION + " INTEGER, " +
                COLUMN_PLAY_COUNT + " INTEGER DEFAULT 0, " +
                COLUMN_PLAY_TIME + " INTEGER DEFAULT 0, " +
                COLUMN_LAST_PLAYED + " INTEGER DEFAULT 0)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_TRACKS + " ADD COLUMN " + COLUMN_PLAY_COUNT + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_TRACKS + " ADD COLUMN " + COLUMN_PLAY_TIME + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_TRACKS + " ADD COLUMN " + COLUMN_LAST_PLAYED + " INTEGER DEFAULT 0");
        }
    }

    public void addTrack(MainActivity.AudioFile track) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, track.title);
        values.put(COLUMN_ARTIST, track.artist);
        values.put(COLUMN_PATH, track.path);
        values.put(COLUMN_DURATION, track.duration);
        db.insertWithOnConflict(TABLE_TRACKS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
    }

    public void incrementPlayCount(String path, long playTime) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_TRACKS + 
                   " SET " + COLUMN_PLAY_COUNT + " = " + COLUMN_PLAY_COUNT + " + 1, " +
                   COLUMN_PLAY_TIME + " = " + COLUMN_PLAY_TIME + " + ?, " +
                   COLUMN_LAST_PLAYED + " = ? " +
                   " WHERE " + COLUMN_PATH + " = ?",
                   new Object[]{playTime, System.currentTimeMillis(), path});
        db.close();
    }

    public ArrayList<MainActivity.AudioFile> getAllTracks() {
        ArrayList<MainActivity.AudioFile> tracks = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_TRACKS, null);

        if (cursor.moveToFirst()) {
            do {
                tracks.add(new MainActivity.AudioFile(
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getLong(4)
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return tracks;
    }

    public void deleteTrack(String path) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRACKS, COLUMN_PATH + "=?", new String[]{path});
        db.close();
    }
}
