package com.example.wifissistor2j;

import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
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

public class ToolsActivity extends AppCompatActivity implements SignalStrengthListener {

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

    private SignalStrengthProvider signalStrengthProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tools);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        signalStrengthProvider = new SignalStrengthProvider(this, this);

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

        setupNavigation();
        displayWifiInfo();
        setupPingTool();
    }

    @Override
    protected void onResume() {
        super.onResume();
        signalStrengthProvider.start();
        bottomNavigationView.setSelectedItemId(R.id.nav_tools);
    }

    @Override
    protected void onPause() {
        super.onPause();
        signalStrengthProvider.stop();
    }

    private void displayWifiInfo() {
        if (wifiManager == null) {
            String wifiNotAvailable = getString(R.string.wifi_not_available);
            ipAddressText.setText(wifiNotAvailable);
            gatewayText.setText(wifiNotAvailable);
            subnetMaskText.setText(wifiNotAvailable);
            dnsServerText.setText(wifiNotAvailable);
            return;
        }

        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        if (dhcpInfo == null || dhcpInfo.ipAddress == 0) {
            String notConnected = getString(R.string.tools_not_connected_to_wifi);
            ipAddressText.setText(notConnected);
            gatewayText.setText(notConnected);
            subnetMaskText.setText(notConnected);
            dnsServerText.setText(notConnected);
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
                Toast.makeText(this, R.string.tools_ping_empty_host_toast, Toast.LENGTH_SHORT).show();
                return;
            }
            runPingTest(host);
        });
    }

    private void runPingTest(String host) {
        pingResultText.setText(getString(R.string.tools_pinging, host));
        executorService.execute(() -> {
            try {
                long startTime = System.currentTimeMillis();
                InetAddress inetAddress = InetAddress.getByName(host);
                boolean isReachable = inetAddress.isReachable(5000); // 5 second timeout
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                handler.post(() -> {
                    if (isReachable) {
                        pingResultText.setText(getString(R.string.tools_ping_success, host, duration));
                    } else {
                        pingResultText.setText(R.string.tools_ping_failed);
                    }
                });

            } catch (IOException e) {
                handler.post(() -> pingResultText.setText(getString(R.string.tools_ping_error, e.getMessage())));
            }
        });
    }

    private String getSignalStrengthDescription(int rssi) {
        if (rssi >= -30) {
            return getString(R.string.signal_strength_perfect);
        } else if (rssi >= -50) {
            return getString(R.string.signal_strength_excellent);
        } else if (rssi >= -67) {
            return getString(R.string.signal_strength_very_good);
        } else if (rssi >= -70) {
            return getString(R.string.signal_strength_okay);
        } else if (rssi >= -80) {
            return getString(R.string.signal_strength_poor);
        } else {
            return getString(R.string.signal_strength_very_poor);
        }
    }

    private String intToIp(int i) {
        if (i == 0) return getString(R.string.tools_info_na);
        return (i & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                ((i >> 24) & 0xFF);
    }

    private void setupNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_tools) {
                return true;
            }
            Intent intent = null;
            if (itemId == R.id.nav_home) {
                intent = new Intent(this, HomeActivity.class);
            } else if (itemId == R.id.nav_map) {
                intent = new Intent(this, MapActivity.class);
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

    @Override
    public void onSignalStrengthChanged(int rssi) {
        signalStrengthText.setText(getString(R.string.signal_strength_dbm, rssi));
        signalStrengthDescriptionText.setText(getSignalStrengthDescription(rssi));
    }

    @Override
    public void onWifiUnavailable() {
        signalStrengthText.setText(R.string.not_connected);
        signalStrengthDescriptionText.setText("");
    }
}
