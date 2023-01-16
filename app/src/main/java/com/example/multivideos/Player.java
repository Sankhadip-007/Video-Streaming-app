package com.example.multivideos;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import java.io.File;

public class Player extends AppCompatActivity {

    Handler customerHandler=new Handler();
    LinearLayout container;
    TextView timer1,url_view;
    StyledPlayerView playerView;
    ExoPlayer exoPlayer;
    ProgressBar pg;

    int set=0;
    long starttime=0L,timemilli=0L,timeswap=0L,updatetime=0L;
    Runnable updateTimeThread=new Runnable() {
        @Override
        public void run() {
            timemilli= SystemClock.uptimeMillis()-starttime;
            updatetime=timeswap+timemilli;
            int secs=(int)(updatetime/1000);
            int min=secs/60;
            secs%=60;
            int milliseconds=(int)(updatetime%1000);
            if(exoPlayer.isPlaying())
            {
                pg=findViewById(R.id.progressBar);
                pg.setVisibility(View.GONE);
                timer1.setText("Started Playng");
                set=1;
                customerHandler.removeCallbacks(updateTimeThread);
                System.out.println("buffered Time = "+min+":"+secs+":"+milliseconds);

            }else
            {
                if(set==1){timer1.setText("Buffering");

                }else {
                    timer1.setText("" + min + ":" + String.format("%2d", secs) + ":" + String.format("%3d", milliseconds));
                }

            }

            customerHandler.postDelayed(this,0);
        }
    };
    public static final String TAG = "TAG";
    ProgressBar spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        spinner = findViewById(R.id.progressBar);

        Intent i = getIntent();
        Bundle data = i.getExtras();
        Video v = (Video) data.getSerializable("videoData");

        getSupportActionBar().setTitle(v.getName());
        Log.d(TAG, "onCreate:");

        TextView title = findViewById(R.id.videoTitle);
        title.setText(v.getName());


        Uri videoUrl = Uri.parse(v.getVideoUrl());
        playerView=findViewById(R.id.exoPlayer);

         timer1=(TextView) findViewById(R.id.timer);
        starttime= SystemClock.uptimeMillis();

       customerHandler.postDelayed(updateTimeThread,0);

       url_view=findViewById(R.id.url);
       url_view.setText(v.getVideoUrl());


        // exo player

        // simple cache
        LeastRecentlyUsedCacheEvictor lru=new LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024);
        StandaloneDatabaseProvider sdp=new StandaloneDatabaseProvider(getApplicationContext());
        SimpleCache simpleCache=new SimpleCache(new File(getCacheDir(),"EXOPlayer"+v.getId()+"_"+v.getName()),lru,sdp);
        DefaultHttpDataSource.Factory dfh=new DefaultHttpDataSource.Factory().
                                          setAllowCrossProtocolRedirects(true);
        DefaultDataSource.Factory dff=new DefaultDataSource.Factory(getApplicationContext(),dfh);

        CacheDataSource.Factory cdf=new CacheDataSource.Factory().setCache(simpleCache).
                                    setUpstreamDataSourceFactory(dfh).
                                     setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);

        exoPlayer=new ExoPlayer.Builder(this).
                setMediaSourceFactory(new DefaultMediaSourceFactory(
                                     cdf)).build();
        // simple cache

        playerView.setPlayer(exoPlayer);
        MediaItem mediaItem=MediaItem.fromUri(videoUrl);
        ProgressiveMediaSource mediaSource= new ProgressiveMediaSource.Factory(cdf).
                                         createMediaSource(mediaItem);
        exoPlayer.setMediaSource(mediaSource);
        exoPlayer.prepare();
        exoPlayer.play();
        //
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == android.R.id.home)
        {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}