package com.example.multivideos;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.ViewHolder> {

    private List<Video> allVideos;
    private final Context context;

    public VideoAdapter(Context ctx, List<Video> videos)
    {
        this.allVideos = videos;
        this.context = ctx;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v= LayoutInflater.from(parent.getContext()).inflate(R.layout.video_view,parent,false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder,int position) {
    holder.title.setText(allVideos.get(position).getName());
    holder.vv.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Bundle b = new Bundle();
            b.putSerializable("videoData", allVideos.get(holder.getAdapterPosition()));
            Intent i = new Intent(context, Player.class);
            i.putExtras(b);
            v.getContext().startActivity(i);
        }
    });
    }

    @Override
    public int getItemCount() {
        return  allVideos.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        ImageView videoImage;
        TextView title;
        View vv;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.VideoTitle);
            videoImage=itemView.findViewById((R.id.Video_Thumbnail));
            vv = itemView;
        }
    }
}
