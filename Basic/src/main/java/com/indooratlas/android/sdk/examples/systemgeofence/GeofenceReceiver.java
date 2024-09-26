package com.indooratlas.android.sdk.examples.systemgeofence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.indooratlas.android.sdk.examples.foregroundservice.ForegroundService;

public class GeofenceReceiver extends BroadcastReceiver implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {
    public final static String ACTION_GEOFENCE_EVENT = "ACTION_GEOFENCE_EVENT";
    private final static String TAG = "Geofencing";

    @Override
    public void onReceive(Context context, Intent intent) {

        sendLogToMainActivity(context, "GeofenceReceiver onReceive");

        if (intent.getAction().equals(ACTION_GEOFENCE_EVENT)) {
            GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

            if (geofencingEvent.hasError()) {
                sendLogToMainActivity(context, "Geofencing Error Code(" +
                        geofencingEvent.getErrorCode() + ")");
                return;
            }
            switch (geofencingEvent.getGeofenceTransition()) {
                case Geofence.GEOFENCE_TRANSITION_ENTER:
                    sendLogToMainActivity(context, "GeofenceReceiver onReceive : " +
                            "GEOFENCE_TRANSITION_ENTER");
                    sendLogToMainActivity(context, "GeofenceReceiver onReceive : " +
                            "starting IndoorAtlas positioning in Foreground Service" +
                            "--> See ActionBar for location updates");

                    sendLogToMainActivity(context, "GeofenceReceiver onReceive : " +
                            "--> Note: you can use a Fake GPS app to simulate location updates" +
                            " inside and outside the geofence platform geofence area");

                    // After starting the foreground service, you can close the
                    // example app and continue
                    // using the foreground service notification
                    Intent startIntent = new Intent(context, ForegroundService.class);
                    startIntent.setAction(ForegroundService.STARTFOREGROUND_ACTION);
                    context.startService(startIntent);
                    break;
                case Geofence.GEOFENCE_TRANSITION_EXIT:
                    sendLogToMainActivity(context, "GeofenceReceiver onReceive : " +
                            "GEOFENCE_TRANSITION_EXIT");
                    sendLogToMainActivity(context, "GeofenceReceiver onReceive : " +
                            "stopping IndoorAtlas positioning in Foreground Service" +
                            "--> ActionBar notification is closed");

                    // To close the foreground service, "Stop foreground service" button
                    // must be pressed
                    Intent stopIntent = new Intent(context, ForegroundService.class);
                    stopIntent.setAction(ForegroundService.STOPFOREGROUND_ACTION);
                    context.startService(stopIntent);
                    break;
                default:
                    break;
            }

        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onResult(@NonNull Status status) {

    }

    private void sendLogToMainActivity(Context context, String log) {
        // Send a local broadcast
        Log.d(TAG, "sendLogToMainActivity : "+log);
        Intent localIntent = new Intent("GeofenceReceiverLog");
        localIntent.putExtra("log", log);
        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
    }
}
