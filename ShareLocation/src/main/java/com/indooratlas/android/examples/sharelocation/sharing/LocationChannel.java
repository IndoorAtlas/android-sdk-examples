/*
 *
 */
package com.indooratlas.android.examples.sharelocation.sharing;

import com.indooratlas.android.sdk.IALocation;

/**
 *
 */
public interface LocationChannel {


    void subscribe(LocationChannelListener listener) throws LocationChannelException;

    void publish(LocationSource identity, IALocation location);

    void disconnect();

}
