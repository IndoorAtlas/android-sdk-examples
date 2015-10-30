package com.indooratlas.android.sdk.examples;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IARegion;

/**
 *
 */
@SdkExample(description = R.string.example_simple_description)
public class LocationManagerActivity extends AppCompatActivity
    implements IALocationListener, IARegion.Listener {

    private static final String TAG = LocationManagerActivity.class.getSimpleName();
    private static final int REQUEST_CODE_EDIT_LOCATION = 1;

    // public variable for testing purposes
    public IALocationManager mLocationManager;

    private TextView mLog;

    private ScrollView mScrollView;
    private long startTime = 0;
    private boolean firstLocation = false;

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_manager);

        mLog = (TextView) findViewById(R.id.text);
        mScrollView = (ScrollView) findViewById(R.id.scroller);
        Log.d(TAG, "creating manager");
        mLocationManager = IALocationManager.create(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "destroying manager");
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_EDIT_LOCATION && resultCode == RESULT_OK) {
            String floorPlanId = data.getStringExtra(Intent.EXTRA_TEXT);
            mLocationManager.setLocation(IALocation.from(IARegion.floorPlan(floorPlanId)));
            log("set location to " + floorPlanId);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_basic, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case R.id.action_clear:
                mLog.setText(null);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void requestUpdates(View view) {
        log("requestLocationUpdates");
        startTime = System.currentTimeMillis();
        firstLocation = true;
        mLocationManager.requestLocationUpdates(IALocationRequest.create(), this);
    }

    public void removeUpdates(View view) {
        log("removeLocationUpdates");
        startTime = System.currentTimeMillis();
        mLocationManager.removeLocationUpdates(this);
    }

    public void setLocation(View view) {
        startActivityForResult(new Intent(this, LocationEditorActivity.class),
            REQUEST_CODE_EDIT_LOCATION);
    }

    @Override
    public void onLocationChanged(IALocation location) {
        Log.d(TAG, "onLocationChanged: " + location);
        log(location.toString());
        if (firstLocation == true) {
            long time = System.currentTimeMillis();
            firstLocation = false;
            log("First fix in : " + (time - startTime) / 1000.0f + " s");
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        log("onStatusChanged: " + status + ", extras: " + extras);
    }

    public void log(String msg) {
        mLog.append("\n> " + msg);
        mScrollView.smoothScrollBy(0, mLog.getBottom());
    }


    @Override
    public void onEnterRegion(IARegion region) {
        log("onEnterRegion: " + region);
    }

    @Override
    public void onExitRegion(IARegion region) {
        log("onExitRegion: " + region);
    }
}
