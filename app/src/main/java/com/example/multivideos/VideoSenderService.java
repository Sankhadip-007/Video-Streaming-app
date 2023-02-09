package com.example.multivideos;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class VideoSenderService extends AppCompatActivity {

    private String videoName;
    private ServerSocket serverSocket;
    private Socket socket;
    private File videoFile;
    private OutputStream outputStream;

    VideoSenderService(String videoName){
        this.videoName=videoName;
    }


    public void startSendingVideo() {
       // this.videoName = videoName;
        // Get the video file from the video name
        videoFile = new File(getFilePathFromVideoName(videoName));
        // Start a new thread to listen for incoming connection requests
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(5000);
                    socket = serverSocket.accept();
                    // Send the video file to the client
                    outputStream = socket.getOutputStream();
                    FileInputStream inputStream = new FileInputStream(videoFile);
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    inputStream.close();
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private String getFilePathFromVideoName(String videoName) {
        // Returns the file path for the video with the given name
        File appDir = new File(getApplicationContext().getCacheDir(),videoName);
        return videoFile.getAbsolutePath();
    }
}
