package com.indooratlas.android.sdk.examples.simple;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IARegion;
import com.indooratlas.android.sdk.examples.R;
import com.indooratlas.android.sdk.examples.SdkExample;

import java.util.Locale;

/**
 * Simple example that demonstrates basic interaction with {@link IALocationManager}.
 */
@SdkExample(description = R.string.example_simple_description)
public class SimpleActivity extends AppCompatActivity
        implements IALocationListener, IARegion.Listener {

    private IALocationManager mLocationManager;
    private TextView mLog;
    private ScrollView mScrollView;
    private long mRequestStartTime;

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_manager);
        mLog = (TextView) findViewById(R.id.text);
        mScrollView = (ScrollView) findViewById(R.id.scroller);
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
        mLocationManager.registerRegionListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocationManager.unregisterRegionListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_simple, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case R.id.action_clear:
                mLog.setText(null);
                return true;
            case R.id.action_share:
                shareLog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void requestUpdates(View view) {
        mRequestStartTime = SystemClock.elapsedRealtime();
        mLocationManager.requestLocationUpdates(IALocationRequest.create(), this);
        log("requestLocationUpdates");
    }

    public void removeUpdates(View view) {
        log("removeLocationUpdates");
        mLocationManager.removeLocationUpdates(this);
    }

    public void setLocation(View view) {
        askLocation();
    }

    @Override
    public void onLocationChanged(IALocation location) {
        log(String.format(Locale.US, "%f,%f, accuracy: %.2f", location.getLatitude(),
                location.getLongitude(), location.getAccuracy()));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        log("onStatusChanged: " + status);
    }

    @Override
    public void onEnterRegion(IARegion region) {
        log("onEnterRegion: " + region.getType() + ", " + region.getId());
    }

    @Override
    public void onExitRegion(IARegion region) {
        log("onExitRegion: " + region.getType() + ", " + region.getId());
    }

    /**
     * Append message into log prefixing with duration since start of location requests.
     */
    private void log(String msg) {
        double duration = mRequestStartTime != 0
                ? (SystemClock.elapsedRealtime() - mRequestStartTime) / 1e3
                : 0d;
        mLog.append(String.format(Locale.US, "\n[%06.2f]: %s", duration, msg));
        mScrollView.smoothScrollBy(0, mLog.getBottom());
    }

    /**
     * Shows AlertDialog with text entry widget. This example assumes that input is a floor plan
     * identifier, i.e. UUID which is displayed in IndoorAtlas developer site next to floor plan's
     * map. See Developer documents for more info: http://docs.indooratlas.com
     */
    private void askLocation() {

        final EditText editText = new EditText(this);

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_set_location_title)
                .setView(editText)
                .setCancelable(true)
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            final String text = editText.getText().toString();
                            final IARegion region = IARegion.floorPlan(text);
                            mLocationManager.setLocation(IALocation.from(region));
                            log("setLocation: " + text);
                        } catch (Exception e) {
                            Toast.makeText(SimpleActivity.this,
                                    getString(R.string.error_could_not_set_location, e.toString()),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }).show();

    }

    /**
     * Share current log content using registered apps.
     */
    private void shareLog() {
        Intent sendIntent = new Intent()
                .setAction(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_TEXT, mLog.getText())
                .setType("text/plain");
        startActivity(sendIntent);
    }

}
