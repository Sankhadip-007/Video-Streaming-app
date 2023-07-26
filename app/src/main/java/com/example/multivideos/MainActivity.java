package com.example.multivideos;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.exoplayer2.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {
    private final int READ_STORAGE_PERMISSION_REQUEST = 1;
    private final int REQUEST_LOCATION_PERMISSION = 1;
    public static final String TAG="TAG";
    RecyclerView videoList;
    TextView empty;
    VideoAdapter adapter;
    List<Video> all_videos;
    database_manager db;
    private Handler handler;
    private Runnable runnable;
    private int finalRssi, counter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestLocationPermission();
        requestStoragePermission();

        db=new database_manager(this);
        all_videos = new ArrayList<>();

        videoList =findViewById(R.id.videoList);
        videoList.setLayoutManager(new LinearLayoutManager(this));

        adapter=new VideoAdapter(this, all_videos);
        videoList.setAdapter(adapter);

        empty=findViewById(R.id.empty);
        getJsonData();

        rssi();

        //client client = new client(this);
        //client.registerService(8888, "exoplayer");

        server server=new server(this,"");
        server.discoverServices();
    }

    public void rssi(){
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = wifiManager.getConnectionInfo();
                int rssi = info.getRssi();

                finalRssi += rssi;
                counter += 1;

                if(counter%10 == 0){
                    System.out.println("RSSI = "+((int)(finalRssi/counter)));
                }
                handler.postDelayed(this, 1000);
            }
        };
        handler.postDelayed(runnable, 1000);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(REQUEST_LOCATION_PERMISSION)
    public void requestLocationPermission() {
        String perms = Manifest.permission.ACCESS_FINE_LOCATION;
        if(EasyPermissions.hasPermissions(this, perms)) {
            //Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
        else {
            EasyPermissions.requestPermissions(this, "Please grant the location permission", REQUEST_LOCATION_PERMISSION, perms);
        }
    }
    @AfterPermissionGranted(READ_STORAGE_PERMISSION_REQUEST)
    public void requestStoragePermission() {
        String perms = Manifest.permission.READ_EXTERNAL_STORAGE;
        if(EasyPermissions.hasPermissions(this, perms)) {
            //Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
        else {
            EasyPermissions.requestPermissions(this, "Please grant the storage permission", READ_STORAGE_PERMISSION_REQUEST, perms);
        }
    }

    public void getJsonData() {
        String URL="http://192.168.0.6:4000/videos";
        //String URL="http://192.168.0.5:4000/videos";
        //String URL="http://192.168.1.2:4000/videos";
        RequestQueue requestQueue= Volley.newRequestQueue(MainActivity.this);
        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.GET, URL, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                //Log.d(TAG, "onResponse: "+ response);
                try {
                    JSONArray videos = response.getJSONArray("videos");
                    //JSONArray videos = categoriesData.getJSONArray("videos");
                    if(videos.length()!=0){
                        empty.setVisibility(View.GONE);
                    }

                    for (int i = 0; i < videos.length(); i++){
                        JSONObject video=videos.getJSONObject(i);
                        Video v = new Video();
                        v.setId(video.getString("id"));
                        v.setName(video.getString("name"));

                        String url="http://192.168.0.6:4000/video/";
                        //String url="http://192.168.0.5:4000/video/";
                        //String url="http://192.168.1.2:4000/video/";
                        url=url.concat(video.getString("id"));

                        //saving to sqlite db
                        //db.addData(v);
                        Log.d(TAG, "url=: "+url);

                        v.setVideoUrl(url);

                        all_videos.add(v);
                        adapter.notifyDataSetChanged();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "onErrorResponse: " + error.getMessage());
                empty.setText(error.getMessage());
            }
        });
        requestQueue.add(objectRequest);
    }
}