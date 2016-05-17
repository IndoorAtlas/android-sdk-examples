/*
 *
 */
package com.indooratlas.android.sdk.examples.credentials;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.examples.LocationManagerHelper;
import com.indooratlas.android.sdk.examples.R;
import com.indooratlas.android.sdk.examples.SdkExample;

import java.util.Locale;

/**
 * There are two ways of setting credentials:
 * <ul>
 * <li>a) specifying as meta-data in AndroidManifest.xml</li>
 * <li>b) passing in as extra parameters via{@link IALocationManager#create(Context, Bundle)}</li>
 * </ul>
 * This example demonstrates option a).
 */
@SdkExample(description = R.string.example_credentials_description)
public class CredentialsFromManifestActivity extends AppCompatActivity implements IALocationListener {

    private IALocationManager mLocationManager;
    private TextView mLog;

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.text_only);
        mLog = (TextView) findViewById(R.id.text);

        mLocationManager = IALocationManager.create(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationManager.destroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLocationManager.requestLocationUpdates(IALocationRequest.create(), this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocationManager.removeLocationUpdates(this);
    }

    @Override
    public void onLocationChanged(IALocation location) {
        log(String.format(Locale.US, "%f,%f, accuracy: %.2f", location.getLatitude(),
                location.getLongitude(), location.getAccuracy()));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        log(LocationManagerHelper.logStatusChanges(status, extras));
    }

    private void log(String msg) {
        mLog.append("\n" + msg);
    }

}
