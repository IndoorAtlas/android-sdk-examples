package com.indooratlas.android.sdk.examples.systemgeofence;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.UiThread;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.indooratlas.android.sdk.examples.R;
import com.indooratlas.android.sdk.examples.SdkExample;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

@SdkExample(description = R.string.example_system_geofencing_description)
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final static int REQUEST_CODE_ACCESS_BACKGROUND_LOCATION = 1;
    private final static String TAG = "Geofencing";
    private final static float GEOFENCING_RADIUS_METER = 100.0f;

    private TextView mLog;
    private ScrollView mScrollView;
    private long mRequestStartTime;


    private LatLng geofenceCenterPoint = new LatLng(0,0);
    private GeofencingClient geofencingClient;
    private Button registerGeofenceButton;
    private Button unregisterGoefenceButton;
    private ProgressBar loadingBar;
    private OkHttpClient httpClient = new OkHttpClient();
    private String mVenueName = "not available";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_geofence);

        mScrollView = findViewById(R.id.scroller);
        registerGeofenceButton = findViewById(R.id.button1);
        unregisterGoefenceButton = findViewById(R.id.button2);
        loadingBar = findViewById(R.id.progressBar);
        mLog = findViewById(R.id.textViewLog);

        registerGeofenceButton.setOnClickListener(this);
        unregisterGoefenceButton.setOnClickListener(this);

        geofencingClient = LocationServices.getGeofencingClient(this);
        ensurePermissions();



        // Register a BroadcastReceiver to receive the logs from GeofenceReceiver
        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Timber.d("LocalBroadcastManager.onReceive: %s", intent.getAction());
                if (intent != null) {
                    String logText = intent.getStringExtra("log");
                    if (logText != null) {
                        log(logText);
                    }
                }
            }
        }, new IntentFilter("GeofenceReceiverLog"));

        /**
         * Loading venue center coordinate from Positioning API
         * If the api key have not "Positioning API" scope,
         * it use hardcoded {@link geofenceCenterPoint geofenceCenterPoint} by default
         */
        String url =
                String.format("https://positioning-api.indooratlas.com/v1/venues?key=%s",
                        getString(R.string.indooratlas_api_key));
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        startLoading();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stopLoading(true);
                    }
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response)
                    throws IOException {
                String resStr = response.body().string();

                log("onResponse: venue received: " + resStr);

                boolean useDefaultCoord = true;
                try {
                    JSONArray jsonArr = new JSONArray(resStr);
                    if (jsonArr.length() != 0) {
                        JSONObject venueJson = (JSONObject) jsonArr.get(0);
                        JSONObject coordinatesJson = venueJson.getJSONObject("coordinates");
                        double lat = coordinatesJson.getDouble("lat");
                        double lon = coordinatesJson.getDouble("lon");
                        geofenceCenterPoint = new LatLng(lat, lon);

                        mVenueName = venueJson.getString("name");
                        log("onResponse: using center coordinate of venue : " + mVenueName);

                        useDefaultCoord = false;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                final boolean finalUseDefaultCoord = useDefaultCoord;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stopLoading(finalUseDefaultCoord);
                    }
                });
            }
        });

    }


    public void onResume() {
        super.onResume();
        log("onResume");

        // Check if notifications are enabled for my app.
        // Needed to show IA positioning updates in the notification aka Action bar.
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        boolean areNotificationsEnabled = notificationManager.areNotificationsEnabled();

        log("onResume, areNotificationsEnabled : "+areNotificationsEnabled);

        if (!areNotificationsEnabled) {
            // Notifications are disabled for your app. You might want to notify the user or take appropriate action.
            Intent intent = new Intent();
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("android.provider.extra.APP_PACKAGE", getPackageName());
            startActivity(intent);
        }
    }

    private void registerGeofence() {

        log("registerGeofence: request removal of any previous platform geofences");

        geofencingClient.removeGeofences(getGeofencePenddingIntent()).addOnSuccessListener(
                new OnSuccessListener<Void>() {
            @SuppressLint("MissingPermission")
            @Override
            public void onSuccess(Void aVoid) {
                // Build the Geofence Object
                Geofence geofence = new Geofence.Builder()
                        .setRequestId(mVenueName)
                        .setCircularRegion(geofenceCenterPoint.latitude,
                                geofenceCenterPoint.longitude,
                                GEOFENCING_RADIUS_METER
                        )
                        .setExpirationDuration(-1)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                                Geofence.GEOFENCE_TRANSITION_EXIT )
                        .build();

                log("registerGeofence: created new geofence: " + geofence
                        + " at " + geofenceCenterPoint.toString());

                // Build the geofence request
                GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER |
                                GeofencingRequest.INITIAL_TRIGGER_EXIT)
                        .addGeofence(geofence)
                        .build();
                /**
                 * Geofencing request will cancel after device reboot.
                 * If you looking for keep wake up foreground service after device reboot completed,
                 * you need need to implement your own {@link android.content.BroadcastReceiver BroadcastReceiver} to
                 * handle {@link android.permission.RECEIVE_BOOT_COMPLETED RECEIVE_BOOT_COMPLETED} event
                 * to re-register geofening request.
                 */

                geofencingClient.addGeofences(geofencingRequest, getGeofencePenddingIntent());

                mRequestStartTime = SystemClock.elapsedRealtime();

                log( "registerGeofence: geofence added OK to OS for monitoring: " + geofence
                        + " at " + geofenceCenterPoint.toString());
            }
        });
    }

    private void unregisterGeofence() {
        log("Unregistering platform geofence");
        geofencingClient.removeGeofences(getGeofencePenddingIntent());
    }

    private void log(String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                double duration = mRequestStartTime != 0
                        ? (SystemClock.elapsedRealtime() - mRequestStartTime) / 1e3
                        : 0d;
                mLog.append("\n");
                mLog.append(String.format(Locale.US, "\n[%06.2f]: %s", duration, msg));

                mScrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        mScrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });
    }


    public static boolean checkBackgroundLocationPermissions(Activity activity) {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void ensurePermissions() {

        if (!checkBackgroundLocationPermissions(this)) {
            // We don't have access to ACCESS_BACKGROUND_LOCATION
            // If you want to keep receive the geofencing event after activity life-cycle destroyed,
            // it required background location permission granted
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {

                new AlertDialog.Builder(this)
                        .setTitle(R.string.location_permission_request_title)
                        .setMessage(R.string.location_permission_request_rationale)
                        .setPositiveButton(R.string.permission_button_accept, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                log("requesting permissions");
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                        REQUEST_CODE_ACCESS_BACKGROUND_LOCATION);
                            }
                        })
                        .setNegativeButton(R.string.permission_button_deny, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(MainActivity.this,
                                        R.string.location_permission_denied_message,
                                        Toast.LENGTH_LONG).show();
                            }
                        })
                        .show();

            } else {

                // ask user for permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        REQUEST_CODE_ACCESS_BACKGROUND_LOCATION);

            }

        }
    }

    private PendingIntent getGeofencePenddingIntent() {
        Intent intent = new Intent(this, GeofenceReceiver.class);
        intent.setAction(GeofenceReceiver.ACTION_GEOFENCE_EVENT);
        // Use FLAG_UPDATE_CURRENT so that you get the same pending intent back when calling
        PendingIntent geofencePenddingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        return geofencePenddingIntent;
    }

    @Override
    public void onClick(View view) {
        if (view == registerGeofenceButton) {
            registerGeofence();
        }else if (view == unregisterGoefenceButton) {
            unregisterGeofence();
        }
    }

    @UiThread
    private void startLoading() {
        registerGeofenceButton.setEnabled(false);
        unregisterGoefenceButton.setEnabled(false);
        loadingBar.setVisibility(View.VISIBLE);
        //defaultGeofenceCoordText.setVisibility(View.GONE);
        log("Loading venue center coordinate from Positioning API");
    }

    @UiThread
    private void stopLoading(boolean useDefaultCoord) {
        registerGeofenceButton.setEnabled(true);
        unregisterGoefenceButton.setEnabled(true);
        loadingBar.setVisibility(View.GONE);
        if (useDefaultCoord) {
            log("Failed to load venue center coordinate from Positioning API, " +
                    "using hardcoded value, see code. The used API key must have Positioning API scope.");
        } else {
            log("Venue center coordinate loaded from Positioning API");
        }

    }
}