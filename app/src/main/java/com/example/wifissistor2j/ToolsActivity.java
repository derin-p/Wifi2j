package com.example.wifissistor2j;

import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ToolsActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    // Network Info Views
    private TextView ipAddressText;
    private TextView gatewayText;
    private TextView subnetMaskText;
    private TextView dnsServerText;

    // Ping Tool Views
    private EditText pingEditText;
    private Button pingButton;
    private TextView pingResultText;

    // Signal Strength Views
    private TextView signalStrengthText;
    private TextView signalStrengthDescriptionText;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WifiManager wifiManager;

    private final Handler signalStrengthHandler = new Handler(Looper.getMainLooper());
    private Runnable signalStrengthRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tools);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // --- Find Views ---
        bottomNavigationView = findViewById(R.id.bottom_nav);
        ipAddressText = findViewById(R.id.ip_address_text);
        gatewayText = findViewById(R.id.gateway_text);
        subnetMaskText = findViewById(R.id.subnet_mask_text);
        dnsServerText = findViewById(R.id.dns_server_text);
        pingEditText = findViewById(R.id.ping_edit_text);
        pingButton = findViewById(R.id.ping_button);
        pingResultText = findViewById(R.id.ping_result_text);
        signalStrengthText = findViewById(R.id.signal_strength_text);
        signalStrengthDescriptionText = findViewById(R.id.signal_strength_description_text);

        // --- Setup Navigation ---
        bottomNavigationView.setSelectedItemId(R.id.nav_tools);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(ToolsActivity.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_tools) {
                return true;
            } else if (itemId == R.id.nav_map) {
                startActivity(new Intent(ToolsActivity.this, MapActivity.class));
                return true;
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(ToolsActivity.this, SettingsActivity.class));
                return true;
            }
            return false;
        });

        // --- Initialize Tools ---
        displayWifiInfo();
        setupPingTool();
        setupSignalStrengthTool();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Start updating signal strength when the activity is resumed
        signalStrengthHandler.post(signalStrengthRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop updating signal strength when the activity is paused
        signalStrengthHandler.removeCallbacks(signalStrengthRunnable);
    }

    private void displayWifiInfo() {
        if (wifiManager == null) {
            ipAddressText.setText("Wi-Fi not available");
            gatewayText.setText("Wi-Fi not available");
            subnetMaskText.setText("Wi-Fi not available");
            dnsServerText.setText("Wi-Fi not available");
            return;
        }

        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        if (dhcpInfo == null) {
            ipAddressText.setText("Not connected to Wi-Fi");
            gatewayText.setText("Not connected to Wi-Fi");
            subnetMaskText.setText("Not connected to Wi-Fi");
            dnsServerText.setText("Not connected to Wi-Fi");
            return;
        }

        ipAddressText.setText(intToIp(dhcpInfo.ipAddress));
        gatewayText.setText(intToIp(dhcpInfo.gateway));
        subnetMaskText.setText(intToIp(dhcpInfo.netmask));
        dnsServerText.setText(intToIp(dhcpInfo.dns1));
    }

    private void setupPingTool() {
        pingButton.setOnClickListener(v -> {
            String host = pingEditText.getText().toString();
            if (host.isEmpty()) {
                Toast.makeText(this, "Please enter a website to ping", Toast.LENGTH_SHORT).show();
                return;
            }
            runPingTest(host);
        });
    }

    private void runPingTest(String host) {
        pingResultText.setText("Pinging " + host + "...");
        executorService.execute(() -> {
            try {
                long startTime = System.currentTimeMillis();
                InetAddress inetAddress = InetAddress.getByName(host);
                boolean isReachable = inetAddress.isReachable(5000); // 5 second timeout
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                handler.post(() -> {
                    if (isReachable) {
                        pingResultText.setText("Success from " + host + " in " + duration + "ms");
                    } else {
                        pingResultText.setText("Failed: Host not reachable");
                    }
                });

            } catch (IOException e) {
                handler.post(() -> pingResultText.setText("Error: " + e.getMessage()));
            }
        });
    }

    private void setupSignalStrengthTool() {
        signalStrengthRunnable = new Runnable() {
            @Override
            public void run() {
                displaySignalStrength();
                // Schedule the next update in 2 seconds
                signalStrengthHandler.postDelayed(this, 2000);
            }
        };
    }

    private void displaySignalStrength() {
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                int rssi = wifiInfo.getRssi();
                signalStrengthText.setText(rssi + " dBm");
                signalStrengthDescriptionText.setText(getSignalStrengthDescription(rssi));
            } else {
                signalStrengthText.setText("Not connected");
                signalStrengthDescriptionText.setText("");
            }
        } else {
            signalStrengthText.setText("Wi-Fi not available");
            signalStrengthDescriptionText.setText("");
        }
    }

    private String getSignalStrengthDescription(int rssi) {
        if (rssi >= -30) {
            return "Perfect signal";
        } else if (rssi >= -50) {
            return "Excellent signal";
        } else if (rssi >= -67) {
            return "Very good signal";
        } else if (rssi >= -70) {
            return "Okay signal";
        } else if (rssi >= -80) {
            return "Poor signal";
        } else {
            return "Very poor signal";
        }
    }

    private String intToIp(int i) {
        if (i == 0) return "N/A";
        return (i & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                ((i >> 24) & 0xFF);
    }
}
