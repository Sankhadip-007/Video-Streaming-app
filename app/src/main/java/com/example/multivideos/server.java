package com.example.multivideos;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Environment;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class server extends AppCompatActivity {
    private static final String SERVICE_TYPE = "_http._tcp.";
    private static final String TAG = "NsdClient";
    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    private NsdManager.ResolveListener resolveListener;
    private Socket socket;
    private String videoName;

    public server(Context context,String videoName) {
        nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        this.videoName=videoName;
    }

    public void discoverServices() {
        Log.d(TAG, "Discovery services");
        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String serviceType) {
                Log.d(TAG, "Discovery started");
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.d(TAG, "Discovery stopped");
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Service found: " + serviceInfo.getServiceName());
                //nsdManager.resolveService(serviceInfo, resolveListener);
                resolveService(serviceInfo);
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Service lost: " + serviceInfo.getServiceName());
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Error code: " + errorCode);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Error code: " + errorCode);
            }
        };

        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    public void stopDiscovery() {
        nsdManager.stopServiceDiscovery(discoveryListener);
    }

    public void resolveService(NsdServiceInfo serviceInfo) {
        resolveListener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Error code: " + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                stopDiscovery();
                saveVideo(serviceInfo);
            }
        };

        nsdManager.resolveService(serviceInfo, resolveListener);
    }

    public void close() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket: " + e.getMessage());
            }
        }
        if (resolveListener != null) {
            nsdManager.stopServiceDiscovery(discoveryListener);
            //nsdManager.unregisterService(resolveListener);
            resolveListener = null;
        }
        if (discoveryListener != null) {
            nsdManager.stopServiceDiscovery(discoveryListener);
            discoveryListener = null;
        }
    }

    public void saveVideo(NsdServiceInfo serviceInfo) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                if (socket == null) {
                    Log.d(TAG, "Client disconnectedd");
                }
                try {
                    Log.d(TAG, "Waiting for incoming connections...");
                    InetAddress host = InetAddress.getByName(serviceInfo.getHost().getHostAddress());
                    int port = serviceInfo.getPort();

                    // connect to the server
                    socket = new Socket(host, port);
                    // receive the video
                    InputStream inputStream = socket.getInputStream();
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
                    socket.close();
                    Log.d(TAG, "Video received: " + outputFile.getAbsolutePath());
                } catch (IOException e) {
                    Log.e(TAG, "Error: " + e.getMessage());
                }
            }
        });
    }

}