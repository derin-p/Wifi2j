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
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity implements SpeedTester.SpeedTestListener {

    private static final String TAG = "HomeActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private boolean isTestRunning = false;

    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;
    private ListView wifiListView;
    private BroadcastReceiver wifiScanReceiver;

    private TextView downloadSpeedValueMbpsTextView;
    private TextView downloadSpeedValueMbsTextView;
    private TextView downloadSpeedQualityTextView;
    private TextView uploadSpeedValueMbpsTextView;
    private TextView uploadSpeedValueMbsTextView;
    private TextView uploadSpeedQualityTextView;
    private TextView networkStatusTextView;
    private Button analyzeButton;
    private ProgressBar speedTestProgressBar;
    private BottomNavigationView bottomNavigationView;
    private MaterialCardView suggestionCard;

    private SpeedTestManager speedTestManager;
    private SharedPreferences sharedPreferences;
    private double lastDownloadSpeedMbps = 0;

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
        downloadSpeedQualityTextView = findViewById(R.id.download_speed_quality_text);
        uploadSpeedValueMbpsTextView = findViewById(R.id.upload_speed_value_mbps);
        uploadSpeedValueMbsTextView = findViewById(R.id.upload_speed_value_mbs);
        uploadSpeedQualityTextView = findViewById(R.id.upload_speed_quality_text);
        speedTestProgressBar = findViewById(R.id.speed_test_progress);
        suggestionCard = findViewById(R.id.suggestion_card);
        TextView suggestionTextView = findViewById(R.id.suggestion_text);
        ImageButton suggestionCloseButton = findViewById(R.id.suggestion_close_button);

        // --- Initialize Managers ---
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        speedTestManager = SpeedTestManager.getInstance(this);
        sharedPreferences = getSharedPreferences("theme_prefs", MODE_PRIVATE);

        // --- Setup Listeners ---
        analyzeButton.setOnClickListener(v -> {
            if (isTestRunning) {
                stopSpeedTest();
            } else {
                startSpeedTest();
            }
        });

        suggestionTextView.setText(R.string.suggestion_weak_signal);
        suggestionCloseButton.setOnClickListener(v -> suggestionCard.setVisibility(View.GONE));

        setupNavigation();
        setupWifiScanReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        speedTestManager.setListener(this);
        updateNetworkStatusUI();
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    @Override
    protected void onPause() {
        super.onPause();
        speedTestManager.removeListener();
        unregisterReceiver(wifiScanReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // No need to call shutdown on a singleton manager from an activity's onDestroy
    }

    private void startSpeedTest() {
        if (!analyzeButton.isEnabled()) {
            Toast.makeText(this, R.string.home_no_internet, Toast.LENGTH_SHORT).show();
            return;
        }
        isTestRunning = true;
        analyzeButton.setText(R.string.home_stop_analysis);

        // Reset UI
        suggestionCard.setVisibility(View.GONE);
        downloadSpeedValueMbpsTextView.setText(R.string.speed_value_placeholder);
        downloadSpeedValueMbsTextView.setText(R.string.speed_value_placeholder);
        downloadSpeedQualityTextView.setText("");
        uploadSpeedValueMbpsTextView.setText(R.string.speed_value_placeholder);
        uploadSpeedValueMbsTextView.setText(R.string.speed_value_placeholder);
        uploadSpeedQualityTextView.setText("");
        speedTestProgressBar.setIndeterminate(true);
        speedTestProgressBar.setVisibility(View.VISIBLE);

        speedTestManager.startTest();
    }

    private void stopSpeedTest() {
        speedTestManager.stopTest();
    }

    private void resetTestUI(String buttonText) {
        isTestRunning = false;
        analyzeButton.setText(buttonText);
        speedTestProgressBar.setVisibility(View.GONE);
    }

    private void updateSpeedUI(GenericSpeedTestReport report, boolean isDownload) {
        String units = sharedPreferences.getString("speed_units", "Default");
        BigDecimal speedBps = BigDecimal.valueOf(report.getTransferRateBit());
        BigDecimal speedMbps = speedBps.divide(new BigDecimal(1000000), 2, RoundingMode.HALF_UP);
        BigDecimal speedMbs = speedBps.divide(new BigDecimal(8000000), 2, RoundingMode.HALF_UP);

        String formattedMbps = getString(R.string.speed_format_mbps, speedMbps);
        String formattedMbs = getString(R.string.speed_format_mbs, speedMbs);

        String quality = SpeedResultInterpreter.interpret(speedMbps.doubleValue());

        TextView mbpsView, mbsView, qualityView;

        if (isDownload) {
            mbpsView = downloadSpeedValueMbpsTextView;
            mbsView = downloadSpeedValueMbsTextView;
            qualityView = downloadSpeedQualityTextView;
            lastDownloadSpeedMbps = speedMbps.doubleValue(); // Store for suggestion logic
        } else {
            mbpsView = uploadSpeedValueMbpsTextView;
            mbsView = uploadSpeedValueMbsTextView;
            qualityView = uploadSpeedQualityTextView;
        }

        qualityView.setText(quality);

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

    private void checkSignalAndSuggest() {
        if (!wifiManager.isWifiEnabled()) {
            return;
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            int rssi = wifiInfo.getRssi();
            String signalQuality = SignalStrengthMapper.getSignalStrength(rssi);
            String speedQuality = SpeedResultInterpreter.interpret(lastDownloadSpeedMbps);

            boolean isSignalPoor = signalQuality.equals("Fair") || signalQuality.equals("Weak");
            boolean isSpeedPoor = speedQuality.equals("Fair") || speedQuality.equals("Poor");

            if (isSignalPoor && isSpeedPoor) {
                suggestionCard.setVisibility(View.VISIBLE);
            } else {
                suggestionCard.setVisibility(View.GONE);
            }
        }
    }

    // --- SpeedTestListener Implementation ---

    @Override
    public void onDownloadProgress(float percent, GenericSpeedTestReport report) {
        updateSpeedUI(report, true);
        speedTestProgressBar.setIndeterminate(false);
        speedTestProgressBar.setProgress((int) percent);
    }

    @Override
    public void onDownloadComplete(GenericSpeedTestReport report) {
        updateSpeedUI(report, true);
        speedTestProgressBar.setIndeterminate(true);
    }

    @Override
    public void onUploadProgress(float percent, GenericSpeedTestReport report) {
        updateSpeedUI(report, false);
        speedTestProgressBar.setIndeterminate(false);
        speedTestProgressBar.setProgress((int) percent);
    }

    @Override
    public void onUploadComplete(GenericSpeedTestReport report) {
        updateSpeedUI(report, false);
        checkSignalAndSuggest(); // Check conditions and show suggestion if needed
        // We don't reset the UI here because the test sequence may continue
    }

    @Override
    public void onTestFailed(GenericSpeedTestError error, String message) {
        resetTestUI(getString(R.string.home_analyze_again));
        Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
        Log.e(TAG, message);
    }

    @Override
    public void onTestCancelled() {
        resetTestUI(getString(R.string.analyze_wifi)); // Reset to the default state
        Toast.makeText(this, R.string.home_test_cancelled, Toast.LENGTH_SHORT).show();
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

    private void setupNavigation() {
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
            suggestionCard.setVisibility(View.GONE);
            return;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        if (capabilities == null) {
            networkStatusTextView.setText(R.string.status_not_connected);
            analyzeButton.setEnabled(false);
            suggestionCard.setVisibility(View.GONE);
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
            wifiListView.setVisibility(View.GONE);
            suggestionCard.setVisibility(View.GONE);
        } else {
            networkStatusTextView.setText(R.string.status_not_connected);
            analyzeButton.setEnabled(false);
            suggestionCard.setVisibility(View.GONE);
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
            return; // Permission not granted, can\'t display results
        }
        List<ScanResult> scanResults = wifiManager.getScanResults();
        List<WifiNetwork> networkList = new ArrayList<>();
        for (ScanResult scanResult : scanResults) {
            if (scanResult.SSID != null && !scanResult.SSID.isEmpty()) {
                String strength = SignalStrengthMapper.getSignalStrength(scanResult.level);
                networkList.add(new WifiNetwork(scanResult.SSID, strength));
            }
        }
        ArrayAdapter<WifiNetwork> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, networkList);
        wifiListView.setAdapter(adapter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateNetworkStatusUI(); // Re-check status and scan if on Wi-Fi
            } else {
                Toast.makeText(this, R.string.home_permission_required, Toast.LENGTH_LONG).show();
            }
        }
    }
}
