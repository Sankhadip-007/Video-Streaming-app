package com.example.multivideos;

import android.content.Context;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;

public class VideoReceiverService extends AppCompatActivity {

    private String hostAddress;
    private int port;
    private Socket socket;
    private InputStream inputStream;
    String videoName;
    Context context=this;
    VideoReceiverService(String hostAddress, int port,String vname){
        this.hostAddress = hostAddress;
        this.port = port;
        this.videoName = vname;
    }
        public  void request(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Create a socket to send the request
                try {
                    socket = new Socket(hostAddress, port);
                    // Get the OutputStream from the socket
                    OutputStream outputStream = socket.getOutputStream();
                   // Write the request message to the OutputStream
                    String requestMessage = videoName;
                    outputStream.write(requestMessage.getBytes());
                   // Close the OutputStream
                    outputStream.close();
                    socket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        });


     }
    public void startReceivingVideo() {
        // Start a new thread to connect to the server and receive the video
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //socket = new Socket(hostAddress, port);
                    inputStream = socket.getInputStream();

                    File videoFile = new File(context.getCacheDir(), videoName);
                    FileOutputStream fos = new FileOutputStream(videoFile);

                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        inputStream = Files.newInputStream(videoFile.toPath());
                    }
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

