/*
 *
 */
package com.indooratlas.android.examples.sharelocation.sharing;

import com.indooratlas.android.sdk.IALocation;

/**
 *
 */
public class LocationChannelEvent {

    public final LocationSource source;

    public final IALocation location;

    public LocationChannelEvent(LocationSource source, IALocation location) {
        this.source = source;
        this.location = location;
    }
}
