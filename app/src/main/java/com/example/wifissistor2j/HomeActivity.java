package com.example.wifissistor2j;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;
    private ListView wifiListView;
    private BroadcastReceiver wifiScanReceiver;

    private TextView downloadSpeedValueTextView;
    private TextView uploadSpeedValueTextView;
    private TextView networkStatusTextView;
    private Button analyzeButton;
    private ProgressBar speedTestProgressBar;

    private SpeedTestSocket speedTestSocket;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final List<SpeedTestServer> servers = Arrays.asList(
            new SpeedTestServer("OVH", "http://proof.ovh.net/files/10Mio.dat", "http://proof.ovh.net/", 1000000),
            new SpeedTestServer("Tele2", "http://speedtest.tele2.net/10MB.zip", "http://speedtest.tele2.net/", 10000000),
            new SpeedTestServer("Hetzner", "http://speed.hetzner.de/100MB.bin", "http://speed.hetzner.de/", 10000000)
    );
    private final Random random = new Random();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // --- Find Views ---
        analyzeButton = findViewById(R.id.btn_analyze);
        wifiListView = findViewById(R.id.wifi_list);
        networkStatusTextView = findViewById(R.id.network_status_text);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_nav);
        downloadSpeedValueTextView = findViewById(R.id.download_speed_value);
        uploadSpeedValueTextView = findViewById(R.id.upload_speed_value);
        speedTestProgressBar = findViewById(R.id.speed_test_progress);

        // --- Initialize Managers ---
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        speedTestSocket = new SpeedTestSocket();

        // --- Setup Listeners ---
        analyzeButton.setOnClickListener(v -> runSpeedTest());
        setupNavigation(bottomNavigationView);
        setupWifiScanReceiver();

        // --- Initial UI Update ---
        updateNetworkStatusUI();
    }

    private void runSpeedTest() {
        updateNetworkStatusUI();

        if (!analyzeButton.isEnabled()) {
            Toast.makeText(this, "No internet connection.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Reset UI
        downloadSpeedValueTextView.setText(R.string.speed_value_placeholder);
        uploadSpeedValueTextView.setText(R.string.speed_value_placeholder);
        speedTestProgressBar.setIndeterminate(true);
        speedTestProgressBar.setVisibility(View.VISIBLE);

        SpeedTestServer server = getRandomServer();
        Toast.makeText(this, "Starting speed test on " + server.getName() + "...", Toast.LENGTH_SHORT).show();

        startDownloadTest(server);
    }

    private void startDownloadTest(SpeedTestServer server) {
        new Thread(() -> {
            speedTestSocket.clearListeners();
            speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {
                @Override
                public void onCompletion(SpeedTestReport report) {
                    handler.post(() -> {
                        updateSpeedUI(report, downloadSpeedValueTextView);
                        // Download is complete, now start the upload test
                        startUploadTest(server);
                    });
                }

                @Override
                public void onProgress(float percent, SpeedTestReport report) {
                    handler.post(() -> {
                        updateSpeedUI(report, downloadSpeedValueTextView);
                        speedTestProgressBar.setIndeterminate(false);
                        speedTestProgressBar.setProgress((int) percent);
                    });
                }

                @Override
                public void onError(SpeedTestError speedTestError, String errorMessage) {
                    handler.post(() -> {
                        Log.e(TAG, "Download error: " + errorMessage);
                        speedTestProgressBar.setVisibility(View.GONE);
                        Toast.makeText(HomeActivity.this, "Download failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                    });
                }
            });
            speedTestSocket.startDownload(server.getDownloadUrl());
        }).start();
    }

    private void startUploadTest(SpeedTestServer server) {
        new Thread(() -> {
            speedTestSocket.clearListeners();
            speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {
                @Override
                public void onCompletion(SpeedTestReport report) {
                    handler.post(() -> {
                        updateSpeedUI(report, uploadSpeedValueTextView);
                        speedTestProgressBar.setVisibility(View.GONE);
                        Toast.makeText(HomeActivity.this, "Test complete!", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onProgress(float percent, SpeedTestReport report) {
                    handler.post(() -> {
                        updateSpeedUI(report, uploadSpeedValueTextView);
                        speedTestProgressBar.setIndeterminate(false);
                        speedTestProgressBar.setProgress((int) percent);
                    });
                }

                @Override
                public void onError(SpeedTestError speedTestError, String errorMessage) {
                    handler.post(() -> {
                        speedTestProgressBar.setVisibility(View.GONE);
                        Toast.makeText(HomeActivity.this, "Upload failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Upload error: " + errorMessage);
                    });
                }
            });
            speedTestSocket.startUpload(server.getUploadUrl(), server.getUploadSize());
        }).start();
    }

    private void updateSpeedUI(SpeedTestReport report, TextView speedValueTextView) {
        BigDecimal speedBps = report.getTransferRateBit();
        BigDecimal speedMbps = speedBps.divide(new BigDecimal(1000000), 2, RoundingMode.HALF_UP);
        speedValueTextView.setText(speedMbps.toPlainString());
    }

    private SpeedTestServer getRandomServer() {
        return servers.get(random.nextInt(servers.size()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNetworkStatusUI();
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(wifiScanReceiver);
    }

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

    // Helper class for Speed Test Server
    private static class SpeedTestServer {
        private final String name;
        private final String downloadUrl;
        private final String uploadUrl;
        private final int uploadSize;

        public SpeedTestServer(String name, String downloadUrl, String uploadUrl, int uploadSize) {
            this.name = name;
            this.downloadUrl = downloadUrl;
            this.uploadUrl = uploadUrl;
            this.uploadSize = uploadSize;
        }

        public String getName() {
            return name;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public String getUploadUrl() {
            return uploadUrl;
        }

        public int getUploadSize() {
            return uploadSize;
        }
    }
}
