package com.example.multivideos;

import static android.content.ContentValues.TAG;
import android.util.Log;

import java.io.Serializable;

public class Video implements Serializable{
    private String id;
    private String name;
    private String videoUrl;

    public String getVideoUrl() {
        //Log.d(TAG, "Video url() = "+videoUrl);
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
