package com.indooratlas.android.sdk.examples.ar.wrapper;

import android.content.Context;
import android.os.Handler;

import com.google.ar.core.ArCoreApk;
import com.indooratlas.android.sdk.examples.ar.wrapper.Api;
import com.indooratlas.android.sdk.examples.ar.wrapper.ArCore;

public class Implementation {
    public static Api createArWrapper() {
        return new ArCore();
    }

    public static void checkArAvailability(final Context context, final Api.ARAvailabilityCallback callback) {
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(context);
        if (availability.isTransient()) {
            // (This is directly from Google's example code)
            // Re-query at 5Hz while compatibility is checked in the background.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkArAvailability(context, callback);
                }
            }, 200);
        }
        if (availability.isSupported()) {
            callback.onARAvailability(true);
        } else { // Unsupported or unknown.
            callback.onARAvailability(false);
        }
    }
}
