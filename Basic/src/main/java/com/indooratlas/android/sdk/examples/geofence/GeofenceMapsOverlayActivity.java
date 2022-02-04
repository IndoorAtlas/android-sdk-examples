package com.indooratlas.android.sdk.examples.geofence;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.indooratlas.android.sdk.IAGeofence;
import com.indooratlas.android.sdk.IAGeofenceEvent;
import com.indooratlas.android.sdk.IAGeofenceListener;
import com.indooratlas.android.sdk.IAGeofenceRequest;
import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IARegion;
import com.indooratlas.android.sdk.examples.ListExamplesActivity;
import com.indooratlas.android.sdk.examples.R;
import com.indooratlas.android.sdk.examples.SdkExample;
import com.indooratlas.android.sdk.resources.IAFloorPlan;
import com.indooratlas.android.sdk.resources.IALatLng;
import com.indooratlas.android.sdk.resources.IALocationListenerSupport;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@SdkExample(description = R.string.example_googlemaps_overlay_geofencing_description)
public class GeofenceMapsOverlayActivity extends FragmentActivity implements LocationListener, OnMapReadyCallback, IAGeofenceListener {

    private static final String TAG = "IndoorAtlasExample";

    /* used to decide when bitmap should be downscaled */
    private static final int MAX_DIMENSION = 2048;

    private static final double GEOFENCE_RADIUS_METERS = 5.0;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Circle mCircle;
    private Marker mMarker;
    private IARegion mOverlayFloorPlan = null;
    private GroundOverlay mGroundOverlay = null;
    private IALocationManager mIALocationManager;
    private Target mLoadTarget;
    private boolean mCameraPositionNeedsUpdating = true; // update on first location
    private boolean mShowIndoorLocation = false;

    IALocation mLatestLocation = null;
    private HashMap<LatLng, IAGeofence> mGeofences = new HashMap<>();
    private HashMap<LatLng, Circle> mGeofenceCircles = new HashMap<>();
    private List<IAGeofence> mTriggeredGeofences = new ArrayList<>();

    private static int mRunningGeofenceId = 0;

    private boolean mGuideShown = false;

    @Override
    public void onGeofencesTriggered(IAGeofenceEvent event) {

        // Assume we have only one geofence (this example)
        IAGeofence geofence = event.getTriggeringGeofences().get(0);

        if (event.getGeofenceTransition() == IAGeofence.GEOFENCE_TRANSITION_ENTER) {
            mTriggeredGeofences.addAll(event.getTriggeringGeofences());
        } else {
            for (IAGeofence g : event.getTriggeringGeofences()) {
                mTriggeredGeofences.remove(g);
            }
        }

        String sb = "Geofence " + geofence.getName() + " triggered. Trigger type: " +
                ((event.getGeofenceTransition() == IAGeofence.GEOFENCE_TRANSITION_ENTER) ?
                        "ENTER" : "EXIT");

        Toast.makeText(this,
                sb,
                Toast.LENGTH_LONG).show();

        final LatLng center = new LatLng(mLatestLocation.getLatitude(), mLatestLocation.getLongitude());

        if (mShowIndoorLocation) {
            showBlueDot(center, mLatestLocation.getAccuracy(), mLatestLocation.getBearing());
        }
    }


    /**
     * Place a geofence with radius of 10 meters around specified location
     *
     * @param latLng LatLng where to put the geofence
     */
    private void placeNewGeofence(LatLng latLng) {
        // Add a circular geofence by adding points with a 5 m radius clockwise
        final double radius = GEOFENCE_RADIUS_METERS;
        final int edgeCount = 12;
        final double EARTH_RADIUS_METERS = 6.371e6;
        final double latPerMeter = 1.0 / (EARTH_RADIUS_METERS * Math.PI / 180);
        final double lonPerMeter = latPerMeter / Math.cos(Math.PI / 180.0 * latLng.latitude);

        ArrayList<double[]> edges = new ArrayList<>();
        for (int i = 0; i < edgeCount; i++) {
            double angle = -2 * Math.PI * i / edgeCount;
            double lat = latLng.latitude + radius * latPerMeter * Math.sin(angle);
            double lon = latLng.longitude + radius * lonPerMeter * Math.cos(angle);
            edges.add(new double[]{lat, lon});
        }

        String geofenceId = "My geofence " + mRunningGeofenceId++;
        Log.d(TAG, "Creating a geofence with id \"" + geofenceId + "\"");
        IAGeofence geofence = new IAGeofence.Builder()
                .withEdges(edges)
                .withId(geofenceId)
                .withName(geofenceId)
                .build();


        Log.i(TAG, "New geofence registered: " + geofence);
        mIALocationManager.addGeofences(new IAGeofenceRequest.Builder()
                .withCloudGeofences(true) // listen also geofences defined in app.indooratlas.com
                .withGeofence(geofence)
                .build(), this);

        mGeofences.put(latLng, geofence);

        Toast.makeText(this,
                "New geofence set! Listening also triggers for " +
                        "geofences defined in app.indooratlas.com",
                Toast.LENGTH_LONG).show();
    }

    private void showBlueDot(LatLng center, double accuracyRadius, double bearing) {
        if (mMap != null) {

            int dotColor = 0x31812016; // red-ish for outdoors
            if (mShowIndoorLocation) {
                dotColor = 0x201681FB;
            }

            if (mCircle == null) {
                // location can received before map is initialized, ignoring those updates

                mCircle = mMap.addCircle(new CircleOptions()
                        .center(center)
                        .radius(accuracyRadius)
                        .fillColor(dotColor)
                        .strokeColor(0x500A78DD)
                        .zIndex(1.0f)
                        .visible(true)
                        .strokeWidth(5.0f));
                mMarker = mMap.addMarker(new MarkerOptions()
                        .position(center)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_blue_dot))
                        .anchor(0.5f, 0.5f)
                        .rotation((float) bearing)
                        .flat(true));
            } else {
                // move existing markers position to received location
                mCircle.setCenter(center);
                mCircle.setRadius(accuracyRadius);
                mCircle.setFillColor(dotColor);
                mMarker.setPosition(center);
                mMarker.setRotation((float) bearing);
            }

            // Draw geofences
            for (LatLng geofence : mGeofences.keySet()) {

                Log.d(TAG, "mTriggeredGeofences.size:: " + mTriggeredGeofences.size());
                Log.d(TAG, "mGeofences.size:: " + mGeofences.size());

                int color = 0x3172d000;
                if (mTriggeredGeofences.contains(mGeofences.get(geofence))) {
                    Log.d(TAG, "geofence : " + geofence + ", is currently inside!");
                    // color the triggered geofences
                    color = 0x31fb7927;
                } else {
                    Log.d(TAG, "geofence : " + geofence + ", NOT currently inside!");
                }

                // already drawn
                if (mGeofenceCircles.containsKey(geofence)) {
                    mGeofenceCircles.get(geofence).setFillColor(color);
                } else {
                    Circle c = mMap.addCircle(new CircleOptions()
                            .center(geofence)
                            .radius(GEOFENCE_RADIUS_METERS) // hardcoded geofence radius
                            .fillColor(color)
                            .strokeColor(0x31515724)
                            .zIndex(1.0f)
                            .visible(true)
                            .strokeWidth(5.0f));

                    mGeofenceCircles.put(geofence, c);
                }
            }
        }
    }

    /**
     * Listener that handles location change events.
     */
    private IALocationListener mListener = new IALocationListenerSupport() {

        /**
         * Location changed, move marker and camera position.
         */
        @Override
        public void onLocationChanged(IALocation location) {

            if (!mGuideShown) {
                mGuideShown = true;
                Toast.makeText(GeofenceMapsOverlayActivity.this,
                        "Long-touch to add a geofence",
                        Toast.LENGTH_LONG).show();
            }

            mLatestLocation = location;

            Log.d(TAG, "new location received with coordinates: " + location.getLatitude()
                    + "," + location.getLongitude());

            if (mMap == null) {
                // location received before map is initialized, ignoring update here
                return;
            }

            final LatLng center = new LatLng(location.getLatitude(), location.getLongitude());

            if (mShowIndoorLocation) {
                showBlueDot(center, location.getAccuracy(), location.getBearing());
            }

            // our camera position needs updating if location has significantly changed
            if (mCameraPositionNeedsUpdating) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 17.5f));
                mCameraPositionNeedsUpdating = false;
            }
        }
    };

    /**
     * Listener that changes overlay if needed
     */
    private IARegion.Listener mRegionListener = new IARegion.Listener() {
        @Override
        public void onEnterRegion(IARegion region) {
            Log.d(TAG, "trace ID:" + mIALocationManager.getExtraInfo().traceId);
            if (region.getType() == IARegion.TYPE_FLOOR_PLAN) {


                if (mGroundOverlay == null || !region.equals(mOverlayFloorPlan)) {
                    mCameraPositionNeedsUpdating = true; // entering new fp, need to move camera
                    if (mGroundOverlay != null) {
                        mGroundOverlay.remove();
                        mGroundOverlay = null;
                    }
                    mOverlayFloorPlan = region; // overlay will be this (unless error in loading)
                    fetchFloorPlanBitmap(region.getFloorPlan());
                } else {
                    mGroundOverlay.setTransparency(0.0f);
                }

                mShowIndoorLocation = true;
                showInfo("Showing IndoorAtlas SDK's location output");
            }
            showInfo("Enter " + (region.getType() == IARegion.TYPE_VENUE
                    ? "VENUE "
                    : "FLOOR_PLAN ") + region.getId());
        }

        @Override
        public void onExitRegion(IARegion region) {
            if (mGroundOverlay != null) {
                // Indicate we left this floor plan but leave it there for reference
                // If we enter another floor plan, this one will be removed and another one loaded
                mGroundOverlay.setTransparency(0.5f);
            }

            mShowIndoorLocation = false;
            showInfo("Exit " + (region.getType() == IARegion.TYPE_VENUE
                    ? "VENUE "
                    : "FLOOR_PLAN ") + region.getId());
        }

    };

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (!mShowIndoorLocation) {
            Log.d(TAG, "new LocationService location received with coordinates: " + location.getLatitude()
                    + "," + location.getLongitude());

            showBlueDot(
                    new LatLng(location.getLatitude(), location.getLongitude()),
                    location.getAccuracy(),
                    location.getBearing());
        }
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // prevent the screen going to sleep while app is on foreground
        findViewById(android.R.id.content).setKeepScreenOn(true);

        // instantiate IALocationManager
        mIALocationManager = IALocationManager.create(this);

        startListeningPlatformLocations();

        // Try to obtain the map from the SupportMapFragment.
        ((SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map))
                .getMapAsync(this);
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

        // start receiving location updates & monitor region changes
        mIALocationManager.requestLocationUpdates(IALocationRequest.create(), mListener);
        mIALocationManager.registerRegionListener(mRegionListener);
        mIALocationManager.addGeofences(new IAGeofenceRequest.Builder()
                .withCloudGeofences(true) // listen geofences defined in app.indooratlas.com
                .build(), this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // unregister location & region changes
        mIALocationManager.removeLocationUpdates(mListener);
        mIALocationManager.unregisterRegionListener(mRegionListener);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (!ListExamplesActivity.checkLocationPermissions(this)) {
            finish(); // Handle permission asking in ListExamplesActivity
            return;
        }

        // do not show Google's outdoor location
        mMap.setMyLocationEnabled(false);

        // Setup long click to add a dynamic geofence
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                placeNewGeofence(latLng);
            }
        });
    }

    /**
     * Sets bitmap of floor plan as ground overlay on Google Maps
     */
    private void setupGroundOverlay(IAFloorPlan floorPlan, Bitmap bitmap) {

        if (mGroundOverlay != null) {
            mGroundOverlay.remove();
        }

        if (mMap != null) {
            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
            IALatLng iaLatLng = floorPlan.getCenter();
            LatLng center = new LatLng(iaLatLng.latitude, iaLatLng.longitude);
            GroundOverlayOptions fpOverlay = new GroundOverlayOptions()
                    .image(bitmapDescriptor)
                    .zIndex(0.0f)
                    .position(center, floorPlan.getWidthMeters(), floorPlan.getHeightMeters())
                    .bearing(floorPlan.getBearing());

            mGroundOverlay = mMap.addGroundOverlay(fpOverlay);
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
                showInfo("Failed to load bitmap");
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

    private void showInfo(String text) {
        final Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), text,
                Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.button_close, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                snackbar.dismiss();
            }
        });
        snackbar.show();
    }

    private void startListeningPlatformLocations() {
        try{
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Platform location permissions not granted",
                    Toast.LENGTH_LONG).show();
        }

    }
}
