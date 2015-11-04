/*
 *
 */
package com.indooratlas.android.sdk.examples.sharelocation.channel.pubnub;

import android.text.TextUtils;
import android.util.Log;

import com.indooratlas.android.sdk.examples.sharelocation.channel.ConversionUtils;
import com.indooratlas.android.sdk.examples.sharelocation.channel.LocationChannel;
import com.indooratlas.android.sdk.examples.sharelocation.channel.LocationEvent;
import com.indooratlas.android.sdk.examples.sharelocation.channel.LocationChannelException;
import com.indooratlas.android.sdk.examples.sharelocation.channel.LocationChannelListener;
import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.json.JSONException;
import org.json.JSONObject;

import static com.indooratlas.android.sdk.examples.sharelocation.SharingUtils.TAG;

/**
 * A naive implementation of LocationChannel using PubNub cloud service. This 5minute coding
 * exercise is not to be taken as an example of how to use PubNub, for that please take a look at:
 * https://www.pubnub.com/docs/android-java/pubnub-java-sdk
 */
public class PubNubLocationChannelImpl implements LocationChannel {

    private Pubnub mPubNub;

    private LocationChannelListener mListener;

    private LocationEvent mPendingEvent;

    public PubNubLocationChannelImpl(String publishKey, String subscribeKey) {

        if (TextUtils.isEmpty(publishKey)
                || TextUtils.isEmpty(subscribeKey)) {
            throw new IllegalArgumentException("all arguments must be non null");
        }
        mPubNub = new Pubnub(publishKey, subscribeKey);

    }

    @Override
    public void subscribe(String channelName, LocationChannelListener listener)
            throws LocationChannelException {

        if (TextUtils.isEmpty(channelName)) {
            throw new IllegalArgumentException("channelName must be non empty");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener must be non null");
        }

        mListener = listener;
        try {
            mPubNub.subscribe(channelName, mCallback);
        } catch (PubnubException e) {
            throw new LocationChannelException("subscribe failed", e);
        }
    }

    @Override
    public void unsubscribe(String channel) {
        mPubNub.unsubscribe(channel);
    }

    @Override
    public void publish(String channelName, LocationEvent event) {

        try {
            mPubNub.publish(channelName, ConversionUtils.toJSON(event), false, mCallback);
        } catch (JSONException e) {
            throw new IllegalStateException("conversion failed", e);
        }

    }

    @Override
    public void disconnect() {
        mPubNub.unsubscribeAll();
        mPubNub.shutdown();
    }


    private Callback mCallback = new Callback() {
        @Override
        public void successCallback(String channel, Object message) {
            if (message instanceof JSONObject) {
                JSONObject jsonObject = (JSONObject) message;
                LocationEvent event = ConversionUtils.parseJSONOpt(jsonObject);
                if (event != null) {
                    mListener.onLocation(event);
                }
            }
        }

        @Override
        public void connectCallback(String channel, Object message) {
            Log.w(TAG, "connected, channel: " + channel + ", message: " + message);
        }

        @Override
        public void disconnectCallback(String channel, Object message) {
            Log.w(TAG, "disconnect, channel: " + channel + ", message: " + message);
        }

        @Override
        public void reconnectCallback(String channel, Object message) {
            Log.d(TAG, "reconnect, channel: " + channel + ", message: " + message);
        }

        @Override
        public void errorCallback(String channel, PubnubError error) {
            Log.e(TAG, "error, channel: " + channel + ", error: " + error);
        }
    };
}
