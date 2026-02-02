package com.musicplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import java.util.ArrayList;

public class MusicService extends Service {

    private final IBinder binder = new MusicBinder();
    private MediaPlayer mediaPlayer;
    private ArrayList<MainActivity.AudioFile> songs;
    private int songPos;
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "MusicPlayerChannel";
    
    private MediaSessionCompat mediaSession;

    @Override
    public void onCreate() {
        super.onCreate();
        songPos = 0;
        mediaPlayer = new MediaPlayer();
        initMusicPlayer();
        initMediaSession();
        createNotificationChannel();
    }

    private void initMediaSession() {
        mediaSession = new MediaSessionCompat(this, "MusicService");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() { go(); }
            @Override
            public void onPause() { pausePlayer(); }
            @Override
            public void onSkipToNext() { 
                Intent intent = new Intent("com.musicplayer.NEXT");
                sendBroadcast(intent);
            }
            @Override
            public void onSkipToPrevious() {
                Intent intent = new Intent("com.musicplayer.PREVIOUS");
                sendBroadcast(intent);
            }
            @Override
            public void onSeekTo(long pos) { seek((int)pos); }
        });
    }

    public void initMusicPlayer() {
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        mediaPlayer.setAudioAttributes(audioAttributes);
        mediaPlayer.setOnCompletionListener(mp -> {
            if (onCompletionListener != null) onCompletionListener.onCompletion();
        });
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            mp.reset();
            return false;
        });
    }

    public void setList(ArrayList<MainActivity.AudioFile> theSongs) {
        songs = theSongs;
    }

    public class MusicBinder extends Binder {
        MusicService getService() { return MusicService.this; }
    }

    @Override
    public IBinder onBind(Intent intent) { return binder; }

    @Override
    public boolean onUnbind(Intent intent) {
        mediaSession.release();
        return false;
    }

    public void playSong() {
        mediaPlayer.reset();
        if (songs == null || songPos < 0 || songPos >= songs.size()) return;
        
        MainActivity.AudioFile playSong = songs.get(songPos);
        try {
            mediaPlayer.setDataSource(getApplicationContext(), Uri.parse(playSong.path));
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                updateMediaSessionMetadata();
                updateMediaSessionState(PlaybackStateCompat.STATE_PLAYING);
                showNotification();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setSong(int songIndex) { songPos = songIndex; }

    public int getPosn() { return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0; }
    public int getDur() { return mediaPlayer != null ? mediaPlayer.getDuration() : 0; }
    public boolean isPng() { return mediaPlayer != null && mediaPlayer.isPlaying(); }

    public void pausePlayer() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            updateMediaSessionState(PlaybackStateCompat.STATE_PAUSED);
            showNotification();
        }
    }

    public void seek(int posn) { if (mediaPlayer != null) mediaPlayer.seekTo(posn); }
    public void go() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            updateMediaSessionState(PlaybackStateCompat.STATE_PLAYING);
            showNotification();
        }
    }

    private void updateMediaSessionMetadata() {
        MainActivity.AudioFile song = songs.get(songPos);
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getDur());

        Bitmap art = getAlbumArt(song.path);
        if (art != null) builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, art);

        mediaSession.setMetadata(builder.build());
    }

    private void updateMediaSessionState(int state) {
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                        PlaybackStateCompat.ACTION_SEEK_TO)
                .setState(state, getPosn(), 1.0f);
        mediaSession.setPlaybackState(stateBuilder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID, "Music Playback", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
        }
    }

    private void showNotification() {
        MainActivity.AudioFile song = songs.get(songPos);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(song.title)
                .setContentText(song.artist)
                .setLargeIcon(getAlbumArt(song.path))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .addAction(android.R.drawable.ic_media_previous, "Previous", getPendingIntent("PREVIOUS"))
                .addAction(mediaPlayer.isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play, "Play/Pause", getPendingIntent("PAUSE"))
                .addAction(android.R.drawable.ic_media_next, "Next", getPendingIntent("NEXT"));

        Notification notification = builder.build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private PendingIntent getPendingIntent(String action) {
        Intent intent = new Intent("com.musicplayer." + action);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private Bitmap getAlbumArt(String path) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, Uri.parse(path));
            byte[] art = retriever.getEmbeddedPicture();
            if (art != null) return BitmapFactory.decodeByteArray(art, 0, art.length);
        } catch (Exception ignored) {}
        finally { try { retriever.release(); } catch (Exception ignored) {} }
        return null;
    }

    public interface OnCompletionListener { void onCompletion(); }
    private OnCompletionListener onCompletionListener;
    public void setOnCompletionListener(OnCompletionListener listener) { this.onCompletionListener = listener; }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        if (mediaSession != null) mediaSession.release();
        super.onDestroy();
    }
}
