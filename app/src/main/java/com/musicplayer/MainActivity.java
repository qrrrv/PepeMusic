package com.musicplayer;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int PICK_AUDIO_REQUEST = 200;

    private RecyclerView trackRecyclerView;
    private TextView currentTrackText, currentArtistText, currentTimeText, totalTimeText;
    private SeekBar seekBar;
    private ImageButton playPauseBtn;
    private ImageButton nextBtn, prevBtn, shuffleBtn, repeatBtn, settingsBtn, addMusicFab;
    private ImageView mainAlbumArt;
    private MaterialButton addMusicBtn;
    private LinearLayout emptyStateLayout;

    private MusicService musicService;
    private Intent playIntent;
    private boolean musicBound = false;

    private ArrayList<AudioFile> audioFiles = new ArrayList<>();
    private TrackAdapter adapter;
    private int currentPosition = -1;
    private boolean isShuffle = false;
    private int repeatMode = 0; 

    private MusicDatabaseHelper dbHelper;
    private Handler handler = new Handler();
    private long trackStartTime = 0;

    private BroadcastReceiver musicReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.musicplayer.NEXT".equals(action)) playNext();
            else if ("com.musicplayer.PREVIOUS".equals(action)) playPrevious();
            else if ("com.musicplayer.PAUSE".equals(action)) togglePlayPause();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new MusicDatabaseHelper(this);
        initViews();
        loadSavedTracks();
        checkPermissions();

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.musicplayer.NEXT");
        filter.addAction("com.musicplayer.PREVIOUS");
        filter.addAction("com.musicplayer.PAUSE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(musicReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(musicReceiver, filter);
        }
    }
    
    private void applyTheme() {
        android.content.SharedPreferences prefs = getSharedPreferences("MusicPlayerSettings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", true);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void initViews() {
        trackRecyclerView = findViewById(R.id.trackRecyclerView);
        currentTrackText = findViewById(R.id.currentTrackText);
        currentArtistText = findViewById(R.id.currentArtistText);
        currentTimeText = findViewById(R.id.currentTimeText);
        totalTimeText = findViewById(R.id.totalTimeText);
        seekBar = findViewById(R.id.seekBar);
        playPauseBtn = findViewById(R.id.playPauseBtn);
        nextBtn = findViewById(R.id.nextBtn);
        prevBtn = findViewById(R.id.prevBtn);
        shuffleBtn = findViewById(R.id.shuffleBtn);
        repeatBtn = findViewById(R.id.repeatBtn);
        addMusicBtn = findViewById(R.id.addMusicBtn);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        settingsBtn = findViewById(R.id.settingsBtn);
        mainAlbumArt = findViewById(R.id.mainAlbumArt);
        addMusicFab = findViewById(R.id.addMusicFab);

        trackRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TrackAdapter(audioFiles);
        trackRecyclerView.setAdapter(adapter);

        setupListeners();
        updateUIState();
    }

    private void setupListeners() {
        View.OnClickListener addListener = v -> {
            v.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.button_press));
            v.postDelayed(() -> {
                v.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.button_release));
                openFilePicker();
            }, 150);
        };

        addMusicBtn.setOnClickListener(addListener);
        if (addMusicFab != null) addMusicFab.setOnClickListener(addListener);
        
        playPauseBtn.setOnClickListener(v -> {
            v.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.rotate_scale));
            togglePlayPause();
        });
        
        nextBtn.setOnClickListener(v -> {
            v.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.button_press));
            v.postDelayed(() -> {
                v.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.button_release));
                playNext();
            }, 150);
        });
        
        prevBtn.setOnClickListener(v -> {
            v.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.button_press));
            v.postDelayed(() -> {
                v.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.button_release));
                playPrevious();
            }, 150);
        });
        
        shuffleBtn.setOnClickListener(v -> {
            v.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.button_press));
            v.postDelayed(() -> {
                v.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.button_release));
                toggleShuffle();
            }, 150);
        });
        
        repeatBtn.setOnClickListener(v -> {
            v.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.button_press));
            v.postDelayed(() -> {
                v.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.button_release));
                toggleRepeat();
            }, 150);
        });
        
        settingsBtn.setOnClickListener(v -> {
            v.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.button_press));
            v.postDelayed(() -> {
                v.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.button_release));
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }, 150);
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && musicService != null) musicService.seek(progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateUIState() {
        if (audioFiles.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            trackRecyclerView.setVisibility(View.GONE);
            if (addMusicFab != null) addMusicFab.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            trackRecyclerView.setVisibility(View.VISIBLE);
            if (addMusicFab != null) addMusicFab.setVisibility(View.VISIBLE);
        }
    }

    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicService.setList(audioFiles);
            musicBound = true;
            musicService.setOnCompletionListener(() -> {
                if (repeatMode == 2) playTrack(currentPosition);
                else playNext();
            });
        }
        @Override
        public void onServiceDisconnected(ComponentName name) { musicBound = false; }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.READ_MEDIA_AUDIO,
                        Manifest.permission.POST_NOTIFICATIONS
                }, PERMISSION_REQUEST_CODE);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, PICK_AUDIO_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_AUDIO_REQUEST && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    addTrackFromUri(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                addTrackFromUri(data.getData());
            }
            adapter.notifyDataSetChanged();
            updateUIState();
            if (musicService != null) musicService.setList(audioFiles);
        }
    }

    private void addTrackFromUri(Uri uri) {
        try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
        
        String title = "Unknown Track";
        String artist = "Unknown Artist";
        long duration = 0;

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, uri);
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durStr != null) duration = Long.parseLong(durStr);
            if (title == null || title.isEmpty()) title = uri.getLastPathSegment();
            if (artist == null || artist.isEmpty()) artist = "Unknown Artist";
        } catch (Exception e) { title = uri.getLastPathSegment(); }
        finally { try { retriever.release(); } catch (Exception ignored) {} }

        AudioFile track = new AudioFile(title, artist, uri.toString(), duration);
        
        boolean exists = false;
        for (AudioFile f : audioFiles) {
            if (f.path.equals(track.path)) { exists = true; break; }
        }
        
        if (!exists) {
            audioFiles.add(track);
            dbHelper.addTrack(track);
        }
    }

    private void loadSavedTracks() {
        audioFiles.clear();
        audioFiles.addAll(dbHelper.getAllTracks());
        adapter.notifyDataSetChanged();
        updateUIState();
    }

    private void playTrack(int position) {
        if (position < 0 || position >= audioFiles.size()) return;
        
        // Сохраняем статистику предыдущего трека
        if (currentPosition >= 0 && currentPosition < audioFiles.size() && trackStartTime > 0) {
            long playTime = System.currentTimeMillis() - trackStartTime;
            AudioFile prevTrack = audioFiles.get(currentPosition);
            dbHelper.incrementPlayCount(prevTrack.path, playTime);
        }
        
        currentPosition = position;
        trackStartTime = System.currentTimeMillis();
        
        musicService.setSong(position);
        musicService.playSong();
        
        AudioFile track = audioFiles.get(position);
        currentTrackText.setText(track.title);
        currentArtistText.setText(track.artist);
        playPauseBtn.setImageResource(R.drawable.msg_round_pause_m);
        
        loadArtwork(track.path, mainAlbumArt);
        adapter.notifyDataSetChanged();
        updateSeekBar();
        
        // Сохраняем информацию о текущем треке для виджета
        saveCurrentTrackInfo(track, true);
        // Обновляем виджет
        MusicWidgetProvider.updateAllWidgets(this);
    }

    private void loadArtwork(String path, ImageView imageView) {
        new Thread(() -> {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(this, Uri.parse(path));
                byte[] art = retriever.getEmbeddedPicture();
                if (art != null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
                    runOnUiThread(() -> {
                        Glide.with(MainActivity.this)
                            .load(bitmap)
                            .transform(new RoundedCorners(24))
                            .into(imageView);
                        imageView.setColorFilter(null);
                    });
                } else {
                    runOnUiThread(() -> {
                        imageView.setImageResource(android.R.drawable.ic_lock_silent_mode_off);
                        imageView.setColorFilter(ContextCompat.getColor(this, R.color.bright_purple), android.graphics.PorterDuff.Mode.SRC_IN);
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    imageView.setImageResource(android.R.drawable.ic_lock_silent_mode_off);
                    imageView.setColorFilter(ContextCompat.getColor(this, R.color.bright_purple), android.graphics.PorterDuff.Mode.SRC_IN);
                });
            } finally { 
                try { retriever.release(); } catch (Exception ignored) {} 
            }
        }).start();
    }

    private void togglePlayPause() {
        if (musicService == null) return;
        if (musicService.isPng()) {
            musicService.pausePlayer();
            playPauseBtn.setImageResource(R.drawable.msg_round_play_m);
            if (currentPosition >= 0 && currentPosition < audioFiles.size()) {
                saveCurrentTrackInfo(audioFiles.get(currentPosition), false);
            }
        } else {
            if (currentPosition == -1 && !audioFiles.isEmpty()) playTrack(0);
            else if (!audioFiles.isEmpty()) {
                musicService.go();
                playPauseBtn.setImageResource(R.drawable.msg_round_pause_m);
                updateSeekBar();
                saveCurrentTrackInfo(audioFiles.get(currentPosition), true);
            }
        }
        // Обновляем виджет
        MusicWidgetProvider.updateAllWidgets(this);
    }

    private void playNext() {
        if (audioFiles.isEmpty()) return;
        if (isShuffle) currentPosition = new Random().nextInt(audioFiles.size());
        else {
            currentPosition++;
            if (currentPosition >= audioFiles.size()) {
                if (repeatMode == 1) currentPosition = 0;
                else { currentPosition = audioFiles.size() - 1; return; }
            }
        }
        playTrack(currentPosition);
    }

    private void playPrevious() {
        if (audioFiles.isEmpty()) return;
        currentPosition--;
        if (currentPosition < 0) {
            if (repeatMode == 1) currentPosition = audioFiles.size() - 1;
            else { currentPosition = 0; return; }
        }
        playTrack(currentPosition);
    }

    private void toggleShuffle() {
        isShuffle = !isShuffle;
        shuffleBtn.setColorFilter(isShuffle ? ContextCompat.getColor(this, R.color.bright_purple) : ContextCompat.getColor(this, android.R.color.darker_gray));
    }

    private void toggleRepeat() {
        repeatMode = (repeatMode + 1) % 3;
        switch (repeatMode) {
            case 0: repeatBtn.setColorFilter(ContextCompat.getColor(this, android.R.color.darker_gray)); break;
            case 1: repeatBtn.setColorFilter(ContextCompat.getColor(this, R.color.bright_purple)); break;
            case 2: repeatBtn.setColorFilter(ContextCompat.getColor(this, R.color.light_purple)); break;
        }
    }

    private void updateSeekBar() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (musicService != null && musicService.isPng()) {
                    int current = musicService.getPosn();
                    int total = musicService.getDur();
                    seekBar.setMax(total);
                    seekBar.setProgress(current);
                    currentTimeText.setText(formatTime(current));
                    totalTimeText.setText(formatTime(total));
                    handler.postDelayed(this, 1000);
                }
            }
        }, 100);
    }

    private String formatTime(long ms) {
        int sec = (int) (ms / 1000);
        return String.format("%d:%02d", sec / 60, sec % 60);
    }

    private void deleteTrack(int position) {
        if (position < 0 || position >= audioFiles.size()) return;
        
        AudioFile track = audioFiles.get(position);
        new AlertDialog.Builder(this)
                .setTitle("Удалить трек")
                .setMessage("Вы уверены, что хотите удалить этот трек из списка?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    dbHelper.deleteTrack(track.path);
                    
                    if (currentPosition == position) {
                        if (musicService != null) {
                            musicService.pausePlayer();
                        }
                        currentPosition = -1;
                        currentTrackText.setText("Выберите трек");
                        currentArtistText.setText("Исполнитель");
                        mainAlbumArt.setImageResource(android.R.drawable.ic_lock_silent_mode_off);
                        playPauseBtn.setImageResource(R.drawable.msg_round_play_m);
                    } else if (currentPosition > position) {
                        currentPosition--;
                    }
                    
                    audioFiles.remove(position);
                    adapter.notifyDataSetChanged();
                    updateUIState();
                    
                    if (musicService != null) {
                        musicService.setList(audioFiles);
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
    
    private void saveCurrentTrackInfo(AudioFile track, boolean isPlaying) {
        android.content.SharedPreferences prefs = getSharedPreferences("CurrentTrack", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        
        if (track != null) {
            editor.putString("track_name", track.title);
            editor.putString("artist_name", track.artist);
            editor.putString("track_path", track.path);
        }
        editor.putBoolean("is_playing", isPlaying);
        editor.apply();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Сохраняем статистику при уходе из приложения
        if (currentPosition >= 0 && currentPosition < audioFiles.size() && trackStartTime > 0) {
            long playTime = System.currentTimeMillis() - trackStartTime;
            AudioFile track = audioFiles.get(currentPosition);
            dbHelper.incrementPlayCount(track.path, playTime);
            trackStartTime = System.currentTimeMillis(); // Сбрасываем для следующего сеанса
        }
    }

    @Override
    protected void onDestroy() {
        // Сохраняем статистику перед закрытием
        if (currentPosition >= 0 && currentPosition < audioFiles.size() && trackStartTime > 0) {
            long playTime = System.currentTimeMillis() - trackStartTime;
            AudioFile track = audioFiles.get(currentPosition);
            dbHelper.incrementPlayCount(track.path, playTime);
        }
        
        if (musicBound) { unbindService(musicConnection); musicBound = false; }
        try { unregisterReceiver(musicReceiver); } catch (Exception ignored) {}
        super.onDestroy();
    }

    public static class AudioFile {
        String title, artist, path;
        long duration;
        AudioFile(String t, String a, String p, long d) { title = t; artist = a; path = p; duration = d; }
    }

    private class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.ViewHolder> {
        private ArrayList<AudioFile> files;
        TrackAdapter(ArrayList<AudioFile> files) { this.files = files; }
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.track_item, parent, false));
        }
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AudioFile file = files.get(position);
            holder.title.setText(file.title);
            holder.artist.setText(file.artist);
            holder.itemView.setOnClickListener(v -> {
                v.startAnimation(android.view.animation.AnimationUtils.loadAnimation(MainActivity.this, R.anim.button_press));
                v.postDelayed(() -> playTrack(position), 100);
            });
            holder.menuBtn.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(MainActivity.this, v);
                popup.getMenu().add("Удалить");
                popup.setOnMenuItemClickListener(item -> { 
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        deleteTrack(pos);
                    }
                    return true; 
                });
                popup.show();
            });
            
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(MainActivity.this, Uri.parse(file.path));
                byte[] art = retriever.getEmbeddedPicture();
                if (art != null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
                    Glide.with(MainActivity.this)
                        .load(bitmap)
                        .transform(new RoundedCorners(16))
                        .into(holder.albumArt);
                    holder.albumArt.setColorFilter(null);
                } else {
                    holder.albumArt.setImageResource(android.R.drawable.ic_lock_silent_mode_off);
                    holder.albumArt.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.bright_purple), android.graphics.PorterDuff.Mode.SRC_IN);
                }
            } catch (Exception e) {
                holder.albumArt.setImageResource(android.R.drawable.ic_lock_silent_mode_off);
                holder.albumArt.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.bright_purple), android.graphics.PorterDuff.Mode.SRC_IN);
            } finally { 
                try { retriever.release(); } catch (Exception ignored) {} 
            }

            if (position == currentPosition) {
                holder.title.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.bright_purple));
                holder.itemView.setBackgroundResource(R.drawable.card_background);
            } else {
                holder.title.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.white));
                holder.itemView.setBackground(null);
            }
        }
        @Override
        public int getItemCount() { return files.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, artist;
            ImageView albumArt;
            ImageButton menuBtn;
            ViewHolder(View v) { 
                super(v); 
                title = v.findViewById(R.id.trackTitle); 
                artist = v.findViewById(R.id.trackArtist); 
                albumArt = v.findViewById(R.id.albumArt);
                menuBtn = v.findViewById(R.id.trackMenuBtn);
            }
        }
    }
}
