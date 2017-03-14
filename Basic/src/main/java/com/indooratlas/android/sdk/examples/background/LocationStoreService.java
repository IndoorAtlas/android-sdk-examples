package com.indooratlas.android.sdk.examples.background;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.indooratlas.android.sdk.IALocation;

/**
 * Very simple IntentService used to store received locations. Sends a local broadcast
 * ("location-update") when a new location is received. This can be used to update e.g. UI
 * components.
 */
public class LocationStoreService extends IntentService {

    public LocationStoreService() {
        super("LocationStoreService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        IALocation location = IALocation.from(intent);
        if (location != null) {
            LocationStore.obtain(this).store(location);
            sendLocalBroadcast(location);
        }
    }

    private void sendLocalBroadcast(IALocation location) {
        Intent intent = new Intent("location-update");
        intent.putExtra("location", location);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


}
