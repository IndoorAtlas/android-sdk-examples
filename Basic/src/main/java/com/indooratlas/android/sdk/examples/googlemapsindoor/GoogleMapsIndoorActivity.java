package com.indooratlas.android.sdk.examples.googlemapsindoor;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.IndoorBuilding;
import com.google.android.gms.maps.model.IndoorLevel;
import com.google.android.gms.maps.model.LatLng;
import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IARegion;
import com.indooratlas.android.sdk.examples.R;
import com.indooratlas.android.sdk.examples.SdkExample;

import java.util.HashMap;
import java.util.Map;

@SdkExample(description = R.string.example_googlemaps_indoor_description)
public class GoogleMapsIndoorActivity extends FragmentActivity implements
        IALocationListener, GoogleMap.OnIndoorStateChangeListener {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Circle mCircle;
    private IALocationManager mIALocationManager;
    private GoogleMapsFloorLevelMatcher mFloorLevelMatcher;
    private IndoorLevel mLastAutoActivatedLevel;

    private static final int ACTIVE_LEVEL_BLUE_DOT_COLOR = 0x601681FB;
    private static final int OTHER_LEVEL_BLUE_DOT_COLOR = 0x60808080;

    /**
     * Matches Google Maps IndoorLevels to IndoorAtlas floor plans.
     * The IndoorAtlas floor plan name should match the IndoorLevel short name
     * in Google Maps for this to work reliably. Uses floor number matching as
     * a fallback
     */
    private static class GoogleMapsFloorLevelMatcher {

        private IndoorBuilding mBuilding;
        private IALocation mLastLocation;
        private Map<String, IndoorLevel> mNameToLevel;

        void setBuilding(IndoorBuilding building) {
            mBuilding = building;
            mNameToLevel = new HashMap<>();
            for (IndoorLevel level : building.getLevels()) {
                mNameToLevel.put(level.getShortName(), level);
            }
        }

        void setIALocation(IALocation location) {
            mLastLocation = location;
        }

        boolean isIALocationOnActiveLevel() {
            IndoorLevel iaLocationLevel = findIALocationLevel();
            return iaLocationLevel != null && iaLocationLevel.equals(activeGoogleMapsLevel());
        }

        private IndoorLevel activeGoogleMapsLevel() {
            if (mBuilding == null) return null;
            return mBuilding.getLevels().get(mBuilding.getActiveLevelIndex());
        }

        private IndoorLevel findIALocationLevel() {
            if (mLastLocation == null || !mLastLocation.hasFloorLevel() || mBuilding == null) {
                return null;
            }

            // first try to match floor plan name to IndoorLevel short name
            IARegion region = mLastLocation.getRegion();
            if (region != null) {
                if (mNameToLevel.containsKey(region.getName())) {
                    return mNameToLevel.get(region.getName());
                }
            }

            // as a fall back, try to find an IndoorLevel whose short name matches the
            // floor level (number) in the IALocation
            return mNameToLevel.get("" + mLastLocation.getFloorLevel());
        }
    }

    private void showLocationCircle(LatLng center, double accuracyRadius) {
        if (mCircle == null) {
            // location can received before map is initialized, ignoring those updates
            if (mMap != null) {
                mCircle = mMap.addCircle(new CircleOptions()
                    .center(center)
                    .radius(accuracyRadius)
                    .fillColor(ACTIVE_LEVEL_BLUE_DOT_COLOR)
                    .strokeColor(0x00000000)
                    .zIndex(1.0f)
                    .visible(true));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 17.0f));
            }
        } else {
            mCircle.setCenter(center);
            mCircle.setRadius(accuracyRadius);
        }
    }

    private void updateLocationCircleColor() {
        if (mCircle != null) {
            if (mFloorLevelMatcher.isIALocationOnActiveLevel()) {
                mCircle.setFillColor(ACTIVE_LEVEL_BLUE_DOT_COLOR);
            } else {
                mCircle.setFillColor(OTHER_LEVEL_BLUE_DOT_COLOR);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mIALocationManager = IALocationManager.create(this);
        mFloorLevelMatcher = new GoogleMapsFloorLevelMatcher();
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
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            mMap.setOnIndoorStateChangeListener(this);
        }
        mIALocationManager.requestLocationUpdates(IALocationRequest.create(), this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mIALocationManager != null) {
            mIALocationManager.removeLocationUpdates(this);
        }
        if (mMap != null) {
            mMap.setOnIndoorStateChangeListener(null);
        }

    }

    public void onLocationChanged(IALocation location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        showLocationCircle(latLng, location.getAccuracy());
        mFloorLevelMatcher.setIALocation(location);
        updateLocationCircleColor();

        IndoorLevel iaLocationLevel = mFloorLevelMatcher.findIALocationLevel();
        if (iaLocationLevel != null && !iaLocationLevel.equals(mLastAutoActivatedLevel)) {
            mLastAutoActivatedLevel = iaLocationLevel;
            mLastAutoActivatedLevel.activate();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onIndoorBuildingFocused() {
    }

    @Override
    public void onIndoorLevelActivated(IndoorBuilding indoorBuilding) {
        mFloorLevelMatcher.setBuilding(indoorBuilding);
        updateLocationCircleColor();
    }
}
