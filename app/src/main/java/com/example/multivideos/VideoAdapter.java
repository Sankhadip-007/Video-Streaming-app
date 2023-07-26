package com.example.multivideos;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.ViewHolder> implements Filterable {
    private List<Video> allVideos;
    List<Video> videoListAll=new ArrayList<>(); // for filtering
    private final Context context;

    public VideoAdapter(Context ctx, List<Video> videos)
    {
        this.allVideos = videos;
        this.context = ctx;
        videoListAll.addAll(allVideos);
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
            Intent i = new Intent(context, Player.class); //
            i.putExtras(b);
            v.getContext().startActivity(i);
        }
    });
    }

    @Override
    public int getItemCount() {
        return  allVideos.size();
    }

    @Override
    public Filter getFilter() {
        return filter;
    }
    Filter filter =new Filter() {
        // run on background
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {List<Video> filteredList=new ArrayList<>();
        if(constraint.length() == 0){
            filteredList.addAll(videoListAll);
        }
        else {
            for (Video vid:videoListAll){
                if(vid.getName().toLowerCase().contains(constraint.toString().toLowerCase())){
                    filteredList.add(vid);
                }
            }
        }
        FilterResults filterResults= new FilterResults();
        filterResults.values=filteredList;
        return filterResults;
    }
    // run on ui
    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
     allVideos.clear();
     allVideos.addAll((Collection<? extends Video>) results.values);
     notifyDataSetChanged();
    }
};
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
