package com.example.wifissistor2j;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MapActivity extends AppCompatActivity {

    private static final int RSSI_MAX = -30;
    private static final int RSSI_MIN = -100;
    private static final int HOT_THRESHOLD = -50;
    private static final int WARM_THRESHOLD = -60;
    private static final int TEPID_THRESHOLD = -70;
    private static final int COLD_THRESHOLD = -80;

    private BottomNavigationView bottomNavigationView;
    private TextView signalStrengthValueText;
    private TextView signalStrengthGameText;
    private ProgressBar signalStrengthProgressBar;
    private Button radarButton;

    private WifiManager wifiManager;
    private final Handler signalStrengthHandler = new Handler(Looper.getMainLooper());
    private Runnable signalStrengthRunnable;
    private boolean isRadarRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // --- Find Views ---
        bottomNavigationView = findViewById(R.id.bottom_nav);
        signalStrengthValueText = findViewById(R.id.signal_strength_value_text);
        signalStrengthGameText = findViewById(R.id.signal_strength_game_text);
        signalStrengthProgressBar = findViewById(R.id.signal_strength_progress_bar);
        radarButton = findViewById(R.id.radar_button);

        // --- Setup Navigation ---
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_map) {
                return true;
            }
            Intent intent = null;
            if (itemId == R.id.nav_home) {
                intent = new Intent(this, HomeActivity.class);
            } else if (itemId == R.id.nav_tools) {
                intent = new Intent(this, ToolsActivity.class);
            } else if (itemId == R.id.nav_settings) {
                intent = new Intent(this, SettingsActivity.class);
            }
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            }
            return true;
        });

        // --- Initialize Signal Finder ---
        setupSignalStrengthGame();

        radarButton.setOnClickListener(v -> {
            if (isRadarRunning) {
                stopRadar();
            } else {
                startRadar();
            }
        });
    }

    private void startRadar() {
        isRadarRunning = true;
        radarButton.setText(R.string.stop);
        signalStrengthHandler.post(signalStrengthRunnable);
    }

    private void stopRadar() {
        isRadarRunning = false;
        radarButton.setText(R.string.start);
        signalStrengthHandler.removeCallbacks(signalStrengthRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isRadarRunning) {
            startRadar();
        }
        // Set the correct navigation item when returning to the activity
        bottomNavigationView.setSelectedItemId(R.id.nav_map);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRadar();
    }

    private void setupSignalStrengthGame() {
        signalStrengthRunnable = new Runnable() {
            @Override
            public void run() {
                displaySignalStrength();
                // Schedule the next update in 1 second
                signalStrengthHandler.postDelayed(this, 1000);
            }
        };
    }

    private void displaySignalStrength() {
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                int rssi = wifiInfo.getRssi();
                signalStrengthValueText.setText(getString(R.string.signal_strength_dbm, rssi));

                int progress = calculateProgress(rssi);
                signalStrengthProgressBar.setProgress(progress);

                int color = getSignalColor(rssi);
                signalStrengthValueText.setTextColor(color);
                signalStrengthGameText.setTextColor(color);

                if (rssi > HOT_THRESHOLD) {
                    signalStrengthGameText.setText(R.string.signal_strength_super_hot);
                } else if (rssi > WARM_THRESHOLD) {
                    signalStrengthGameText.setText(R.string.signal_strength_hotter);
                } else if (rssi > TEPID_THRESHOLD) {
                    signalStrengthGameText.setText(R.string.signal_strength_warm);
                } else if (rssi > COLD_THRESHOLD) {
                    signalStrengthGameText.setText(R.string.signal_strength_colder);
                } else {
                    signalStrengthGameText.setText(R.string.signal_strength_freezing);
                }

            } else {
                signalStrengthValueText.setText(R.string.not_connected);
                signalStrengthGameText.setText(R.string.connect_to_wifi_to_play);
                signalStrengthProgressBar.setProgress(0);
                signalStrengthValueText.setTextColor(ContextCompat.getColor(this, R.color.default_text_color));
                signalStrengthGameText.setTextColor(ContextCompat.getColor(this, R.color.default_text_color));
            }
        } else {
            signalStrengthValueText.setText(R.string.wifi_not_available);
            signalStrengthGameText.setText(R.string.enable_wifi_to_play);
            signalStrengthProgressBar.setProgress(0);
            signalStrengthValueText.setTextColor(ContextCompat.getColor(this, R.color.default_text_color));
            signalStrengthGameText.setTextColor(ContextCompat.getColor(this, R.color.default_text_color));
        }
    }

    private int calculateProgress(int rssi) {
        // Clamp the RSSI value between RSSI_MIN and RSSI_MAX
        int clampedRssi = Math.max(RSSI_MIN, Math.min(rssi, RSSI_MAX));
        // Normalize the value to a 0-70 range for the progress bar
        return (clampedRssi - RSSI_MIN);
    }

    private int getSignalColor(int rssi) {
        float percentage = (float) (calculateProgress(rssi)) / (RSSI_MAX - RSSI_MIN);
        // Clamp percentage between 0 and 1
        percentage = Math.max(0f, Math.min(percentage, 1f));
        
        // Blue (cold) to Red (hot) interpolation
        int red = (int) (255 * percentage);
        int blue = (int) (255 * (1 - percentage));
        return Color.rgb(red, 0, blue);
    }
}
