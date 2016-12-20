package com.indooratlas.android.sdk.examples.simple;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
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

    static final String FASTEST_INTERVAL = "fastestInterval";
    static final String SHORTEST_DISPLACEMENT = "shortestDisplacement";

    private IALocationManager mLocationManager;
    private TextView mLog;
    private ScrollView mScrollView;
    private long mRequestStartTime;

    private long mFastestInterval = -1L;
    private float mShortestDisplacement = -1f;

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mFastestInterval = savedInstanceState.getLong(FASTEST_INTERVAL);
            mShortestDisplacement = savedInstanceState.getFloat(SHORTEST_DISPLACEMENT);
        }

        setContentView(R.layout.activity_simple);
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
        setLocationRequestOptions();
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
        log(String.format(Locale.US, "%f,%f, accuracy: %.2f, certainty: %.2f",
                location.getLatitude(), location.getLongitude(), location.getAccuracy(),
                location.getFloorCertainty()));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
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
                log("Calibration change. Quality: " + quality);
                break;
            case IALocationManager.STATUS_AVAILABLE:
                log("onStatusChanged: Available");
                break;
            case IALocationManager.STATUS_LIMITED:
                log("onStatusChanged: Limited");
                break;
            case IALocationManager.STATUS_OUT_OF_SERVICE:
                log("onStatusChanged: Out of service");
                break;
            case IALocationManager.STATUS_TEMPORARILY_UNAVAILABLE:
                log("onStatusChanged: Temporarily unavailable");
        }
    }

    @Override
    public void onEnterRegion(IARegion region) {
        log("onEnterRegion: " + regionType(region.getType()) + ", " + region.getId());
    }

    @Override
    public void onExitRegion(IARegion region) {
        log("onExitRegion: " + regionType(region.getType()) + ", " + region.getId());
    }

    /**
     * Turn {@link IARegion#getType()} to human-readable name
     */
    private String regionType(int type) {
        switch (type) {
            case IARegion.TYPE_UNKNOWN:
                return "unknown";
            case IARegion.TYPE_FLOOR_PLAN:
                return "floor plan";
            case IARegion.TYPE_VENUE:
                return "venue";
            default:
                return Integer.toString(type);
        }
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

    private void setLocationRequestOptions() {

        LayoutInflater inflater = LayoutInflater.from(this);
        final View dialogLayout = inflater.inflate(R.layout.location_request_dialog, null);

        final EditText fastestInterval = (EditText) dialogLayout
                .findViewById(R.id.edit_text_interval);

        final EditText shortestDisplacement = (EditText) dialogLayout
                .findViewById(R.id.edit_text_displacement);

        if (mFastestInterval != -1L) {
            fastestInterval.setText(String.valueOf(mFastestInterval));
        }

        if (mShortestDisplacement != -1f) {
            shortestDisplacement.setText(String.valueOf(mShortestDisplacement));
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.dialog_request_options_title)
                .setView(dialogLayout)
                .setCancelable(true)
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mRequestStartTime = SystemClock.elapsedRealtime();
                        IALocationRequest request = IALocationRequest.create();

                        String fastestIntervalInput = fastestInterval.getText()
                                .toString();
                        String shortestDisplacementInput = shortestDisplacement.getText()
                                .toString();

                        if (!fastestIntervalInput.isEmpty()) {
                            mFastestInterval = Long.valueOf(fastestIntervalInput);
                            request.setFastestInterval(mFastestInterval);
                        } else {
                            mFastestInterval = -1L;
                        }

                        if (!shortestDisplacementInput.isEmpty()) {
                            mShortestDisplacement = Float.valueOf(shortestDisplacementInput);
                            request.setSmallestDisplacement(mShortestDisplacement);
                        } else {
                            mShortestDisplacement = -1f;
                        }

                        mLocationManager.removeLocationUpdates(SimpleActivity.this);
                        mLocationManager.requestLocationUpdates(request, SimpleActivity.this);
                        log("requestLocationUpdates");
                    }
                })
                .setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putLong(FASTEST_INTERVAL, mFastestInterval);
        savedInstanceState.putFloat(SHORTEST_DISPLACEMENT, mShortestDisplacement);
        super.onSaveInstanceState(savedInstanceState);
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
