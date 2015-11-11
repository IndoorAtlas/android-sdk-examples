/*
 *
 */
package com.indooratlas.android.sdk.examples.sharelocation.channel;

/**
 * Simple contract for subscribing for events and publishing our locations.
 */
public interface LocationChannel {

    /**
     * Subscribe for events occurring on {@code channelName}.
     *
     * @param channelName Channel name to listen to. May be any identifier.
     * @param listener Listener that's methods will be invoked when clients publish their locations
     *                 to channel
     * @throws LocationChannelException is thrown in case subscribing failed
     */
    void subscribe(String channelName, LocationChannelListener listener)
            throws LocationChannelException;

    /**
     * Un-subscribe from given {@code channelName}.
     */
    void unsubscribe(String channelName);

    /**
     * Publish my location to given {@code channelName}.
     */
    void publish(String channelName, LocationEvent event);

    /**
     *  Disconnect from service.
     */
    void disconnect();

}
