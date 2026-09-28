package com.example.smartpantrymanager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * Screen 5: Settings. Lets the user toggle expiring-soon alerts and choose a
 * preferred unit system, satisfying the minimum-screens requirement. Preferences
 * are persisted using SharedPreferences so they survive app restarts.
 */
public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "pantry_settings";
    private static final String KEY_EXPIRY_ALERTS = "expiry_alerts_enabled";
    private static final String KEY_UNIT_SYSTEM = "unit_system"; // "metric" or "imperial"

    private SharedPreferences prefs;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        SwitchMaterial switchExpiryAlerts = findViewById(R.id.switchExpiryAlerts);
        RadioGroup radioGroupUnits = findViewById(R.id.radioGroupUnits);
        RadioButton radioMetric = findViewById(R.id.radioMetric);
        RadioButton radioImperial = findViewById(R.id.radioImperial);

        // Load saved preferences.
        switchExpiryAlerts.setChecked(prefs.getBoolean(KEY_EXPIRY_ALERTS, true));
        String unitSystem = prefs.getString(KEY_UNIT_SYSTEM, "metric");
        if ("imperial".equals(unitSystem)) {
            radioImperial.setChecked(true);
        } else {
            radioMetric.setChecked(true);
        }

        switchExpiryAlerts.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(KEY_EXPIRY_ALERTS, isChecked).apply());

        radioGroupUnits.setOnCheckedChangeListener((group, checkedId) -> {
            String value = (checkedId == R.id.radioImperial) ? "imperial" : "metric";
            prefs.edit().putString(KEY_UNIT_SYSTEM, value).apply();
        });

        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_settings);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_pantry) {
                Intent intent = new Intent(this, PantryListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_suggestions) {
                Intent intent = new Intent(this, SuggestedRecipesActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_settings) {
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_settings);
        }
    }
}
