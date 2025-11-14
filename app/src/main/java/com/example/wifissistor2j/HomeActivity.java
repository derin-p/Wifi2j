package com.example.wifissistor2j;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.model.SpeedTestError;

public class HomeActivity extends AppCompatActivity implements SpeedTestManager.SpeedTestListener {

    private static final String TAG = "HomeActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;
    private ListView wifiListView;
    private BroadcastReceiver wifiScanReceiver;

    private TextView downloadSpeedValueMbpsTextView;
    private TextView downloadSpeedValueMbsTextView;
    private TextView uploadSpeedValueMbpsTextView;
    private TextView uploadSpeedValueMbsTextView;
    private TextView networkStatusTextView;
    private Button analyzeButton;
    private ProgressBar speedTestProgressBar;
    private BottomNavigationView bottomNavigationView;

    private SpeedTestManager speedTestManager;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // --- Find Views ---
        analyzeButton = findViewById(R.id.btn_analyze);
        wifiListView = findViewById(R.id.wifi_list);
        networkStatusTextView = findViewById(R.id.network_status_text);
        bottomNavigationView = findViewById(R.id.bottom_nav);
        downloadSpeedValueMbpsTextView = findViewById(R.id.download_speed_value_mbps);
        downloadSpeedValueMbsTextView = findViewById(R.id.download_speed_value_mbs);
        uploadSpeedValueMbpsTextView = findViewById(R.id.upload_speed_value_mbps);
        uploadSpeedValueMbsTextView = findViewById(R.id.upload_speed_value_mbs);
        speedTestProgressBar = findViewById(R.id.speed_test_progress);

        // --- Initialize Managers ---
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        speedTestManager = new SpeedTestManager();
        sharedPreferences = getSharedPreferences("theme_prefs", MODE_PRIVATE);

        // --- Setup Listeners ---
        analyzeButton.setOnClickListener(v -> runSpeedTest());
        setupNavigation(bottomNavigationView);
        setupWifiScanReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNetworkStatusUI();
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(wifiScanReceiver);
    }

    private void runSpeedTest() {
        if (!analyzeButton.isEnabled()) {
            Toast.makeText(this, "No internet connection.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Reset UI
        downloadSpeedValueMbpsTextView.setText(R.string.speed_value_placeholder);
        downloadSpeedValueMbsTextView.setText(R.string.speed_value_placeholder);
        uploadSpeedValueMbpsTextView.setText(R.string.speed_value_placeholder);
        uploadSpeedValueMbsTextView.setText(R.string.speed_value_placeholder);
        speedTestProgressBar.setIndeterminate(true);
        speedTestProgressBar.setVisibility(View.VISIBLE);

        speedTestManager.startTest(this);
    }

    private void updateSpeedUI(SpeedTestReport report, boolean isDownload) {
        String units = sharedPreferences.getString("speed_units", "Default");
        BigDecimal speedBps = report.getTransferRateBit();

        BigDecimal speedMbps = speedBps.divide(new BigDecimal(1000000), 2, RoundingMode.HALF_UP);
        BigDecimal speedMbs = speedBps.divide(new BigDecimal(8000000), 2, RoundingMode.HALF_UP);

        String formattedMbps = speedMbps.toPlainString() + " Mbps";
        String formattedMbs = speedMbs.toPlainString() + " MB/s";

        TextView mbpsView = isDownload ? downloadSpeedValueMbpsTextView : uploadSpeedValueMbpsTextView;
        TextView mbsView = isDownload ? downloadSpeedValueMbsTextView : uploadSpeedValueMbsTextView;

        switch (units) {
            case "Default":
                mbpsView.setText(formattedMbps);
                mbsView.setText(formattedMbs);
                mbpsView.setVisibility(View.VISIBLE);
                mbsView.setVisibility(View.VISIBLE);
                break;
            case "Mbps":
                mbpsView.setText(formattedMbps);
                mbpsView.setVisibility(View.VISIBLE);
                mbsView.setVisibility(View.GONE);
                break;
            case "MB/s":
                mbsView.setText(formattedMbs);
                mbsView.setVisibility(View.VISIBLE);
                mbpsView.setVisibility(View.GONE);
                break;
        }
    }

    // --- SpeedTestListener Implementation ---

    @Override
    public void onDownloadProgress(float percent, SpeedTestReport report) {
        updateSpeedUI(report, true);
        speedTestProgressBar.setIndeterminate(false);
        speedTestProgressBar.setProgress((int) percent);
    }

    @Override
    public void onDownloadComplete(SpeedTestReport report) {
        updateSpeedUI(report, true);
        speedTestProgressBar.setIndeterminate(true); // Prepare for upload
    }

    @Override
    public void onUploadProgress(float percent, SpeedTestReport report) {
        updateSpeedUI(report, false);
        speedTestProgressBar.setIndeterminate(false);
        speedTestProgressBar.setProgress((int) percent);
    }

    @Override
    public void onUploadComplete(SpeedTestReport report) {
        updateSpeedUI(report, false);
        speedTestProgressBar.setVisibility(View.GONE);
        Toast.makeText(HomeActivity.this, "Test complete!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTestFailed(SpeedTestError error, String message) {
        speedTestProgressBar.setVisibility(View.GONE);
        Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
        Log.e(TAG, message);
    }

    // --- The rest of HomeActivity remains the same ---

    private void setupWifiScanReceiver() {
        wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                    displayScanResults();
                }
            }
        };
    }

    private void setupNavigation(BottomNavigationView bottomNavigationView) {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true; // Already on Home screen
            }
            Intent intent;
            if (itemId == R.id.nav_tools) {
                intent = new Intent(HomeActivity.this, ToolsActivity.class);
            } else if (itemId == R.id.nav_map) {
                intent = new Intent(HomeActivity.this, MapActivity.class);
            } else if (itemId == R.id.nav_settings) {
                intent = new Intent(HomeActivity.this, SettingsActivity.class);
            } else {
                return false;
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            return true;
        });
    }

    private void updateNetworkStatusUI() {
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            networkStatusTextView.setText(R.string.status_not_connected);
            analyzeButton.setText(R.string.analyze_wifi);
            analyzeButton.setEnabled(false);
            wifiListView.setVisibility(View.GONE);
            return;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        if (capabilities == null) {
            networkStatusTextView.setText(R.string.status_not_connected);
            analyzeButton.setEnabled(false);
            return;
        }

        analyzeButton.setEnabled(true);
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            networkStatusTextView.setText(R.string.status_connected_wifi);
            analyzeButton.setText(R.string.analyze_wifi);
            wifiListView.setVisibility(View.VISIBLE);
            checkPermissionsAndScan();
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            networkStatusTextView.setText(R.string.status_connected_mobile);
            analyzeButton.setText(R.string.analyze_mobile_data);
            wifiListView.setVisibility(View.GONE); // Hide Wi-Fi list on mobile data
        } else {
            networkStatusTextView.setText(R.string.status_not_connected);
            analyzeButton.setEnabled(false);
        }
    }

    private void checkPermissionsAndScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startScan();
        }
    }

    private void startScan() {
        if (wifiManager.isWifiEnabled()) {
            wifiManager.startScan();
        }
    }

    private void displayScanResults() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return; // Permission not granted, can't display results
        }
        List<ScanResult> scanResults = wifiManager.getScanResults();
        ArrayList<String> deviceList = new ArrayList<>();
        for (ScanResult scanResult : scanResults) {
            if (scanResult.SSID != null && !scanResult.SSID.isEmpty()) {
                deviceList.add(scanResult.SSID);
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);
        wifiListView.setAdapter(adapter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateNetworkStatusUI(); // Re-check status and scan if on Wi-Fi
            } else {
                Toast.makeText(this, "Location permission is required to scan for Wi-Fi networks.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
