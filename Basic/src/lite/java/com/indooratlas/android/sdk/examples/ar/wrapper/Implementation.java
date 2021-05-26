package com.indooratlas.android.sdk.examples.ar.wrapper;

import com.indooratlas.android.sdk.examples.ar.wrapper.Api;

public class Implementation {
    public static void checkArAvailability(Object contextUnused, Api.ARAvailabilityCallback callback) {
        callback.onARAvailability(false);
    }
}
