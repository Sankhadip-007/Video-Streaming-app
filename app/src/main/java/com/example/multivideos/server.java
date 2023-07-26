package com.example.multivideos;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class server extends AppCompatActivity {
    private static final String SERVICE_TYPE = "_http._tcp.";
    private static final String TAG = "Server";
    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    private NsdManager.ResolveListener resolveListener;
    private Socket socket;
    private String videoName;
    private long startTime, endTime;
    private Context context;

    static byte[] buffer = new byte[4096];

    List<NsdServiceInfo> services = new ArrayList<>();

    private boolean isResolvingService = false;
    static String path = "";

    OutputStream outputStream;
    InputStream inputStream;


    public server(Context context, String videoName) {
        this.context = context;
        nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        this.videoName = videoName;
    }

    public void discoverServices() {
        Log.d(TAG, "Discovering services");
        startTime = System.currentTimeMillis();
        services = new ArrayList<>();

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
                services.add(serviceInfo);
                resolveServices();
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Service lost: " + serviceInfo.getServiceName());
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Error code (onStartDiscoveryFailed): " + errorCode);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Error code (onStopDiscoveryFailed): " + errorCode);
            }
        };
        services = new ArrayList<>();
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    public void stopDiscovery() {
        try {
            nsdManager.stopServiceDiscovery(discoveryListener);
        }
        catch(Exception e) {}
    }

    public void resolveServices() {
        if (!services.isEmpty() && isResolvingService==false) {
            NsdServiceInfo serviceInfo = services.remove(0);
            resolveService(serviceInfo);
            isResolvingService = true;
        }
    }
    public void resolveService(NsdServiceInfo serviceInfo) {
        NsdManager.ResolveListener resolveListener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Error code (onResolveFailed): " + errorCode+"..."+serviceInfo.getServiceName());
                isResolvingService = false;
                resolveServices();
            }
            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                System.out.println("On service resolved");
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                int deviceIpAddress = wifiInfo.getIpAddress();
                String formattedIpAddress = String.format("%d.%d.%d.%d", (deviceIpAddress & 0xff),
                        (deviceIpAddress >> 8 & 0xff), (deviceIpAddress >> 16 & 0xff), (deviceIpAddress >> 24 & 0xff));
                String hostIpAddress = serviceInfo.getHost().getHostAddress();
                Log.d(TAG, "Service resolved: " + serviceInfo.getServiceName());
                Log.d(TAG, "Host: " + hostIpAddress);

                if (!formattedIpAddress.equals(hostIpAddress)) {
                    boolean foundVideo = false;
                    byte[] valueBytes = serviceInfo.getAttributes().get("Vidname");
                    String value = new String(valueBytes);

                    File cacheDir = context.getCacheDir();
                    File[] cacheFiles = cacheDir.listFiles();
                    if (cacheFiles != null) {
                        for (File cacheFile : cacheFiles) {
                            if (cacheFile.isFile()) {
                                if(cacheFile.getName().equals(value)){
                                    foundVideo = true;
                                    path = cacheFile.getAbsolutePath();
                                    break;
                                }
                            }
                        }
                    }
                    if (foundVideo) {
                        stopDiscovery();
                        sendVideo(serviceInfo, path);
                    }
                    else {
                        isResolvingService = false;
                        resolveServices();
                    }
                } else {
                    Log.e(TAG, "Cannot resolve own service");
                    onResolveFailed(serviceInfo, 4);
                    isResolvingService = false;
                    resolveServices();
                }
            }
        };

        NsdManager nsdM = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        nsdM.resolveService(serviceInfo, resolveListener);
    }

    public void sendVideo(NsdServiceInfo serviceInfo, String path) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    InetAddress host = serviceInfo.getHost();
                    int port = serviceInfo.getPort();

                    // Connect to the server
                    socket = new Socket(host, port);
                    endTime = System.currentTimeMillis();

                    // Receive the video
                    outputStream = socket.getOutputStream();
                    File file = new File(path);
                    inputStream = new BufferedInputStream(new FileInputStream(file));
                    int bytesRead;

                    System.out.println("Sending video...");
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    Log.d(TAG, "Sent video");
                    outputStream.close();
                    inputStream.close();
                    socket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error: " + e.getMessage()+e);
                } finally {
                    try {
                        if (outputStream != null) {
                            outputStream.close();
                        }
                    } catch (IOException e) {}
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    } catch (IOException e) {}
                    try{
                        if (socket != null) {
                            socket.close();
                        }
                    } catch (IOException e) {}
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            // Create a new instance of Server and call discoverServices()
                            server server = new server(context, "");
                            server.discoverServices();
                        }
                    });
                }
            }
        });
    }
}
