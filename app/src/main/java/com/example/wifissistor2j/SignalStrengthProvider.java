package com.example.wifissistor2j;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;

public class SignalStrengthProvider {

    private static final int POLLING_INTERVAL_MS = 1000;

    private final WifiManager wifiManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SignalStrengthListener listener;
    private final Runnable pollingRunnable;

    public SignalStrengthProvider(Context context, SignalStrengthListener listener) {
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.listener = listener;

        this.pollingRunnable = new Runnable() {
            @Override
            public void run() {
                pollSignalStrength();
                handler.postDelayed(this, POLLING_INTERVAL_MS);
            }
        };
    }

    public void start() {
        handler.post(pollingRunnable);
    }

    public void stop() {
        handler.removeCallbacks(pollingRunnable);
    }

    private void pollSignalStrength() {
        if (wifiManager == null || !wifiManager.isWifiEnabled()) {
            listener.onWifiUnavailable();
            return;
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null && wifiInfo.getNetworkId() != -1) {
            listener.onSignalStrengthChanged(wifiInfo.getRssi());
        } else {
            listener.onWifiUnavailable();
        }
    }
}
