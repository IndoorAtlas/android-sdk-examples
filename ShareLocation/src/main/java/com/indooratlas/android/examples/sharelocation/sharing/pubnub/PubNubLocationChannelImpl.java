/*
 *
 */
package com.indooratlas.android.examples.sharelocation.sharing.pubnub;

import android.text.TextUtils;

import com.indooratlas.android.examples.sharelocation.sharing.LocationChannel;
import com.indooratlas.android.examples.sharelocation.sharing.LocationChannelException;
import com.indooratlas.android.examples.sharelocation.sharing.LocationChannelListener;
import com.indooratlas.android.examples.sharelocation.sharing.LocationSource;
import com.indooratlas.android.sdk.IALocation;
import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubException;

/**
 *
 */
public class PubNubLocationChannelImpl implements LocationChannel {

    private Pubnub mPubNub;

    private String mChannel;

    private LocationChannelListener mListener;

    public PubNubLocationChannelImpl(String publishKey, String subscribeKey, String channel) {

        if (TextUtils.isEmpty(publishKey)
                || TextUtils.isEmpty(subscribeKey)
                || TextUtils.isEmpty(channel)) {
            throw new IllegalArgumentException("all arguments must be non null");
        }
        mPubNub = new Pubnub(publishKey, subscribeKey);
        mChannel = channel;

    }

    @Override
    public void subscribe(LocationChannelListener listener) throws LocationChannelException {

        mListener = listener;
        try {
            mPubNub.subscribe(mChannel, mCallback);
        } catch (PubnubException e) {
            throw new LocationChannelException("subscribe failed", e);
        }
    }

    @Override
    public void publish(LocationSource identity, IALocation location) {



        mPubNub.publish(mChannel, );

    }

    @Override
    public void disconnect() {
        mPubNub.unsubscribeAll();
    }

    private Callback mCallback = new Callback() {
        @Override
        public void successCallback(String channel, Object message) {

        }
    };
}
