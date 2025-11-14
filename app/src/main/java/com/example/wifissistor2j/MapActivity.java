package com.example.wifissistor2j;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MapActivity extends AppCompatActivity implements SignalStrengthListener {

    private static final int RSSI_MAX = -30;
    private static final int RSSI_MIN = -100;

    private BottomNavigationView bottomNavigationView;
    private TextView signalStrengthValueText;
    private TextView signalStrengthGameText;
    private ProgressBar signalStrengthProgressBar;
    private Button radarButton;

    private SignalStrengthProvider signalStrengthProvider;
    private boolean isRadarRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // --- Initialize Provider ---
        signalStrengthProvider = new SignalStrengthProvider(this, this);

        // --- Find Views ---
        bottomNavigationView = findViewById(R.id.bottom_nav);
        signalStrengthValueText = findViewById(R.id.signal_strength_value_text);
        signalStrengthGameText = findViewById(R.id.signal_strength_game_text);
        signalStrengthProgressBar = findViewById(R.id.signal_strength_progress_bar);
        radarButton = findViewById(R.id.radar_button);

        setupNavigation();

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
        signalStrengthProvider.start();
    }

    private void stopRadar() {
        isRadarRunning = false;
        radarButton.setText(R.string.start);
        signalStrengthProvider.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isRadarRunning) {
            startRadar();
        }
        bottomNavigationView.setSelectedItemId(R.id.nav_map);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop the provider when the activity is not visible to save battery
        stopRadar();
    }

    @Override
    public void onSignalStrengthChanged(int rssi) {
        signalStrengthValueText.setText(getString(R.string.signal_strength_dbm, rssi));

        int progress = calculateProgress(rssi);
        signalStrengthProgressBar.setProgress(progress);

        int color = getSignalColor(progress);
        signalStrengthValueText.setTextColor(color);

        String strengthText = SignalStrengthMapper.getSignalStrengthAsDistance(rssi);
        signalStrengthGameText.setText(strengthText);
        signalStrengthGameText.setTextColor(color);
    }

    @Override
    public void onWifiUnavailable() {
        signalStrengthValueText.setText(R.string.not_connected);
        signalStrengthGameText.setText(R.string.connect_to_wifi_to_play);
        signalStrengthProgressBar.setProgress(0);
        int defaultColor = ContextCompat.getColor(this, R.color.default_text_color);
        signalStrengthValueText.setTextColor(defaultColor);
        signalStrengthGameText.setTextColor(defaultColor);
    }

    private int calculateProgress(int rssi) {
        int clampedRssi = Math.max(RSSI_MIN, Math.min(rssi, RSSI_MAX));
        // Normalize the value to a 0-100 range for the progress bar
        return (int) (((double) (clampedRssi - RSSI_MIN) / (RSSI_MAX - RSSI_MIN)) * 100);
    }

    private int getSignalColor(int progress) {
        float percentage = progress / 100.0f;
        // Blue (cold) to Red (hot) interpolation
        int red = (int) (255 * percentage);
        int blue = (int) (255 * (1 - percentage));
        return Color.rgb(red, 0, blue);
    }

    private void setupNavigation() {
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
    }
}
