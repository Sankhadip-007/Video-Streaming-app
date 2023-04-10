package com.example.multivideos;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Environment;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NsdServer extends AppCompatActivity {
    private static final String SERVICE_TYPE = "_http._tcp.";
    private static final String SERVICE_NAME = "multivideos";
    private static final String TAG = "NsdServer";

    private NsdManager nsdManager;
    private NsdServiceInfo nsdServiceInfo;
    private ServerSocket serverSocket;
    private Socket clientSocket;

    private String videoName;

    public NsdServer(Context context, String videoName) {
        nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        this.videoName = videoName;
    }

    public void registerService(int port) {
        try {
            serverSocket = new ServerSocket(port);
            nsdServiceInfo = new NsdServiceInfo();
            nsdServiceInfo.setServiceType(SERVICE_TYPE);
            nsdServiceInfo.setServiceName(SERVICE_NAME);
            nsdServiceInfo.setPort(port);

            nsdManager.registerService(nsdServiceInfo, NsdManager.PROTOCOL_DNS_SD, new NsdManager.RegistrationListener() {
                @Override
                public void onServiceRegistered(NsdServiceInfo serviceInfo) {
                    Log.d(TAG, "Service registered: " + serviceInfo.getServiceName());
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
            });
        } catch (IOException e) {
            Log.e(TAG, "Error: " + e.getMessage());
        }
    }

    public void acceptConnections() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                if (clientSocket == null) {
                    Log.d(TAG, "Client disconnectedd");
                }
                try {
                    Log.d(TAG, "Waiting for incoming connections...");
                    clientSocket = serverSocket.accept();
                    Log.d(TAG, "Client connected: " + clientSocket.getInetAddress().getHostAddress());
                    InputStream inputStream = clientSocket.getInputStream();
                    File outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), videoName);
                    OutputStream outputStream = new FileOutputStream(outputFile);
                    byte[] buffer = new byte[1024 * 1024];
                    int bytesRead, count=0;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        Log.d(TAG, "Bytes read = "+(++count));
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    inputStream.close();
                    outputStream.close();
                    clientSocket.close();
                    serverSocket.close();
                    Log.d(TAG, "Video received: " + outputFile.getAbsolutePath());
                } catch (IOException e) {
                    Log.e(TAG, "Error: " + e.getMessage());
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
            nsdManager.unregisterService(resolveListener);
        }

    }
    private NsdManager.RegistrationListener resolveListener = new NsdManager.RegistrationListener() {
        @Override
        public void onServiceRegistered(NsdServiceInfo serviceInfo) {
            Log.d(TAG, "Service registered: " + serviceInfo.getServiceName());
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



    /*public File getFile(){
        return outputFile;
    }*/
}