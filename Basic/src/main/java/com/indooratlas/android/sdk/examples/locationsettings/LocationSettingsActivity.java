package com.indooratlas.android.sdk.examples.locationsettings;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

import com.indooratlas.android.sdk.examples.R;
import com.indooratlas.android.sdk.examples.SdkExample;
import com.indooratlas.android.sdk.examples.utils.ExampleUtils;

@SdkExample(description = R.string.example_location_settings_description)
public class LocationSettingsActivity extends AppCompatActivity {


    // This example demonstrates how to access some of the system settings that might affect overall
    // positioning performance with IndoorAtlas SDK.
    // Check our docs page for more information about different device settings
    // https://docs.indooratlas.com/technical/android-settings/


    private static final int WIFI_BACKGROUND_SCANNING_ENABLED_REQUEST_CODE = 100;
    private static final int BT_ENABLED_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_settings);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final String text;
        switch (requestCode) {
            case WIFI_BACKGROUND_SCANNING_ENABLED_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    text = getString(R.string.wifi_background_scanning_enabled);
                } else {
                    text = getString(R.string.wifi_background_scanning_denied);
                }
                ExampleUtils.showInfo(this, text);
                break;

            case BT_ENABLED_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    text = getString(R.string.bt_enabled);
                } else {
                    text = getString(R.string.bt_denied);
                }
                ExampleUtils.showInfo(this, text);
                break;
        }
    }

    /**
     * Check that WiFi is supported and background scanning is enabled
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void onCheckWiFiBackgroundScanning(View view) {
        WifiManager manager = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (manager == null) {
            ExampleUtils.showInfo(this, getString(R.string.wifi_not_supported));
        } else {
            if (manager.isScanAlwaysAvailable()) {
                ExampleUtils.showInfo(this, getString(R.string.wifi_background_scanning_enabled));
            } else {
                // Ask user to enable background scanning
                startActivityForResult(
                        new Intent(WifiManager.ACTION_REQUEST_SCAN_ALWAYS_AVAILABLE),
                        WIFI_BACKGROUND_SCANNING_ENABLED_REQUEST_CODE);
            }
        }
    }

    /**
     * Check if Bluetooth is supported and enabled
     */
    public void onCheckBluetoothStatus(View view) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            ExampleUtils.showInfo(this, getString(R.string.bt_not_supported));
        } else {
            if (adapter.getState() == BluetoothAdapter.STATE_ON) {
                ExampleUtils.showInfo(this, getString(R.string.bt_enabled));
            } else {
                // Ask user to enable Bluetooth
                startActivityForResult(
                        new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                        BT_ENABLED_REQUEST_CODE);
            }
        }
    }

    /**
     * Verify currently selected location mode
     */
    public void onCheckLocationMode(View view) {

        // Check also https://developer.android.com/training/location/change-location-settings.html
        // using the LocationRequest adds dependency to Google Play Services SDK.
        // This approach below shows one way to get a reference about current location mode without
        // the dependency.

        try {
            final int mode = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.LOCATION_MODE);
            if (mode == Settings.Secure.LOCATION_MODE_HIGH_ACCURACY
                    || mode == Settings.Secure.LOCATION_MODE_BATTERY_SAVING) {
                ExampleUtils.showInfo(this, getString(R.string.location_provider_available));
            } else {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        } catch (Settings.SettingNotFoundException exception) {
            ExampleUtils.showInfo(this, exception.getMessage());
        } catch (ActivityNotFoundException exception) {
            ExampleUtils.showInfo(this, exception.getMessage());
        }
    }
}
