package com.example.wifissistor2j;

public interface SignalStrengthListener {
    void onSignalStrengthChanged(int rssi);
    void onWifiUnavailable();
}
