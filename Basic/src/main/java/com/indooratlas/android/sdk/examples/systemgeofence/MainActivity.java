package com.indooratlas.android.sdk.examples.systemgeofence;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.UiThread;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
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

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@SdkExample(description = R.string.example_system_geofencing_description)
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final static int REQUEST_CODE_ACCESS_BACKGROUND_LOCATION = 1;
    private final static String TAG = "Geofencing";
    private final static float GEOFENCING_RADIUS_METER = 100.0f;

    private LatLng geofenceCenterPoint = new LatLng(0,0);
    private GeofencingClient geofencingClient;
    private Button registerGeofenceButton;
    private Button unregisterGoefenceButton;
    private ProgressBar loadingBar;
    private TextView defaultGeofenceCoordText;
    private OkHttpClient httpClient = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_geofence);

        registerGeofenceButton = findViewById(R.id.button1);
        unregisterGoefenceButton = findViewById(R.id.button2);
        loadingBar = findViewById(R.id.progressBar);
        defaultGeofenceCoordText = findViewById(R.id.textView1);
        registerGeofenceButton.setOnClickListener(this);
        unregisterGoefenceButton.setOnClickListener(this);

        geofencingClient = LocationServices.getGeofencingClient(this);
        ensurePermissions();

        /**
         * Loading venue center coordinate from Positioning API
         * If the api key have not "Positioning API" scope, it use hardcoded {@link geofenceCenterPoint geofenceCenterPoint} by default
         */
        String url = String.format("https://positioning-api.indooratlas.com/v1/venues?key=%s", getString(R.string.indooratlas_api_key));
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
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String resStr = response.body().string();
                boolean useDefaultCoord = true;
                try {
                    JSONArray jsonArr = new JSONArray(resStr);
                    if (jsonArr.length() != 0) {
                        JSONObject venueJson = (JSONObject) jsonArr.get(0);
                        JSONObject coordinatesJson = venueJson.getJSONObject("coordinates");
                        double lat = coordinatesJson.getDouble("lat");
                        double lon = coordinatesJson.getDouble("lon");
                        geofenceCenterPoint = new LatLng(lat, lon);
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

    private void registerGeofence() {

        geofencingClient.removeGeofences(getGeofencePenddingIntent()).addOnSuccessListener(new OnSuccessListener<Void>() {
            @SuppressLint("MissingPermission")
            @Override
            public void onSuccess(Void aVoid) {
                // Build the Geofence Object
                Geofence geofence = new Geofence.Builder()
                        .setRequestId("CP_OFFICE")
                        .setCircularRegion(geofenceCenterPoint.latitude,
                                geofenceCenterPoint.longitude,
                                GEOFENCING_RADIUS_METER
                        )
                        .setExpirationDuration(-1)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT )
                        .build();

                // Build the geofence request
                GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_EXIT)
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

            }
        });
    }

    private void unregisterGeofence() {
        geofencingClient.removeGeofences(getGeofencePenddingIntent());
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
                                Log.d(TAG, "request permissions");
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
        defaultGeofenceCoordText.setVisibility(View.GONE);
    }

    @UiThread
    private void stopLoading(boolean useDefaultCoord) {
        registerGeofenceButton.setEnabled(true);
        unregisterGoefenceButton.setEnabled(true);
        loadingBar.setVisibility(View.GONE);
        defaultGeofenceCoordText.setVisibility(useDefaultCoord ? View.VISIBLE : View.GONE);
    }
}