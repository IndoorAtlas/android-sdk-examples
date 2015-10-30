package com.indooratlas.android.sdk.examples.mapsoverlay;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IARegion;
import com.indooratlas.android.sdk.examples.R;
import com.indooratlas.android.sdk.examples.SdkExample;
import com.indooratlas.android.sdk.resources.IAFloorPlan;
import com.indooratlas.android.sdk.resources.IALatLng;
import com.indooratlas.android.sdk.resources.IALocationListenerSupport;
import com.indooratlas.android.sdk.resources.IAResourceManager;
import com.indooratlas.android.sdk.resources.IAResult;
import com.indooratlas.android.sdk.resources.IAResultCallback;
import com.indooratlas.android.sdk.resources.IATask;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

@SdkExample(description = R.string.example_googlemaps_overlay_description)
public class MapsOverlayActivity extends FragmentActivity {

    private static final String TAG = "IndoorAtlasExample";

    private static final float HUE_IABLUE = 200.0f;
    // used to decide when bitmap should be downscaled
    private static final int MAX_DIMENSION = 2048;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Marker mMarker;
    private GroundOverlay groundOverlay;
    private IALocationManager mIALocationManager;
    private IAResourceManager mFloorPlanManager;
    private IATask<IAFloorPlan> mPendingAsyncResult;
    private Target loadtarget;
    private IAFloorPlan mFloorPlan;
    private boolean mCameraPositionNeedsUpdating;

    private IALocationListener mListener = new IALocationListenerSupport() {
        /**
         * Callback for receiving locations.
         * This is where location updates can be handled by moving markers or the camera.
         */
        @Override
        public void onLocationChanged(IALocation location) {
            Log.d(TAG, "location is: " + location.getLatitude() + "," + location.getLongitude());
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            if (mMarker == null) {
                if (mMap != null) {
                    mMarker = mMap.addMarker(new MarkerOptions().position(latLng)
                            .icon(BitmapDescriptorFactory.defaultMarker(HUE_IABLUE)));
                }
            } else {
                mMarker.setPosition(latLng);
            }

            if (mCameraPositionNeedsUpdating) {
                if (mMap != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17.5f));
                }
                mCameraPositionNeedsUpdating = false;
            }
        }
    };

    private IARegion.Listener mRegionListener = new IARegion.Listener() {
        @Override
        public void onEnterRegion(IARegion region) {

            if (region.getType() == IARegion.TYPE_UNKNOWN) {
                Toast.makeText(MapsOverlayActivity.this, "Moved out of map",
                        Toast.LENGTH_LONG).show();
                return;
            }

            // entering new region, mark need to move camera
            mCameraPositionNeedsUpdating = true;

            final String newId = region.getId();
            Log.d(TAG, "floorPlan changed to " + newId);
            Toast.makeText(MapsOverlayActivity.this, newId, Toast.LENGTH_SHORT).show();
            fetchFloorPlan(newId);
        }

        @Override
        public void onExitRegion(IARegion region) {
            if (mMarker != null) {
                mMarker.remove();
                mMarker = null;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // prevent the screen going to sleep while app is on foreground
        findViewById(android.R.id.content).setKeepScreenOn(true);
        // Creates IndoorAtlas location manager
        mIALocationManager = IALocationManager.create(this);
        /* optional setup of floor plan id
           if setLocation is not called, then location manager tries to find
           location automatically */
        final String floorPlanId = getString(R.string.indooratlas_floor_plan_id);
        if (floorPlanId != null && !floorPlanId.isEmpty()) {
            final IALocation FLOOR_PLAN_ID = IALocation.from(IARegion.floorPlan(floorPlanId));
            mIALocationManager.setLocation(FLOOR_PLAN_ID);
        }
        mFloorPlanManager = IAResourceManager.create(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mIALocationManager.destroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
        }
        // creates IndoorAtlas location request
        IALocationRequest request = IALocationRequest.create();
        // starts receiving location updates
        mIALocationManager.requestLocationUpdates(request, mListener);
        mIALocationManager.registerRegionListener(mRegionListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIALocationManager.removeLocationUpdates(mListener);
        mIALocationManager.registerRegionListener(mRegionListener);
    }

    /**
     * Methods for fetching floor plan data and bitmap image.
     * Method {@link #fetchFloorPlan(String id)} fetches floor plan data including URL to bitmap
     * on IndoorAtlas server. On success it calls {@link #setBitmap(String url)} which uses
     * Picasso library to fetch bitmap image. If fetching bitmap image is successful then
     * {@link #setupGroundOverlay(Bitmap bitmap)} is called to set the bitmap as ground overlay on
     * Google Maps.
     */

    // sets bitmap of floor plan as ground overlay on Google Maps
    public void setupGroundOverlay(Bitmap bitmap) {
        if (groundOverlay != null)
            groundOverlay.remove();
        if (mMap != null) {
            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
            IALatLng iaLatLng = mFloorPlan.getCenter();
            LatLng center = new LatLng(iaLatLng.latitude, iaLatLng.longitude);
            GroundOverlayOptions fpOverlay = new GroundOverlayOptions()
                    .image(bitmapDescriptor)
                    .position(center, mFloorPlan.getWidthMeters(),
                            mFloorPlan.getHeightMeters()).bearing(mFloorPlan.getBearing());

            groundOverlay = mMap.addGroundOverlay(fpOverlay);
        }
    }

    // Uses Picasso library to download and cache bitmap image
    public void setBitmap(String url) {
        if (loadtarget == null) loadtarget = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                // scale down large bitmaps
                if ((bitmap.getWidth() * bitmap.getHeight()) > (MAX_DIMENSION * MAX_DIMENSION)) {
                    int largerDimension = (bitmap.getHeight() > bitmap.getWidth()) ?
                            bitmap.getHeight() : bitmap.getWidth();
                    int scaleFactor = (largerDimension / MAX_DIMENSION) + 1;
                    Bitmap dsBitMap = Bitmap.createScaledBitmap(bitmap,
                            bitmap.getWidth() / scaleFactor, bitmap.getHeight() / scaleFactor, false);
                    setupGroundOverlay(dsBitMap);
                } else {
                    setupGroundOverlay(bitmap);
                }
                Log.d(TAG, "picasso bitmap loaded");
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                Log.d(TAG, "picasso on prepare load");
            }

            @Override
            public void onBitmapFailed(Drawable placeHolderDraweble) {
                Log.d(TAG, "picasso bitmap failed");
                Toast.makeText(MapsOverlayActivity.this, "Failed to load bitmap", Toast.LENGTH_SHORT).show();
            }
        };

        Picasso.with(this).load(url).into(loadtarget);
    }

    /**
     * Fetches floor plan data from IndoorAtlas server.
     */
    private void fetchFloorPlan(String id) {
        cancelPendingNetworkCalls();
        final IATask<IAFloorPlan> asyncResult = mFloorPlanManager.fetchFloorPlanWithId(id);
        mPendingAsyncResult = asyncResult;

        if (mPendingAsyncResult != null) {
            mPendingAsyncResult.setCallback(new IAResultCallback<IAFloorPlan>() {
                @Override
                public void onResult(IAResult<IAFloorPlan> result) {
                    Log.d(TAG, "fetch floor plan result:" + result);
                    if (result.isSuccess() && result.getResult() != null) {
                        mFloorPlan = result.getResult();
                        setBitmap(mFloorPlan.getUrl());
                    } else {
                        if (!asyncResult.isCancelled()) {
                            // do something with error
                            Toast.makeText(MapsOverlayActivity.this,
                                    "loading floor plan failed: " + result.getError(), Toast.LENGTH_LONG)
                                    .show();
                            // remove current ground overlay
                            if (groundOverlay != null) {
                                groundOverlay.remove();
                                groundOverlay = null;
                            }
                        }
                    }
                }
            }, Looper.getMainLooper()); // deliver callbacks in main thread
        }
    }

    private void cancelPendingNetworkCalls() {
        if (mPendingAsyncResult != null && !mPendingAsyncResult.isCancelled()) {
            mPendingAsyncResult.cancel();
        }
    }
}
