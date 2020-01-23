package com.indooratlas.android.sdk.examples.osmdroid;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import com.indooratlas.android.sdk.resources.IAFloorPlan;
import com.indooratlas.android.sdk.resources.IALatLng;
import com.indooratlas.android.sdk.resources.IALocationListenerSupport;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import org.osmdroid.bonuspack.overlays.GroundOverlay;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;

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

    private RotationGestureOverlay mRotationGestureOverlay;
    private CopyrightOverlay mCopyrightOverlay;

    private IALocationManager mIALocationManager;
    private Target mLoadTarget;
    private boolean mCameraPositionNeedsUpdating = true; // update on first location

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

            // our camera position needs updating if location has significantly changed

            if (mCameraPositionNeedsUpdating) {
                //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17.5f));
                mOsmv.getController().setCenter(geoPoint);
                mCameraPositionNeedsUpdating = false;
            }

            mOsmv.invalidate();
        }
    };

    /**
     * Listener that changes overlay if needed
     */
    private IARegion.Listener mRegionListener = new IARegion.Listener() {

        @Override
        public void onEnterRegion(IARegion region) {
            if (region.getType() == IARegion.TYPE_FLOOR_PLAN) {
                // Are we entering a new floor plan or coming back the floor plan we just left?
                if (mGroundOverlay == null || !region.equals(mOverlayFloorPlan)) {
                    mCameraPositionNeedsUpdating = true; // entering new fp, need to move camera
                    if (mGroundOverlay != null) {
                        mOsmv.getOverlayManager().remove(mGroundOverlay);
                        mGroundOverlay = null;
                    }
                    mOverlayFloorPlan = region; // overlay will be this (unless error in loading)
                    fetchFloorPlanBitmap(region.getFloorPlan());
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

        // instantiate IALocationManager
        mIALocationManager = IALocationManager.create(this);
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

        // enable indoor-outdoor mode, required since SDK 3.2
        mIALocationManager.lockIndoors(false);
        // start receiving location updates & monitor region changes
        mIALocationManager.requestLocationUpdates(IALocationRequest.create(), mListener);
        mIALocationManager.registerRegionListener(mRegionListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // unregister location & region changes
        mIALocationManager.removeLocationUpdates(mListener);
        mIALocationManager.unregisterRegionListener(mRegionListener);
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

    private void initOsmMapView(){
        if (mOsmv == null) {

            mOsmv = new MapView(this);
            mOsmv.setTilesScaledToDpi(true);

            // Sets up the user agent to prevent being banned from OSM servers:
            // load(...) sets up default values, such as populating userAgent with packageName
            Configuration.getInstance().load(
                    this, PreferenceManager.getDefaultSharedPreferences(this));

            mOsmv.getController().setZoom(18.0);

            // Enable zoom and rotation controls
            mRotationGestureOverlay = new RotationGestureOverlay(mOsmv);
            mRotationGestureOverlay.setEnabled(true);
            mOsmv.getOverlayManager().add(mRotationGestureOverlay);
            mOsmv.setMultiTouchControls(true);
            // gestures fill this role
            mOsmv.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);

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
     * Download floor plan using Picasso library.
     */
    private void fetchFloorPlanBitmap(final IAFloorPlan floorPlan) {

        final String url = floorPlan.getUrl();
        mLoadTarget = new Target() {

            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                Log.d(TAG, "onBitmap loaded with dimensions: " + bitmap.getWidth() + "x"
                        + bitmap.getHeight());
                if (mOverlayFloorPlan != null && floorPlan.getId().equals(mOverlayFloorPlan.getId())) {
                    setupGroundOverlay(floorPlan, bitmap);
                }
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                // N/A
            }

            @Override
            public void onBitmapFailed(Drawable placeHolderDrawable) {
                Toast.makeText(OpenStreetMapOverlay.this, "Failed to load bitmap",
                        Toast.LENGTH_SHORT).show();
                mOverlayFloorPlan = null;
            }
        };

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
                    Toast.makeText(this, R.string.storage_permission_denied_message_osm,
                            Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

}
