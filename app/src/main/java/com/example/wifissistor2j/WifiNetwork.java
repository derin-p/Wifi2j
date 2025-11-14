package com.example.wifissistor2j;

import androidx.annotation.NonNull;

public class WifiNetwork {
    private final String ssid;
    private final String signalStrength;

    public WifiNetwork(String ssid, String signalStrength) {
        this.ssid = ssid;
        this.signalStrength = signalStrength;
    }

    // Override toString() so the ArrayAdapter can display the information correctly.
    @NonNull
    @Override
    public String toString() {
        return ssid + "\t\t" + signalStrength;
    }
}
