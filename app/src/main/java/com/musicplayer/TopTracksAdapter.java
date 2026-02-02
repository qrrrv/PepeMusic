package com.musicplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TopTracksAdapter extends RecyclerView.Adapter<TopTracksAdapter.ViewHolder> {

    private List<StatisticsActivity.TrackStats> tracks;

    public TopTracksAdapter(List<StatisticsActivity.TrackStats> tracks) {
        this.tracks = tracks;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.top_track_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StatisticsActivity.TrackStats track = tracks.get(position);
        holder.positionText.setText(String.valueOf(position + 1));
        holder.titleText.setText(track.title);
        holder.artistText.setText(track.artist);
        holder.playCountText.setText(track.playCount + " прослушиваний");
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView positionText, titleText, artistText, playCountText;

        ViewHolder(View itemView) {
            super(itemView);
            positionText = itemView.findViewById(R.id.positionText);
            titleText = itemView.findViewById(R.id.titleText);
            artistText = itemView.findViewById(R.id.artistText);
            playCountText = itemView.findViewById(R.id.playCountText);
        }
    }
}
