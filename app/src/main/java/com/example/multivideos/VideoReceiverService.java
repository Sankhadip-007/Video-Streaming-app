package com.example.multivideos;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class VideoReceiverService extends AppCompatActivity {

    private String hostAddress;
    private int port;
    private Socket socket;
    private InputStream inputStream;
    String videoName;

    public void startReceivingVideo(String hostAddress, int port,String vname) {
        this.hostAddress = hostAddress;
        this.port = port;
        this.videoName=vname;
        // Start a new thread to connect to the server and receive the video
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(hostAddress, port);
                    inputStream = socket.getInputStream();
                    File videoFile = new File(getApplicationContext().getCacheDir(), "receivedVideo.mp4");
                    FileOutputStream fos = new FileOutputStream(videoFile);

                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    inputStream = new FileInputStream(videoFile);
                    fos.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    public InputStream getVideoInputStream() {
        return inputStream;
    }
}

