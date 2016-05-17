package com.indooratlas.android.sdk.examples;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.indooratlas.android.sdk.IALocationManager;

/**
 * Helper class containing methods used to create IALocationManager using
 * credentials entered by the user and log status change messages.
 */
public class LocationManagerHelper {

    public static IALocationManager createLocationManager(Context context) {
        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        String prefApiKey = sharedPrefs.getString(context.getString(R.string.pref_key_api_key), "");
        String prefApiSecret = sharedPrefs.getString(context.getString(R.string.pref_key_api_secret), "");

        Bundle extras = new Bundle(2);
        extras.putString(IALocationManager.EXTRA_API_KEY, prefApiKey);
        extras.putString(IALocationManager.EXTRA_API_SECRET, prefApiSecret);

        return IALocationManager.create(context, extras);
    }

    public static String statusToString(int status, Bundle extras) {
        String logMessage = "onStatusChanged: Unknown";
        switch (status) {
            case IALocationManager.STATUS_CALIBRATION_CHANGED:
                String quality = "unknown";
                switch (extras.getInt("quality")) {
                    case IALocationManager.CALIBRATION_POOR:
                        quality = "Poor";
                        break;
                    case IALocationManager.CALIBRATION_GOOD:
                        quality = "Good";
                        break;
                    case IALocationManager.CALIBRATION_EXCELLENT:
                        quality = "Excellent";
                        break;
                }
                logMessage = "Calibration change. Quality: " + quality;
                break;
            case IALocationManager.STATUS_AVAILABLE:
                logMessage = "onStatusChanged: Available";
                break;
            case IALocationManager.STATUS_LIMITED:
                logMessage = "onStatusChanged: Limited";
                break;
            case IALocationManager.STATUS_OUT_OF_SERVICE:
                logMessage = "onStatusChanged: Out of service";
                break;
            case IALocationManager.STATUS_TEMPORARILY_UNAVAILABLE:
                logMessage = "onStatusChanged: Temporarily unavailable";
                break;
        }
        return logMessage;
    }
}
