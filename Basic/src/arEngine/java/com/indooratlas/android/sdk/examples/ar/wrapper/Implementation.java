package com.indooratlas.android.sdk.examples.ar.wrapper;
import android.content.Context;

public class Implementation {
    public static Api createArWrapper() {
        return new ArEngine();
    }

    public static void checkArAvailability(final Context context, final Api.ARAvailabilityCallback callback) {
        callback.onARAvailability(true);
    }
}
