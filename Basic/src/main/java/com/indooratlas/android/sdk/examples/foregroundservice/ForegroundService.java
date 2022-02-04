package com.indooratlas.android.sdk.examples.foregroundservice;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IARegion;
import com.indooratlas.android.sdk.examples.R;
import com.indooratlas.android.sdk.resources.IAFloorPlan;
import com.indooratlas.android.sdk.resources.IAVenue;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ForegroundService extends Service implements IARegion.Listener {

    public static final String MAIN_ACTION = "main_action";
    public static final String PAUSE_ACTION = "pause_positioning_action";
    public static final String START_ACTION = "start_positioning_action";
    public static final String STOP_ACTION = "stop_positioning_action";
    public static final String STARTFOREGROUND_ACTION = "startforeground";
    public static final String STOPFOREGROUND_ACTION = "stopforeground";

    public static final int NOTIFICATION_ID = 101;
    private static final String NOTIFICATION_CHANNEL_ID = "example_notification_channel";

    private static final String LOG_TAG = "IAForegroundExample";

    private String mReportEndpoint;
    private Bitmap mLargeIconBitmap;
    private IALocationManager mIALocationManager;
    private IAVenue mCurrentVenue = null;

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel notificationChannel =
                    new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications",
                            NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        // set this as backgroundReportEndPoint in gradle.properties to enable reporting
        // locations collected by the Foreground Service to an external backend
        mReportEndpoint = getString(R.string.background_report_endpoint);
        if (!mReportEndpoint.isEmpty()) {
            // Append the bluetooth of the device to the reported data to serve as an example ID
            try {
                String urlSuffix = Settings.Secure.getString(getContentResolver(), "bluetooth_name");
                mReportEndpoint += "/" + URLEncoder
                        .encode(urlSuffix, StandardCharsets.UTF_8.toString())
                        .replace("+", "%20"); // escape space differently in path
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }


        mLargeIconBitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_launcher);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.d(LOG_TAG,"Null intent");
            return super.onStartCommand(intent, flags, startId);
        }

        IALocation location = IALocation.from(intent);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) {
            Log.e(LOG_TAG, "No notification manager");
            return super.onStartCommand(intent, flags, startId);
        }

        if (location != null) {
            Log.i(LOG_TAG, "Got IA Location: " + location);

            // Running: build a notification with Stop & Pause buttons and coordinates as text
            NotificationCompat.Builder notificationBuilder = buildNotification(true)
                    .setContentText(
                            location.getLatitude() + ", " +
                            location.getLongitude());

            // Use floor plan ID, if available, as content text
            IARegion region = location.getRegion();
            String title = mCurrentVenue != null ? (mCurrentVenue.getName() + " - ") : "";
            if (region != null && region.getFloorPlan() != null) {
                title += region.getFloorPlan().getName();
                notificationBuilder.setContentTitle(title);
            }

            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());

            // Get IA trace ID
            String traceId = mIALocationManager != null ? mIALocationManager.getExtraInfo().traceId : null;

            if (!mReportEndpoint.isEmpty())
                new PostLocationToBackendTask(mReportEndpoint, mCurrentVenue, traceId).execute(location);

            return START_STICKY;
        }

        if (intent.getAction().equals(ForegroundService.STARTFOREGROUND_ACTION)) {
            Log.i(LOG_TAG, "Received Start Foreground Intent");
            // Automatically also start positioning + build a notification with a Pause button
            startForeground(NOTIFICATION_ID, buildNotification(true).build());
            startPositioning();
            return START_STICKY;

        } else if (intent.getAction().equals(ForegroundService.PAUSE_ACTION)) {
            Log.i(LOG_TAG, "Clicked Pause: stopping positioning");
            // Paused: build a notification with a Start button
            stopPositioning();
            notificationManager.notify(NOTIFICATION_ID, buildNotification(false).build());
        } else if (intent.getAction().equals(ForegroundService.START_ACTION)) {
            Log.i(LOG_TAG, "Clicked Start: starting positioning");
            // Started: build a notification with a Pause button
            notificationManager.notify(NOTIFICATION_ID, buildNotification(true).build());
            startPositioning();
        } else if (intent.getAction().equals(ForegroundService.STOP_ACTION) ||
                intent.getAction().equals(ForegroundService.STOPFOREGROUND_ACTION)) {
            Log.i(LOG_TAG, "Clicked Stop or received stop intent: stopping positioning and service");
            stopPositioning();
            stopForeground(true);
            stopSelf();
        }

		return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "service onDestroy");
        if (mIALocationManager != null) {
            mIALocationManager.destroy();
            mIALocationManager = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Used only in case if services are bound (Bound Services).
        return null;
    }

    private NotificationCompat.Builder buildNotification(boolean running) {
        Intent openMainActivityIntent = new Intent(this, MainActivity.class);
        openMainActivityIntent.setAction(ForegroundService.MAIN_ACTION);
        openMainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(running ? "No floor plan context" : "Paused")
                .setTicker("IndoorAtlas Foreground Service Example")
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(mLargeIconBitmap)
                .setOngoing(true)
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        openMainActivityIntent, 0))
                .addAction(android.R.drawable.ic_media_ff, "Stop",
                        buildPendingIntentWithAction(ForegroundService.STOP_ACTION));

        if (running) {
            // Currently running: add pause buttons
            builder.addAction(android.R.drawable.ic_media_pause, "Pause",
                    buildPendingIntentWithAction(ForegroundService.PAUSE_ACTION));
        } else {
            // add start button
            builder.addAction(android.R.drawable.ic_media_play, "Start",
                    buildPendingIntentWithAction(ForegroundService.START_ACTION));
        }

        return builder;
    }

    private void startPositioning() {
        if (mIALocationManager == null) {
            mIALocationManager = IALocationManager.create(this);
            mIALocationManager.registerRegionListener(this);
        }
        mIALocationManager.requestLocationUpdates(IALocationRequest.create(), buildPendingIntent());
    }

    private void stopPositioning() {
        mCurrentVenue = null;
        if (mIALocationManager == null) {
            mIALocationManager = IALocationManager.create(this);
            mIALocationManager.unregisterRegionListener(this);
        }
        mIALocationManager.removeLocationUpdates(buildPendingIntent());
        mIALocationManager.destroy();
        mIALocationManager = null;
    }

    private PendingIntent buildPendingIntent() {
        return PendingIntent.getService(this, 0,
                new Intent(this, ForegroundService.class), 0);
    }

    private PendingIntent buildPendingIntentWithAction(String action) {
        Intent intent = new Intent(this, ForegroundService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, 0, intent, 0);
    }

    @Override
    public void onEnterRegion(IARegion iaRegion) {
        if (iaRegion.getType() == IARegion.TYPE_VENUE) {
            mCurrentVenue = iaRegion.getVenue();
        }
    }

    @Override
    public void onExitRegion(IARegion iaRegion) {
        if (iaRegion.getType() == IARegion.TYPE_VENUE) {
            mCurrentVenue = null;
        }

    }

    private static class PostLocationToBackendTask extends AsyncTask<IALocation, Void, Void> {
        final String mEndPoint;
        final String mTraceId;
        final String mVenueId;

        PostLocationToBackendTask(String endPoint, IAVenue currentVenue, String traceId) {
            mEndPoint = endPoint;
            mTraceId = traceId;
            mVenueId = currentVenue == null ? null : currentVenue.getId();
        }

        @Override
        protected Void doInBackground(IALocation... iaLocations) {
            IALocation location = iaLocations[0];

            try {
                URL url = new URL(mEndPoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept","application/json");
                conn.setDoOutput(true);
                conn.setDoInput(false);

                JSONObject jsonPayload = new JSONObject()
                        .put("location", new JSONObject()
                                .put("coordinates", new JSONObject()
                                        .put("lat", location.getLatitude())
                                        .put("lon", location.getLongitude()))
                                .put("accuracy", location.getAccuracy())
                                .put("floorNumber", location.getFloorLevel()));

                JSONObject iaContext = new JSONObject();
                if (location.getRegion() != null && location.getRegion().getFloorPlan() != null) {
                    IAFloorPlan floorPlan = location.getRegion().getFloorPlan();
                    iaContext.put("floorPlanId", floorPlan.getId());
                }

                if (mVenueId != null) {
                    iaContext.put("venueId", mVenueId);
                }

                if (mTraceId != null) {
                    iaContext.put("traceId", mTraceId);
                }

                if (iaContext.keys().hasNext()) {
                    jsonPayload.put("context", new JSONObject()
                            .put("indooratlas", iaContext));
                }

                Log.d(LOG_TAG, "put JSON " + jsonPayload.toString() + " to " + url.toString());

                DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                os.writeBytes(jsonPayload.toString());
                os.flush();
                os.close();

                final int responseCode = conn.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    Log.d(LOG_TAG, "PUT success " + responseCode);
                }
                else {
                    Log.e(LOG_TAG, "PUT failed with " + responseCode + " " + conn.getResponseMessage());
                }

                conn.disconnect();
            } catch (MalformedURLException | JSONException | ProtocolException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                Log.w(LOG_TAG, "Failed to report location (maybe bad network): " + e.getMessage());
            }
            return null;
        }
    }
}
