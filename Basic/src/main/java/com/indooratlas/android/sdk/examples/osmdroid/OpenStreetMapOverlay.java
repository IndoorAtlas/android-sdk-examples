package com.indooratlas.android.sdk.examples.osmdroid;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Toast;

import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IARegion;
import com.indooratlas.android.sdk.examples.R;
import com.indooratlas.android.sdk.examples.SdkExample;
import com.indooratlas.android.sdk.examples.sharelocation.SharingUtils;
import com.indooratlas.android.sdk.examples.sharelocation.channel.LocationChannel;
import com.indooratlas.android.sdk.examples.sharelocation.channel.LocationChannelException;
import com.indooratlas.android.sdk.examples.sharelocation.channel.LocationChannelListener;
import com.indooratlas.android.sdk.examples.sharelocation.channel.LocationEvent;
import com.indooratlas.android.sdk.examples.sharelocation.channel.LocationSource;
import com.indooratlas.android.sdk.examples.sharelocation.channel.pubnub.PubNubLocationChannelImpl;
import com.indooratlas.android.sdk.resources.IAFloorPlan;
import com.indooratlas.android.sdk.resources.IALatLng;
import com.indooratlas.android.sdk.resources.IALocationListenerSupport;
import com.indooratlas.android.sdk.resources.IAResourceManager;
import com.indooratlas.android.sdk.resources.IAResult;
import com.indooratlas.android.sdk.resources.IAResultCallback;
import com.indooratlas.android.sdk.resources.IATask;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import org.osmdroid.bonuspack.overlays.GroundOverlay;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;

import java.text.SimpleDateFormat;
import java.util.Date;

@SdkExample(description = R.string.example_osm_overlay_description)
public class OpenStreetMapOverlay extends Activity {

    private static final String TAG = "IndoorAtlasExample";

    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 1;

    /* used to decide when bitmap should be downscaled */
    private static final int MAX_DIMENSION = 2048;

    private MapView mOsmv;
    private RelativeLayout mLayout;

    private IARegion mOverlayFloorPlan = null;
    private GroundOverlay mGroundOverlay = null;
    private GroundOverlay mBlueDot = null;
    private GroundOverlay mOtherPhoneDot = null;

    private RotationGestureOverlay mRotationGestureOverlay;
    private CopyrightOverlay mCopyrightOverlay;

    private IALocationManager mIALocationManager;
    private IATask<IAFloorPlan> mFetchFloorPlanTask;
    private Target mLoadTarget;
    private boolean mCameraPositionNeedsUpdating = true; // update on first location


    private String mCustomChannelName;

    private LocationChannel mLocationChannel;

    private LocationSource mMyLocationSource;

    private Marker mMarker;
    private Marker mOtherMarker;

    private IAResourceManager mResourceManager;
    /**
     * Listener that handles location change events.
     */
    private IALocationListener mListener = new IALocationListenerSupport() {

        /**
         * Location changed, move marker and camera position.
         */
        @Override
        public void onLocationChanged(IALocation location) {

            Log.d(TAG, "new location received with coordinates: " + location.getLatitude()
                    + "," + location.getLongitude());


            if (mOsmv == null) {
                // location received before map is initialized, ignoring update here
                return;
            }

            GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());

            if (mBlueDot == null) {

                mBlueDot = new GroundOverlay();
                mBlueDot.setImage(getResources().getDrawable(R.drawable.circle));
                mBlueDot.setTransparency(0.5f);

            } else {
                // move existing markers position to received location
                mOsmv.getOverlayManager().remove(mBlueDot);
            }
            mBlueDot.setPosition(geoPoint);
            mBlueDot.setDimensions(location.getAccuracy(), location.getAccuracy());

            // add to top
            mOsmv.getOverlayManager().add(mBlueDot);


            if (mMarker == null) {
                //enables this opt in feature
                Marker.ENABLE_TEXT_LABELS_WHEN_NO_IMAGE = true;
                //build the marker
                mMarker = new Marker(mOsmv);
                mMarker.setTextLabelBackgroundColor(Color.WHITE);
                mMarker.setTextLabelFontSize(25);
                mMarker.setTextLabelForegroundColor(Color.BLACK);
            } else {
                mOsmv.getOverlayManager().remove(mMarker);
            }

            mMarker.setPosition(geoPoint);
            SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
            String dateString = format.format( new Date()   );
            mMarker.setTitle("Phone: "+dateString);
            //must set the icon to null last
            mMarker.setIcon(null);
            mOsmv.getOverlays().add(mMarker);

            // our camera position needs updating if location has significantly changed
            if (mCameraPositionNeedsUpdating) {
                //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17.5f));
                mOsmv.getController().setCenter(geoPoint);
                mCameraPositionNeedsUpdating = false;
            }
            mOsmv.invalidate();
        }
    };


    private void setMyLocationSource(LocationSource source) {
        mMyLocationSource = source;
        setTitle(getString(R.string.title_my_name, source.name));
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
            mLocationChannel.subscribe(channelName, mChannelListener);
            mCustomChannelName = isCustom ? channelName : null;
            return true;
        } catch (LocationChannelException e) {
            Log.e(TAG, e.toString());
        }
        return false;
    }


    /**
     * Handle events coming from location channel.
     */
    private LocationChannelListener mChannelListener = new LocationChannelListener() {
        @Override
        public void onLocation(final LocationEvent event) {
            Log.d(TAG, "LocationChannelListener.onLocation() : event : " + event);
            if (!event.source.equals(mMyLocationSource)) {
                Log.d(TAG, "LocationChannelListener.onLocation() : HANDLING!");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showEventOnMap(event);
                    }
                });
            }
        }
    };

    private void showEventOnMap(LocationEvent event) {

        Log.d(TAG, "showEventOnMap : " + event);

        if (mOsmv == null) {
            Log.d(TAG, "showEventOnMap : mOsmv : " + mOsmv);
            // location received before map is initialized, ignoring update here
            return;
        }

        GeoPoint geoPoint = new GeoPoint(event.location.getLatitude(), event.location.getLongitude());

        if (mOtherPhoneDot == null) {
            Log.d(TAG, "showEventOnMap : mOtherPhoneDot : null");

            mOtherPhoneDot = new GroundOverlay();
            mOtherPhoneDot.setImage(getResources().getDrawable(R.drawable.circle_red));
            mOtherPhoneDot.setTransparency(0.5f);

        } else {
            // move existing markers position to received location
            mOsmv.getOverlayManager().remove(mOtherPhoneDot);
        }
        mOtherPhoneDot.setPosition(geoPoint);
        mOtherPhoneDot.setDimensions(event.location.getAccuracy(), event.location.getAccuracy());

        // add to top
        mOsmv.getOverlayManager().add(mOtherPhoneDot);


        if (mOtherMarker == null) {
            //enables this opt in feature
            Marker.ENABLE_TEXT_LABELS_WHEN_NO_IMAGE = true;
            //build the marker
            mOtherMarker = new Marker(mOsmv);
            mOtherMarker.setTextLabelBackgroundColor(Color.WHITE);
            mOtherMarker.setTextLabelFontSize(25);
            mOtherMarker.setTextLabelForegroundColor(Color.BLACK);
        } else {
            mOsmv.getOverlayManager().remove(mOtherMarker);
        }

        mOtherMarker.setPosition(geoPoint);
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        String dateString = format.format( new Date()   );
        mOtherMarker.setTitle("Watch: "+dateString);// event.source.name
        //must set the icon to null last
        mOtherMarker.setIcon(null);

        mOsmv.getOverlays().add(mOtherMarker);


        // our camera position needs updating if location has significantly changed

        if (mCameraPositionNeedsUpdating) {
            //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17.5f));
            mOsmv.getController().setCenter(geoPoint);
            mCameraPositionNeedsUpdating = false;
        }
        mOsmv.invalidate();
    }


    /**
     * Listener that changes overlay if needed
     */
    private IARegion.Listener mRegionListener = new IARegion.Listener() {

        @Override
        public void onEnterRegion(IARegion region) {
            if (region.getType() == IARegion.TYPE_FLOOR_PLAN) {
                final String newId = region.getId();
                // Are we entering a new floor plan or coming back the floor plan we just left?
                if (newId != null && mGroundOverlay == null || !region.equals(mOverlayFloorPlan)) {
                    mCameraPositionNeedsUpdating = true; // entering new fp, need to move camera
                    if (mGroundOverlay != null) {
                        mOsmv.getOverlays().remove(mGroundOverlay);
                        mGroundOverlay = null;
                    }
                    mOverlayFloorPlan = region; // overlay will be this (unless error in loading)
                    fetchFloorPlan(newId);
                } else {
                    mGroundOverlay.setTransparency(0.0f);
                }
                Toast.makeText(OpenStreetMapOverlay.this, region.getName(), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onExitRegion(IARegion region) {
            if (mGroundOverlay != null) {
                // Indicate we left this floor plan but leave it there for reference
                // If we enter another floor plan, this one will be removed and another one loaded
                mGroundOverlay.setTransparency(0.5f);
            }
        }

    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // prevent the screen going to sleep while app is on foreground
        findViewById(android.R.id.content).setKeepScreenOn(true);

        mLocationChannel = new PubNubLocationChannelImpl(
                getString(R.string.pubnub_publish_key),
                getString(R.string.pubnub_subscribe_key));

        Log.d(TAG, "Created PubNub channel.");

        setMyLocationSource(new LocationSource(SharingUtils.defaultIdentity(),
                SharingUtils.randomColor(this)));

        setChannel("wearos-mikko", true);


        // instantiate IALocationManager
        mIALocationManager = IALocationManager.create(this);
        mResourceManager = IAResourceManager.create(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // remember to clean up after ourselves
        mIALocationManager.destroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ensurePermissions();

        initOsmMapView();

        // start receiving location updates & monitor region changes
        mIALocationManager.requestLocationUpdates(IALocationRequest.create(), mListener);
        mIALocationManager.registerRegionListener(mRegionListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // unregister location & region changes
        mIALocationManager.removeLocationUpdates(mListener);
        mIALocationManager.registerRegionListener(mRegionListener);
    }


    /**
     * Sets bitmap of floor plan as ground overlay on Open Street Map
     */
    private void setupGroundOverlay(IAFloorPlan floorPlan, Bitmap bitmap) {

        if (mGroundOverlay != null) {
            mOsmv.getOverlayManager().remove(mGroundOverlay);
        }

        if (mOsmv != null) {
            IALatLng iaLatLng = floorPlan.getCenter();

            GroundOverlay overlay = new GroundOverlay();
            overlay.setImage(new BitmapDrawable(getResources(), bitmap));
            overlay.setPosition(new GeoPoint(iaLatLng.latitude, iaLatLng.longitude));
            overlay.setDimensions(floorPlan.getWidthMeters(), floorPlan.getHeightMeters());
            overlay.setBearing(floorPlan.getBearing());

            mGroundOverlay = overlay;

            mOsmv.getOverlayManager().add(mGroundOverlay);
        }
    }

    private void initOsmMapView() {
        if (mOsmv == null) {

            mOsmv = new MapView(this);
            mOsmv.setTilesScaledToDpi(true);

            mOsmv.getController().setZoom(18);

            // Enable zoom and rotation controls
            mRotationGestureOverlay = new RotationGestureOverlay(mOsmv);
            mRotationGestureOverlay.setEnabled(true);
            mOsmv.getOverlayManager().add(mRotationGestureOverlay);
            mOsmv.setMultiTouchControls(true);
            mOsmv.setBuiltInZoomControls(false); // gestures fill this role

            mCopyrightOverlay = new CopyrightOverlay(this);
            mOsmv.getOverlayManager().add(mCopyrightOverlay);

            mOsmv.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);

            mLayout = new RelativeLayout(this);
            mLayout.addView(mOsmv, new RelativeLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));

            setContentView(mLayout);
        }
    }

    /**
     * Checks that we have access to required information, if not ask for users permission. Storage
     * permissions needed for storing the map tile cache.
     */
    private void ensurePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case REQUEST_CODE_WRITE_EXTERNAL_STORAGE:

                if (grantResults.length == 0
                        || grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, "Permission denied",
                            Toast.LENGTH_LONG).show();
                }
                break;
        }
    }


    /**
     * Download floor plan using Picasso library.
     */
    private void fetchFloorPlanBitmap(final IAFloorPlan floorPlan) {

        final String url = floorPlan.getUrl();

        if (mLoadTarget == null) {
            mLoadTarget = new Target() {

                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    Log.d(TAG, "onBitmap loaded with dimensions: " + bitmap.getWidth() + "x"
                            + bitmap.getHeight());
                    setupGroundOverlay(floorPlan, bitmap);
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {
                    // N/A
                }

                @Override
                public void onBitmapFailed(Drawable placeHolderDraweble) {
                    Toast.makeText(OpenStreetMapOverlay.this, "Failed to load bitmap",
                            Toast.LENGTH_SHORT).show();
                    mOverlayFloorPlan = null;
                }
            };
        }

        RequestCreator request = Picasso.with(this).load(url);

        final int bitmapWidth = floorPlan.getBitmapWidth();
        final int bitmapHeight = floorPlan.getBitmapHeight();

        if (bitmapHeight > MAX_DIMENSION) {
            request.resize(0, MAX_DIMENSION);
        } else if (bitmapWidth > MAX_DIMENSION) {
            request.resize(MAX_DIMENSION, 0);
        }

        request.into(mLoadTarget);
    }


    /**
     * Fetches floor plan data from IndoorAtlas server.
     */
    private void fetchFloorPlan(String id) {

        // if there is already running task, cancel it
        cancelPendingNetworkCalls();

        final IATask<IAFloorPlan> task = mResourceManager.fetchFloorPlanWithId(id);

        task.setCallback(new IAResultCallback<IAFloorPlan>() {

            @Override
            public void onResult(IAResult<IAFloorPlan> result) {

                if (result.isSuccess() && result.getResult() != null) {
                    // retrieve bitmap for this floor plan metadata
                    fetchFloorPlanBitmap(result.getResult());
                } else {
                    // ignore errors if this task was already canceled
                    if (!task.isCancelled()) {
                        // do something with error
                        Toast.makeText(OpenStreetMapOverlay.this,
                                "loading floor plan failed: " + result.getError(), Toast.LENGTH_LONG)
                                .show();
                        mOverlayFloorPlan = null;
                    }
                }
            }
        }, Looper.getMainLooper()); // deliver callbacks using main looper

        // keep reference to task so that it can be canceled if needed
        mFetchFloorPlanTask = task;

    }

    /**
     * Helper method to cancel current task if any.
     */
    private void cancelPendingNetworkCalls() {
        if (mFetchFloorPlanTask != null && !mFetchFloorPlanTask.isCancelled()) {
            mFetchFloorPlanTask.cancel();
        }
    }
}
