package com.example.multivideos;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

import java.net.InetAddress;

public class NsdHelper {
    private static final String SERVICE_TYPE = "_http._tcp.";
    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    private NsdManager.ResolveListener resolveListener;

    public interface NsdListener {
        void onDiscoveryStarted();
        void onDiscoveryStopped();
        void onServiceFound(NsdServiceInfo serviceInfo);
        void onServiceLost(NsdServiceInfo serviceInfo);
        void onServiceResolved(NsdServiceInfo serviceInfo);
    }

    public NsdHelper(Context context, final NsdListener listener) {
        nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String s, int i) {

            }

            @Override
            public void onStopDiscoveryFailed(String s, int i) {

            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                listener.onDiscoveryStarted();
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                listener.onDiscoveryStopped();
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                NsdManager.ResolveListener resolveListener = new NsdManager.ResolveListener() {
                    @Override
                    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                        // resolve failed
                    }
                    @Override
                    public void onServiceResolved(NsdServiceInfo serviceInfo) {
                        // service resolved, connect to service
                        InetAddress host = serviceInfo.getHost();
                        int port = serviceInfo.getPort();
                        // create socket connection to host and port
                    }
                };
                nsdManager.resolveService(serviceInfo, resolveListener);

                listener.onServiceFound(serviceInfo);
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                listener.onServiceLost(serviceInfo);
            }

        };

/*        resolveListener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // resolve failed
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                listener.onServiceResolved(serviceInfo);
            }
        };*/
    }

    public void discoverServices() {
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    public void stopDiscovery() {
        nsdManager.stopServiceDiscovery(discoveryListener);
    }

    public void resolveService(NsdServiceInfo serviceInfo) {
        nsdManager.resolveService(serviceInfo, resolveListener);
    }
}

