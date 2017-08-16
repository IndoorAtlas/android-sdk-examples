package com.indooratlas.android.sdk.examples.geofence;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.indooratlas.android.sdk.IAGeofence;
import com.indooratlas.android.sdk.IAGeofenceEvent;
import com.indooratlas.android.sdk.IAGeofenceListener;
import com.indooratlas.android.sdk.IAGeofenceRequest;
import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.examples.R;
import com.indooratlas.android.sdk.examples.SdkExample;
import com.indooratlas.android.sdk.examples.utils.ExampleUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

@SdkExample(description = R.string.example_geofence_description)
public class GeofenceActivity extends AppCompatActivity implements IALocationListener,
        IAGeofenceListener {

    private IALocationManager mManager;

    private boolean mGeofencePlaced = false;

    private final static String TAG = "GeofenceActivity";

    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("HH:mm:ss");

    private TextView mEventLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geofence);

        mEventLog = (TextView) findViewById(R.id.event_log);

        mManager = IALocationManager.create(this);
        mManager.requestLocationUpdates(IALocationRequest.create(), this);

        // Setup long click listener for sharing traceId
        ExampleUtils.shareTraceId(findViewById(R.id.layout), GeofenceActivity.this, mManager);
    }

    @Override
    public void onLocationChanged(IALocation location) {
        String currentDateandTime = mDateFormat.format(new Date());

        String text = mEventLog.getText().toString();
        text += "\n" + currentDateandTime + "\t" +"Got location with accuracy: " +
                location.getAccuracy();
        mEventLog.setText(text);
        ((ScrollView) findViewById(R.id.event_log_scroll)).fullScroll(View.FOCUS_DOWN);

        Log.d(TAG, "New location: " + location);

        if (location.getAccuracy() < 10 && !mGeofencePlaced) {
            // Place a geofence where you are after the location has converged to under 10 meter
            // accuracy
            placeNewGeofence(location);
            mGeofencePlaced = true;
        }
    }

    @Override
    public void onStatusChanged(String str, int status, Bundle bundle) {
    }

    @Override
    public void onGeofencesTriggered(IAGeofenceEvent event) {
        String currentDateandTime = mDateFormat.format(new Date());

        String text = mEventLog.getText().toString();

        // Assume we have only one geofence (this example)
        IAGeofence geofence = event.getTriggeringGeofences().get(0);

        String sb = "Geofence triggered. Geofence id: " + geofence.getId() + ". Trigger type: " +
                ((event.getGeofenceTransition() == IAGeofence.GEOFENCE_TRANSITION_ENTER) ?
                        "ENTER" : "EXIT");

        text += "\n" + currentDateandTime + "\t" + sb;
        mEventLog.setText(text);

        ((ScrollView) findViewById(R.id.event_log_scroll)).fullScroll(View.FOCUS_DOWN);

        Log.d(TAG, "New geofence event: " + event);
    }

    @Override
    public void onPause() {
        super.onPause();
        mManager.removeLocationUpdates(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mManager.requestLocationUpdates(IALocationRequest.create(), this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mManager.removeLocationUpdates(this);
        mManager.destroy();
    }

    /**
     * Place a geofence with radius of 10 meters around specified location
     * @param location Location where to put the geofence
     */
    private void placeNewGeofence(IALocation location) {

        // Add a circular geofence by adding points with a 10 m radius clockwise
        double lat_per_meter = 9e-06*Math.cos(Math.PI/180.0*location.getLatitude());
        double lon_per_meter = 9e-06;
        ArrayList<double[]> edges = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            double lat = location.getLatitude() + 10*lat_per_meter*Math.sin(-2*Math.PI*i/10);
            double lon = location.getLongitude() + 10*lon_per_meter*Math.cos(-2*Math.PI*i/10);
            edges.add(new double[]{lat, lon});
            Log.d(TAG, "Geofence: " + lat + ", " + lon);
        }

        String currentDateandTime = mDateFormat.format(new Date());

        String text = mEventLog.getText().toString();
        text += "\n" + currentDateandTime + "\t" + "Placing a geofence in current location!";
        mEventLog.setText(text);

        // If you want to use simple rectangle instead, uncomment the following:
        /*
        ArrayList<double[]> edges = new ArrayList<>();
        // Approximate meter to coordinate transformations
        double latMeters = 0.4488 * 1e-4;
        double lonMeters = 0.8961 * 1e-4;
        // Size of the geofence e.g. 5 by 5 meters
        double geofenceSize = 5;

        // Coordinates of the south-west corner of the geofence
        double lat1 = location.getLatitude() - 0.5 * geofenceSize * latMeters;
        double lon1 = location.getLongitude() - 0.5 * geofenceSize * lonMeters;

        // Coordiantes of the north-east corner of the geofence
        double lat2 = location.getLatitude() + 0.5 * geofenceSize * latMeters;
        double lon2 = location.getLongitude() + 0.5 * geofenceSize * lonMeters;

        // Add them in clockwise order
        edges.add(new double[]{lat1, lon1});
        edges.add(new double[]{lat2, lon1});
        edges.add(new double[]{lat2, lon2});
        edges.add(new double[]{lat1, lon2});
        */

        Log.d(TAG, "Creating a geofence with id \"My geofence\"");
        IAGeofence geofence = new IAGeofence.Builder()
                .withEdges(edges)
                .withId("My geofence")
                .withTransitionType(IAGeofence.GEOFENCE_TRANSITION_ENTER |
                        IAGeofence.GEOFENCE_TRANSITION_EXIT)
                .build();

        Log.i(TAG, "New geofence registered: " + geofence);
        mManager.addGeofences(new IAGeofenceRequest.Builder()
                .withGeofence(geofence)
                .withInitialTrigger(IAGeofenceRequest.INITIAL_TRIGGER_ENTER)
                .build(), this);
    }

}
