package com.example.multivideos;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiManager;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.FileUtils;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;

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

public class MainActivity extends AppCompatActivity {
    public static final String TAG="TAG";
    RecyclerView videoList;
    TextView empty;
    VideoAdapter adapter;
    List<Video> all_videos;
    database_manager db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db=new database_manager(this);
        all_videos = new ArrayList<>();

        videoList =findViewById(R.id.videoList);
        videoList.setLayoutManager(new LinearLayoutManager(this));

        adapter=new VideoAdapter(this, all_videos);
        videoList.setAdapter(adapter);

        empty=findViewById(R.id.empty);
        getJsonData();

    }

    public void getJsonData() {
        String URL="http://172.16.26.201:4000/videos";
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

                    for (int i = 0; i< videos.length();i++){
                        JSONObject video=videos.getJSONObject(i);
                        Video v = new Video();
                        v.setId(video.getString("id"));
                        v.setName(video.getString("name"));
                        String url="http://172.16.26.201:4000/video/";
                        url=url.concat(video.getString("id"));

                        // saving to sqlite db
                        //db.addData(v);
                        Log.d(TAG, "url=: "+url);

                        v.setVideoUrl(url);

                        all_videos.add(v);
                        adapter.notifyDataSetChanged();;
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