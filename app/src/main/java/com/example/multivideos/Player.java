package com.example.multivideos;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheEvictor;
import com.google.android.exoplayer2.upstream.cache.CacheKeyFactory;
import com.google.android.exoplayer2.upstream.cache.ContentMetadata;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Player extends AppCompatActivity {
    public static String FILE_NAME;
    String vname;
    String Loading_time;
    long st1, pt1;
    TextView mEditText;
    Handler customerHandler = new Handler();
    TextView timer1, url_view;
    StyledPlayerView playerView;
    ExoPlayer exoPlayer;
    static SimpleCache simpleCache;
    long MybufferingTime = 0;
    int set = 0;
    Uri videoUrl;
    File receivedFile;
    Context context = this;
    static int storage_exists = 0;

    long starttime = 0L, timemilli = 0L, timeswap = 0L, updatetime = 0L, min, secs, milliseconds;
    Runnable updateTimeThread = new Runnable() {
        @Override
        public void run() {
            timemilli = SystemClock.uptimeMillis() - starttime;
            updatetime = timeswap + timemilli;
            secs = (int) (updatetime / 1000);
            min = secs / 60;
            secs %= 60;
            milliseconds = (int) (updatetime % 1000);
            //System.out.println("Checking" + exoPlayer.isPlaying());

            if (!exoPlayer.isPlaying()) {
                //System.out.println("Here111");
                //ProgressBar progressBar=findViewById(R.id.progressBar);
                // progressBar.setVisibility(View.GONE);
            }
            if (set == 0) {
                pt1 = Calendar.getInstance().getTimeInMillis();
                long diff = st1 - pt1;
                //System.out.println("DDiff== " + diff);
                Date currentTime = Calendar.getInstance().getTime();
                Loading_time = String.valueOf(currentTime);
                timer1.setText(min + ":" + secs + ":" + milliseconds);
                Loading_time = Loading_time + " " + min + ":" + secs + ":" + milliseconds + "\n";
                View view = null;
                save(view);

                try {
                    postJsonData();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                //return 0;
            } else {
                if (set == 1) {
                    timer1.setText("Buffering");

                } else {
                    timer1.setText("" + min + ":" + String.format("%2d", secs) + ":" + String.format("%3d", milliseconds));
                }

            }

            customerHandler.postDelayed(this, 0);
            //return 0;
        }
    };
    public static final String TAG = "TAG";
    ProgressBar spinner;
    boolean cache = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // spinner = findViewById(R.id.progressBar);
        Intent i = getIntent();
        Bundle data = i.getExtras();
        Video v = (Video) data.getSerializable("videoData");
        String videoName = "Exoplayer" + v.getId() + v.getName();
        getSupportActionBar().setTitle(v.getName());
        Log.d(TAG, "onCreate:");

        TextView title = findViewById(R.id.videoTitle);
        title.setText(v.getName());

        vname = v.getName();
        videoUrl = Uri.parse(v.getVideoUrl());
        playerView = findViewById(R.id.exoPlayer);

        timer1 = (TextView) findViewById(R.id.timer);
        starttime = SystemClock.uptimeMillis();


        url_view = findViewById(R.id.url);
        url_view.setText(v.getVideoUrl());
        // exo player
        // simple cache

//        CacheEvictor cacheEvictor = new LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024);
//        StandaloneDatabaseProvider sdp = new StandaloneDatabaseProvider(getApplicationContext());
//        File file = new File(context.getCacheDir(), videoName);
//        if (simpleCache == null) {
//            simpleCache = new SimpleCache(file, cacheEvictor, sdp);
//        }
//        DefaultHttpDataSource.Factory dfh = new DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true);
//        DefaultDataSource.Factory dff = new DefaultDataSource.Factory(context, dfh);

        exoPlayer = new ExoPlayer.Builder(getApplicationContext()).build();
        playerView.setPlayer(exoPlayer);

        File cacheDir = getApplicationContext().getCacheDir();
        File[] cacheFiles = cacheDir.listFiles();
        int flag = 0;


        if(cacheFiles != null){
            for (File cacheFile : cacheFiles) {
                Log.d(TAG, vname+" Cache files : "+cacheFile.getName());
                if (cacheFile.isFile() && cacheFile.getName().equals(vname)) {
                    Log.d(TAG, "Entered playing from cache");
                    File filee = cacheFile;
                    MediaItem mediaItem = MediaItem.fromUri(filee.getPath());
                    exoPlayer.setMediaItem(mediaItem);
                    exoPlayer.prepare();
                    Toast.makeText(getApplicationContext(), "Playing from cache", Toast.LENGTH_SHORT).show();
                    exoPlayer.play();
                    flag = 1;
                    break;
                }
            }
        }
        //Default code
        //else {
        if(flag == 0) {
            File check;
            System.out.println("Vname = "+vname);

            File outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            String path = outputDir + "/"+ vname+".mp4";
            //String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/"+ vname+".mp4";
            System.out.println(path);

            receivedFile = new File(path);
            boolean exists = receivedFile.exists();
            if(!exists) {
                // Create a new instance of NsdServer and start the service
                //server server=new server(this,vname);
                //server.discoverServices();
                client client = new client(this, vname);
                client.registerService(8888);

                check=new File(path);
                exists=check.exists();
                exoPlayer = new ExoPlayer.Builder(context).build();
                playerView.setPlayer(exoPlayer);
                if (exists) {
                    playFromLocalStorage();
                }
                else {
                    try {
                        Thread.sleep(1500);
                        if(check.exists()) {
                            final Player activity = Player.this;
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    while(storage_exists == 0){}
                                    playFromLocalStorage();
                                }
                            });
                        }
                        else {
                            client.close();
                            MediaItem mediaItem = MediaItem.fromUri(videoUrl);
                            exoPlayer.setMediaItem(mediaItem);
                            exoPlayer.prepare();
                            Toast.makeText(getApplicationContext(), "Playing from server", Toast.LENGTH_SHORT).show();

                            //CACHE Implementation
                            String vUrl = v.getVideoUrl();
                            String fileName = v.getName();
                            Log.d(TAG, "URL() = "+vUrl+"   Name = "+fileName);

                            ExecutorService executorService = Executors.newSingleThreadExecutor();
                            executorService.execute(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        //Thread.sleep(3000);
                                        if(!check.exists()) {
                                            URL url = new URL(vUrl);
                                            URLConnection connection = url.openConnection();
                                            InputStream inputStream = new BufferedInputStream(connection.getInputStream());

                                            File cacheDir = getApplicationContext().getCacheDir();
                                            File videoFile = new File(cacheDir, fileName);
                                            OutputStream outputStream = new FileOutputStream(videoFile);

                                            byte[] buffer = new byte[1024];
                                            int bytesRead;
                                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                                outputStream.write(buffer, 0, bytesRead);
                                            }
                                            outputStream.close();
                                            inputStream.close();

                                            final Player activity = Player.this;
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    activity.runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Toast.makeText(context.getApplicationContext(), "Saved video to cache", Toast.LENGTH_SHORT).show();
                                                            MediaItem mediaItem = MediaItem.fromUri(videoFile.getPath());
                                                            exoPlayer.prepare();
                                                            long currentPos = exoPlayer.getCurrentPosition();
                                                            exoPlayer.setMediaItem(mediaItem);
                                                            exoPlayer.seekTo(currentPos);
                                                            exoPlayer.play();
                                                            Log.d(TAG, "Playing from cache from mid");
                                                            //client.addAttributes(vname);
                                                        }
                                                    });
                                                }
                                            }).start();
                                        }
                                    }
                                    catch(Exception e){
                                        Log.e(TAG, "ERROR while caching : ");
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            else {
                // video available in media folder
                playFromLocalStorage();
            }
            exoPlayer.play();
        }

        //cache = isVideoCached(videoUrl);
        //System.out.println("cache after= " + cache);
        System.out.println("Here");

        customerHandler.postDelayed(updateTimeThread, 0);
        mEditText = findViewById(R.id.editText);
        Button save1 = findViewById(R.id.save);
        Button load1 = findViewById(R.id.load);
        save1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                save(view);
            }
        });
        load1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                load(view);
            }
        });

        exoPlayer.addListener(new com.google.android.exoplayer2.Player.Listener() {
            @Override
            public void onIsLoadingChanged(boolean isLoading) {

                if (isLoading) {
                    MybufferingTime = System.currentTimeMillis();
                } else {
                    long currentTime = System.currentTimeMillis();
                    long bufferedTime = currentTime - MybufferingTime;
                    Log.d("Buffered Time", Long.toString(bufferedTime));
                    MybufferingTime = bufferedTime;
                }
                com.google.android.exoplayer2.Player.Listener.super.onIsLoadingChanged(isLoading);
            }
        });
    }

    void playFromLocalStorage(){
        DataSource.Factory dataSourceFactory = new FileDataSource.Factory();
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(Uri.fromFile(receivedFile)));
        exoPlayer.setMediaSource(mediaSource);
        exoPlayer.prepare();
        Toast.makeText(getApplicationContext(), "Playing from storage", Toast.LENGTH_SHORT).show();
    }
    private boolean isVideoCached(Uri uri) {
        CacheDataSource.Factory cdf = new CacheDataSource.Factory();
        CacheKeyFactory ckf = cdf.getCacheKeyFactory();
        String contentKey = ckf.buildCacheKey(new DataSpec(uri));
        ContentMetadata cmt = simpleCache.getContentMetadata(contentKey);
        long contentLength = ContentMetadata.getContentLength(cmt);
        if (contentLength < 0) {
            // this happens when player has never queried this urL over network
            // or has no info about size of the source
            return false;
        }
        long cachedlength = simpleCache.getCachedBytes(contentKey, 0L, contentLength);

        return contentLength >= cachedlength;
    }

    void writeToFile(String videoName, long bufferingTime, boolean isCached) {
        //File file=new File("F:\\AndroidDev\\test.txt");
        try {
            FileOutputStream fos = openFileOutput("F:\\AndroidDev\\test.txt", MODE_APPEND);
            String data = videoName + "\t" + bufferingTime + "\t" + (isCached ? "Cached" : "Not cached") + "\n";
            fos.write(data.getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save(View v) {
        String text = Loading_time;
        FILE_NAME = vname + ".txt";
        FileOutputStream fos = null;
        try {
            // fos = openFileOutput(FILE_NAME, MODE_PRIVATE);
            fos = openFileOutput(FILE_NAME, MODE_APPEND);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fos);
            myOutWriter.append(text);
            fos.write(text.getBytes());

            //Toast.makeText(this,"Saved to"+getFilesDir()+"/"+FILE_NAME,Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void load(View v) {
        FileInputStream fis = null;

        try {
            fis = openFileInput(FILE_NAME);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String text;
            while ((text = br.readLine()) != null) {
                sb.append(text).append("\n");

            }
            mEditText.setText(sb.toString());

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

    }
    RequestQueue requestQueue = null;
    private void postJsonData() throws JSONException {
        //System.out.println("{json calledddd..........}");
        Date currentTime = Calendar.getInstance().getTime();
        String date1 = String.valueOf(currentTime);
        String temp = vname + "       " + date1 + "      " + min + ":" + secs + ":" + milliseconds + "  " + cache;

        String URL = "http://192.168.0.6:4000/video";
        //String URL = "http://192.168.0.5:4000/video";
        //String URL = "http://192.168.1.2:4000/video";


        if(requestQueue == null){
            requestQueue = Volley.newRequestQueue(this);
        }

        JSONObject json1 = new JSONObject();
        json1.put("tejas", temp);
        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.POST, URL, json1, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                //Log.d(TAG, "response=: " + response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                com.google.android.exoplayer2.util.Log.e(TAG, "OnErrorResponse" + error.getMessage());
            }
        });
        requestQueue.add(objectRequest);
    }


    @Override
    public void onPause() {
        super.onPause();
        exoPlayer.setPlayWhenReady(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        exoPlayer.setPlayWhenReady(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        exoPlayer.release();
    }
    @Override
    protected void onStart() {
        super.onStart();
    }
}