package com.indooratlas.android.sdk.examples.sharelocation;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IARegion;
import com.indooratlas.android.sdk.examples.R;
import com.indooratlas.android.sdk.examples.SdkExample;
import com.indooratlas.android.sdk.examples.sharelocation.channel.LocationChannel;
import com.indooratlas.android.sdk.examples.sharelocation.channel.LocationChannelException;
import com.indooratlas.android.sdk.examples.sharelocation.channel.LocationChannelListener;
import com.indooratlas.android.sdk.examples.sharelocation.channel.LocationEvent;
import com.indooratlas.android.sdk.examples.sharelocation.channel.LocationSource;
import com.indooratlas.android.sdk.examples.sharelocation.channel.pubnub.PubNubLocationChannelImpl;
import com.indooratlas.android.sdk.examples.sharelocation.view.MultiLocationMapView;
import com.indooratlas.android.sdk.resources.IAFloorPlan;
import com.indooratlas.android.sdk.resources.IALocationListenerSupport;
import com.indooratlas.android.sdk.resources.IAResourceManager;
import com.indooratlas.android.sdk.resources.IAResult;
import com.indooratlas.android.sdk.resources.IAResultCallback;
import com.indooratlas.android.sdk.resources.IATask;

/**
 * Simple example of sharing ones location with others on the same map. Uses PubNub cloud service
 * to share messages between clients. 
 *
 * When ever region change is detected, this client will start listening events for that region and
 * to publish it's own locations for others on the same region.
 */
@SdkExample(description = R.string.example_sharelocation_description,
        title = R.string.example_sharelocation_title)
public class ShareLocationActivity extends AppCompatActivity {

    private static final String KEY_SOURCE = "source";

    private static final String KEY_CHANNEL = "channel";

    private IALocationManager mLocationManager;

    private IAResourceManager mResourceManager;

    private LocationChannel mLocationChannel;

    private LocationSource mMyLocationSource;

    private View mCoordinatorLayout;

    private MultiLocationMapView mMapView;

    private IATask<IAFloorPlan> mFetchFloorPlanTask;

    private String mCustomChannelName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_location);
        mCoordinatorLayout = findViewById(R.id.coordinatorLayout);
        mMapView = (MultiLocationMapView) findViewById(R.id.map);

        mLocationManager = IALocationManager.create(this);
        mResourceManager = IAResourceManager.create(this);

        mLocationChannel = new PubNubLocationChannelImpl(
                getString(R.string.pubnub_publish_key),
                getString(R.string.pubnub_subscribe_key));

        if (savedInstanceState != null) {
            setMyLocationSource((LocationSource) savedInstanceState.getParcelable(KEY_SOURCE));
            if (savedInstanceState.containsKey(KEY_CHANNEL)) {
                setChannel(savedInstanceState.getString(KEY_CHANNEL), true);
            }
        } else {
            setMyLocationSource(new LocationSource(SharingUtils.defaultIdentity(),
                    SharingUtils.randomColor(this)));
        }



    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_SOURCE, mMyLocationSource);
        if (mCustomChannelName != null) {
            outState.putString(KEY_CHANNEL, mCustomChannelName);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLocationManager.registerRegionListener(mRegionChangeHandler);
        mLocationManager.requestLocationUpdates(IALocationRequest.create(), mLocationChangeHandler);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocationManager.removeLocationUpdates(mLocationChangeHandler);
        mLocationManager.unregisterRegionListener(mRegionChangeHandler);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationManager.destroy();
        mLocationChannel.disconnect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_locationsharing, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_set_channel) {
            askChannelName();
            return true;
        } else if (item.getItemId() == R.id.action_change_color) {
            mMyLocationSource = new LocationSource(mMyLocationSource.name,
                    SharingUtils.randomColor(this));
        }
        return false;
    }

    /**
     * Invoked via layout onClick definition.
     */
    public void changeIdentity(View view) {
        final EditText text = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle(R.string.share_dialog_title)
                .setView(text)
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!TextUtils.isEmpty(text.getText())) {
                            setMyLocationSource(new LocationSource(text.getText().toString(),
                                    mMyLocationSource.color));
                        }
                    }
                })
                .setCancelable(true)
                .show();

    }

    /**
     * Ask user to enter custom channel name to interact with.
     */
    private void askChannelName() {
        final EditText text = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle(R.string.channel_dialog_title)
                .setView(text)
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!TextUtils.isEmpty(text.getText())) {
                            setChannel(text.getText().toString(), true);
                        }
                    }
                })
                .setCancelable(true)
                .show();
    }

    /**
     * Start using given channel. If a custom channel was already set and this is not one (but from
     * region change), this method has no effect and {@code false} is returned.
     *
     * @param channelName name of the channel to use
     * @param isCustom    set to true if this came from user and not via region change
     * @return true if new channel was set
     */
    private boolean setChannel(String channelName, boolean isCustom) {
        try {
            if (isCustom || !hasCustomChannel()) {
                mLocationChannel.subscribe(channelName, mChannelListener);
                mCustomChannelName = isCustom ? channelName : null;
                setMessage(getString(R.string.current_channel, channelName));
                return true;
            }
        } catch (LocationChannelException e) {
            showError(getString(R.string.error_setting_channel, e.toString()));
        }
        return false;
    }

    /**
     * Change my "identity".
     */
    private void setMyLocationSource(LocationSource source) {
        mMyLocationSource = source;
        setTitle(getString(R.string.title_my_name, source.name));
    }

    /**
     * Helper method for showing errors as toasts.
     */
    private void showError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    /**
     * Set's a message using Snackbar until next is set.
     */
    private void setMessage(String msg) {
        Snackbar.make(mCoordinatorLayout, msg, Snackbar.LENGTH_INDEFINITE)
                .show();
    }

    /**
     * Update location on map.
     */
    private void showEventOnMap(LocationEvent event) {
        mMapView.updateLocation(event);
    }

    /**
     * @return true if custom channel name has already been set
     */
    private boolean hasCustomChannel() {
        return (mCustomChannelName != null);
    }

    /**
     * Start loading floor plan bitmap
     */
    private void updateMapBitmap(IARegion region) {

        if (region.getType() != IARegion.TYPE_FLOOR_PLAN) {
            return;
        }

        if (mFetchFloorPlanTask != null && !mFetchFloorPlanTask.isCancelled()) {
            mFetchFloorPlanTask.cancel();
        }

        final IATask<IAFloorPlan> task = mResourceManager.fetchFloorPlanWithId(region.getId());
        mFetchFloorPlanTask = task;
        task.setCallback(new IAResultCallback<IAFloorPlan>() {
            @Override
            public void onResult(IAResult<IAFloorPlan> result) {
                if (result.isSuccess() && result.getResult() != null) {
                    mMapView.setFloorPlan(result.getResult());
                } else {
                    if (!task.isCancelled()) {
                        showError(getString(R.string.error_loading_floor_plan));
                    }
                }
            }
        }, Looper.getMainLooper());

    }

    /**
     * Handle events related to location changes.
     */
    private IALocationListener mLocationChangeHandler = new IALocationListenerSupport() {

        @Override
        public void onLocationChanged(IALocation location) {
            Log.d(SharingUtils.TAG, "onLocationChanged: " + location);
            final LocationEvent event = new LocationEvent(mMyLocationSource, location);
            showEventOnMap(event);
            mLocationChannel.publish(location.getRegion().getId(), event);
        }

    };


    /**
     * Handle events related to region changes.
     */
    private IARegion.Listener mRegionChangeHandler = new IARegion.Listener() {
        @Override
        public void onEnterRegion(IARegion region) {
            Log.d(SharingUtils.TAG, "onEnterRegion: " + region);
            setChannel(region.getId(), false);
            updateMapBitmap(region);
        }

        @Override
        public void onExitRegion(IARegion region) {
            mLocationChannel.unsubscribe(region.getId());
        }
    };


    /**
     * Handle events coming from location channel.
     */
    private LocationChannelListener mChannelListener = new LocationChannelListener() {
        @Override
        public void onLocation(final LocationEvent event) {
            if (!event.source.equals(mMyLocationSource)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showEventOnMap(event);
                    }
                });
            }
        }
    };

}
