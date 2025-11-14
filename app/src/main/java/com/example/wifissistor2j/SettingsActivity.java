package com.example.wifissistor2j;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getSharedPreferences("theme_prefs", MODE_PRIVATE);
        bottomNavigationView = findViewById(R.id.bottom_nav);

        // Theme settings
        RadioGroup themeRadioGroup = findViewById(R.id.theme_radio_group);
        int savedTheme = sharedPreferences.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (savedTheme == AppCompatDelegate.MODE_NIGHT_NO) {
            themeRadioGroup.check(R.id.theme_light);
        } else if (savedTheme == AppCompatDelegate.MODE_NIGHT_YES) {
            themeRadioGroup.check(R.id.theme_dark);
        } else {
            themeRadioGroup.check(R.id.theme_system_default);
        }

        themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int themeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            if (checkedId == R.id.theme_light) {
                themeMode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (checkedId == R.id.theme_dark) {
                themeMode = AppCompatDelegate.MODE_NIGHT_YES;
            }
            AppCompatDelegate.setDefaultNightMode(themeMode);
            sharedPreferences.edit().putInt("theme", themeMode).apply();
        });

        // Speed test unit settings
        RadioGroup unitsRadioGroup = findViewById(R.id.units_radio_group);
        String savedUnits = sharedPreferences.getString("speed_units", "Default");
        if ("MB/s".equals(savedUnits)) {
            unitsRadioGroup.check(R.id.units_mbs);
        } else if ("Mbps".equals(savedUnits)) {
            unitsRadioGroup.check(R.id.units_mbps);
        } else {
            unitsRadioGroup.check(R.id.units_default);
        }

        unitsRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String units = "Default";
            if (checkedId == R.id.units_mbs) {
                units = "MB/s";
            } else if (checkedId == R.id.units_mbps) {
                units = "Mbps";
            }
            sharedPreferences.edit().putString("speed_units", units).apply();
        });

        setupNavigation();
    }

    private void setupNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_settings) {
                return true; // Already on this screen
            }

            Intent intent;
            if (itemId == R.id.nav_home) {
                intent = new Intent(this, HomeActivity.class);
            } else if (itemId == R.id.nav_tools) {
                intent = new Intent(this, ToolsActivity.class);
            } else if (itemId == R.id.nav_map) {
                intent = new Intent(this, MapActivity.class);
            } else {
                return false; // Should not happen
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set the correct navigation item when returning to the activity
        bottomNavigationView.setSelectedItemId(R.id.nav_settings);
    }
}
