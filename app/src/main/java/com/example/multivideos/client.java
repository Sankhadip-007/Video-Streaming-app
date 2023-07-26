package com.example.multivideos;
import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.exoplayer2.MediaItem;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Time;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class client extends AppCompatActivity {
    final String SERVICE_TYPE = "_http._tcp.";
    final String SERVICE_NAME = "multivideos";
    final String TAG = "Client";
    NsdManager nsdManager;
    NsdServiceInfo nsdServiceInfo;
    ServerSocket serverSocket;
    Socket clientSocket;
    byte[] buffer = new byte[4096];
    Context context;
    String vname;

    long startTime = 0, endTime_buffer = 0, endTime_Total = 0;

    private NsdManager.RegistrationListener resolveListener = new NsdManager.RegistrationListener() {
        @Override
        public void onServiceRegistered(NsdServiceInfo serviceInfo) {
            Log.d(TAG, "Service registered: " + serviceInfo.getServiceName());
            acceptConnection();
        }

        @Override
        public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, "Error code: " + errorCode);
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
            Log.d(TAG, "Service unregistered: " + serviceInfo.getServiceName());
        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, "Error code: " + errorCode);
        }
    };

    public client(Context context, String videoname) {
        nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        this.context = context.getApplicationContext();
        this.vname = videoname;
    }

    public void registerService(int port) {
        try {
            Log.d(TAG, "Entered registerService");
            serverSocket = new ServerSocket(port);
            nsdServiceInfo = new NsdServiceInfo();
            nsdServiceInfo.setServiceType(SERVICE_TYPE);
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int deviceIpAddress = wifiInfo.getIpAddress();
            String formattedIpAddress = String.format("%d.%d.%d.%d", (deviceIpAddress & 0xff), (deviceIpAddress >> 8 & 0xff), (deviceIpAddress >> 16 & 0xff), (deviceIpAddress >> 24 & 0xff));
            nsdServiceInfo.setServiceName(SERVICE_NAME+"/"+formattedIpAddress);
            nsdServiceInfo.setAttribute("Host", formattedIpAddress);
            nsdServiceInfo.setAttribute("Vidname", vname);
            nsdServiceInfo.setPort(port);

            startTime = System.currentTimeMillis();
            nsdManager.registerService(nsdServiceInfo, NsdManager.PROTOCOL_DNS_SD, resolveListener);

        } catch (IOException e) {
            Log.e(TAG, "Error: " + e.toString());
        }
    }

    void acceptConnection() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Waiting for incoming connections...");
                    clientSocket = serverSocket.accept();
                    endTime_buffer = System.currentTimeMillis();

                    //BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    //String code = in.readLine();

                    // Send the video file
                    InputStream inputStream = clientSocket.getInputStream();

                    File outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                    File outputFile = new File(outputDir, vname + ".mp4");
                    //File outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), vname+".mp4");
                    if (outputFile.exists()) {
                        outputFile.delete();
                    }
                    outputFile.createNewFile();

                    OutputStream outputStream = new FileOutputStream(outputFile);
                    int bytesRead, count = 0;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        count++;
                        if(count == 2){
                            Player.storage_exists = 1;
                        }
                    }
                    Player.storage_exists = 1;
                    endTime_Total = System.currentTimeMillis();
                    inputStream.close();
                    outputStream.close();
                    clientSocket.close();
                    serverSocket.close();

                    Log.d(TAG,"Buffer time = "+(endTime_buffer-startTime));
                    Log.d(TAG,"Total time = "+(endTime_Total-startTime));
                    Log.d(TAG,"Transfer time = "+(endTime_Total-endTime_buffer));
                    String text = "Received video to storage from " + clientSocket.getInetAddress();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context.getApplicationContext(), text, Toast.LENGTH_LONG).show();
                        }
                    });
                    Log.d(TAG, "Video received from " + clientSocket.getInetAddress() + ": " + outputFile.getAbsolutePath());

                } catch (IOException e) {
                    Log.e(TAG, "Error: " + e.getMessage());
                } finally {
                    close();
                }
            }
        });
    }

    public void close() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error: " + e.getMessage());
            }
        }
        if (clientSocket != null) {
            try {
                clientSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error: " + e.getMessage());
            }
        }

        if (nsdServiceInfo != null) {
            try {
                nsdManager.unregisterService(resolveListener);
            } catch (Exception e) {
                Log.e(TAG, "Error (unregister): " + e.getMessage());
            }
        }
    }
}
