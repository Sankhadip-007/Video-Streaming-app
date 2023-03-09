package com.example.multivideos;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
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
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheEvictor;
import com.google.android.exoplayer2.upstream.cache.CacheKeyFactory;
import com.google.android.exoplayer2.upstream.cache.ContentMetadata;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.ls.LSOutput;

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
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Player extends AppCompatActivity {
    public static String FILE_NAME;
    String vname;
    String Loading_time;
    long st1, pt1;
    TextView mEditText;
    Handler customerHandler = new Handler();
    LinearLayout container;
    TextView timer1, url_view;
    StyledPlayerView playerView;
    ExoPlayer exoPlayer;
    SimpleCache simpleCache;
    long MybufferingTime = 0;
    int set = 0;
    Uri videoUrl;
    BroadcastReceiver mReceiver;
    Context context = this;
    NsdManager nsdManager; NsdManager.DiscoveryListener discoveryListener;
    NsdManager.ResolveListener resolveListener;
    NsdManager nsdHelper;

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
            System.out.println("Checking" + exoPlayer.isPlaying());

            if (!exoPlayer.isPlaying()) {
                System.out.println("Here111");
                //ProgressBar progressBar=findViewById(R.id.progressBar);
                // progressBar.setVisibility(View.GONE);
            }
            if (set == 0) {
                pt1 = Calendar.getInstance().getTimeInMillis();
                long diff = st1 - pt1;
                System.out.println("DDiff== " + diff);
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

                return;
            } else {
                if (set == 1) {
                    timer1.setText("Buffering");

                } else {
                    timer1.setText("" + min + ":" + String.format("%2d", secs) + ":" + String.format("%3d", milliseconds));
                }

            }

            customerHandler.postDelayed(this, 0);
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
        final WifiP2pManager wifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        final WifiP2pManager.Channel channel = wifiP2pManager.initialize(this, getMainLooper(), null);

        Intent i = getIntent();
        Bundle data = i.getExtras();
        Video v = (Video) data.getSerializable("videoData");
        String videoName="Exoplayer"+v.getId()+v.getName();
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

        CacheEvictor cacheEvictor = new LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024);
        StandaloneDatabaseProvider sdp = new StandaloneDatabaseProvider(getApplicationContext());
        File file = new File(context.getCacheDir(), "EXOPlayer" + v.getId() + '_' + v.getName());
        if (simpleCache == null) {
            simpleCache = new SimpleCache(file, cacheEvictor, sdp);
        }
        DefaultHttpDataSource.Factory dfh = new DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true);
        DefaultDataSource.Factory dff = new DefaultDataSource.Factory(context, dfh);
        exoPlayer = new ExoPlayer.Builder(getApplicationContext()).build();
        playerView.setPlayer(exoPlayer);

        if (isVideoCached(videoUrl)) {
            CacheDataSource.Factory cdf = new CacheDataSource.Factory().setCache(simpleCache).
                    setUpstreamDataSourceFactory(dff).
                    setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);

            exoPlayer = new ExoPlayer.Builder(this).
                    setMediaSourceFactory(new DefaultMediaSourceFactory(cdf)).build();
            ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(cdf).
                    createMediaSource(MediaItem.fromUri(videoUrl));
            exoPlayer.setMediaSource(mediaSource, true);
            exoPlayer.prepare();
            exoPlayer.play();
        } else {
            System.out.println("Inside else");
            wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() { // Connect to the closest device
                    System.out.println("discoverPeers");
                    wifiP2pManager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
                        @Override
                        public void onPeersAvailable(WifiP2pDeviceList peers) {
                            List<WifiP2pDevice> devices = new ArrayList<>(peers.getDeviceList());
                            System.out.println("devices " + devices.size());
                            for (WifiP2pDevice device : devices) {
                                System.out.println("DeviceList: " + device.deviceName);
                            }
                            WifiP2pDevice closestDevice = null;
                            int closestRssi = Integer.MIN_VALUE;
                            for (WifiP2pDevice device : devices) {
                                // find the closest device based on its signal strength (RSSI)
                                if (device.deviceName != null && device.deviceAddress != null) {
                                    int rssi = device.deviceAddress.hashCode();
                                    if (rssi > closestRssi) {
                                        closestRssi = rssi;
                                        closestDevice = device;
                                    }
                                    System.out.println("Closest devicee: " + closestDevice.deviceName);
                                }
                            }

                            if (closestDevice != null) {
                                // connect to the closest device
                                WifiP2pConfig config = new WifiP2pConfig();
                                config.deviceAddress = closestDevice.deviceAddress;
                                String addr = closestDevice.deviceAddress;
                                wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
                                    @Override
                                    public void onSuccess() {// connection successful
                                        System.out.println("connection successful");
                                        VideoReceiverService videoReceiverService = new VideoReceiverService(addr, 5000, "EXOPlayer" + v.getId() + '_' + v.getName());
                                        videoReceiverService.request();
                                        videoReceiverService.startReceivingVideo();

                                        InputStream inputStream = videoReceiverService.getVideoInputStream();

                                        //DataSource.Factory dataSourceFactory = new InputStreamDataSourceFactory(inputStream);
                                        DataSource.Factory dataSourceFactory = new FileDataSource.Factory();
                                        CacheDataSource.Factory cdf1 = new CacheDataSource.Factory().setCache(simpleCache).
                                                setUpstreamDataSourceFactory(dataSourceFactory).
                                                setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
                                        MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                                                .createMediaSource(MediaItem.fromUri(Uri.fromFile(new File(context.getCacheDir(), videoName))));


                                        exoPlayer = new ExoPlayer.Builder(context).setMediaSourceFactory(new DefaultMediaSourceFactory(cdf1)).build();
                                        exoPlayer.setMediaSource(mediaSource, true);
                                        exoPlayer.prepare();
                                        Toast.makeText(context, "Playing from nearby devices", Toast.LENGTH_SHORT).show();
                                        exoPlayer.play();
                                    }

                                    @Override
                                    public void onFailure(int reason) {
                                        // connection failed
                                        System.out.println("Connection rejected");

                                    }
                                });
                            }
                        }
                    });
                }

                @Override
                public void onFailure(int i) {
                    // Handle failure -> go for server
                    exoPlayer = new ExoPlayer.Builder(getApplicationContext()).build();
                    playerView.setPlayer(exoPlayer);
                    MediaItem mediaItem = MediaItem.fromUri(videoUrl);
                    exoPlayer.setMediaItem(mediaItem);
                    exoPlayer.prepare();
                    Toast.makeText(getApplicationContext(), "Playing from server", Toast.LENGTH_SHORT).show();
                    exoPlayer.play();

                }
            });
        }


        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                    // Check to see if WiFi is enabled and notify appropriate activity
                    int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                    if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                        Toast.makeText(context, "Wifi P2P enabled", Toast.LENGTH_SHORT).show();
                        //Wifi P2P is enabled
                    } else {
                        //Wifi P2P is not enabled
                        Toast.makeText(getApplicationContext(), "Wifi P2P NOT enabled", Toast.LENGTH_SHORT).show();
                    }
                } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                    // Call WifiP2pManager.requestPeers() to get a list of current peers

                } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                    // Respond to new connection or disconnections
                    NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                    if (networkInfo.isConnected()) {
                        //We are connected with the other device, request connection info to find group owner IP
                        wifiP2pManager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener() {
                            @Override
                            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                                //Initiate the video sender service to send the video
                                Toast.makeText(getApplicationContext(), "Connection accepted " + info.toString(), Toast.LENGTH_SHORT).show();
                                VideoSenderService videoSenderService = new VideoSenderService(v.getName());
                                videoSenderService.startSendingVideo();
                            }
                        });
                    }
                } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                    // Respond to this device's wifi state changing
                }
            }
        };


        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        if (wifiManager != null) {
            boolean success = wifiManager.startScan();
            if (success) {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                List<ScanResult> results = wifiManager.getScanResults();
                for (ScanResult result : results) {
                    Log.d("Wi-Fi Networks", result.SSID + " - " + result.BSSID);
                }
            } else {
                Log.e("Wi-Fi Scan Error", "Could not start scan");
            }
        }


        cache = isVideoCached(videoUrl);
        System.out.println("cache after= " + cache);
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

        nsdManager.discoverServices(
                "_video_tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener);


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


    private void postJsonData() throws JSONException {
        System.out.println("{json calledddd..........}");
        Date currentTime = Calendar.getInstance().getTime();
        String date1 = String.valueOf(currentTime);
        String temp = vname + "       " + date1 + "      " + min + ":" + secs + ":" + milliseconds + "  " + cache;

        String URL = "http://192.168.54.200:4001/video";
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JSONObject json1 = new JSONObject();
        json1.put("tejas", temp);
        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.POST, URL, json1, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                Log.d(TAG, "response=: " + response);
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
        registerReceiver(mReceiver, filter);
        exoPlayer.setPlayWhenReady(true);
    }

/*    @Override
    public  void onBackPresses()
    {
        super.onBackPressed();
        setPlayPause(false);
        release();
        finish();
    }*/

}