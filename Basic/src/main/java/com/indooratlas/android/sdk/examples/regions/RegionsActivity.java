package com.indooratlas.android.sdk.examples.regions;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.TextView;

import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IARegion;
import com.indooratlas.android.sdk.examples.R;

import com.indooratlas.android.sdk.examples.SdkExample;

/**
 * Demonstrates automatic region transitions and floor level certainty
 */
@SdkExample(description = R.string.example_regions_description)
public class RegionsActivity extends FragmentActivity implements IALocationListener,
        IARegion.Listener {


    IALocationManager mManager;
    IARegion mCurrentVenue = null;
    IARegion mCurrentFloorPlan = null;
    Integer mCurrentFloorLevel = null;
    Float mCurrentCertainty = null;

    TextView mUiVenue;
    TextView mUiFloorPlan;
    TextView mUiFloorLevel;
    TextView mUiFloorCertainty;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_regions);

        mManager = IALocationManager.create(this);
        mManager.registerRegionListener(this);
        mManager.requestLocationUpdates(IALocationRequest.create(), this);

        mUiVenue = (TextView) findViewById(R.id.text_view_venue);
        mUiFloorPlan = (TextView) findViewById(R.id.text_view_floor_plan);
        mUiFloorLevel = (TextView) findViewById(R.id.text_view_floor_level);
        mUiFloorCertainty = (TextView) findViewById(R.id.text_view_floor_certainty);

        updateUi();
    }

    @Override
    protected void onDestroy() {
        mManager.destroy();
        super.onDestroy();
    }

    @Override
    public void onLocationChanged(IALocation iaLocation) {
        mCurrentFloorLevel = iaLocation.hasFloorLevel() ? iaLocation.getFloorLevel() : null;
        mCurrentCertainty = iaLocation.hasFloorCertainty() ? iaLocation.getFloorCertainty() : null;
        updateUi();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onEnterRegion(IARegion iaRegion) {
        if (iaRegion.getType() == IARegion.TYPE_VENUE) {
            mCurrentVenue = iaRegion;
        } else if (iaRegion.getType() == IARegion.TYPE_FLOOR_PLAN) {
            mCurrentFloorPlan = iaRegion;
        }
        updateUi();
    }

    @Override
    public void onExitRegion(IARegion iaRegion) {
        if (iaRegion.getType() == IARegion.TYPE_VENUE) {
            mCurrentVenue = iaRegion;
        } else if (iaRegion.getType() == IARegion.TYPE_FLOOR_PLAN) {
            mCurrentFloorPlan = iaRegion;
        }
        updateUi();
    }

    void updateUi() {
        String venue = getString(R.string.venue_outside);
        String floorPlan = "";
        String level = getString(R.string.floor_level_not_given);
        String certainty = getString(R.string.floor_certainty_not_given);
        if (mCurrentVenue != null) {
            venue = getString(R.string.venue_inside, mCurrentVenue.getId());
            if (mCurrentFloorPlan != null) {
                floorPlan = getString(R.string.floor_plan_inside, mCurrentFloorPlan.getId());
            } else {
                floorPlan = getString(R.string.floor_plan_outside);
            }
        }
        if (mCurrentFloorLevel != null) {
            level = getString(R.string.floor_level_is, mCurrentFloorLevel);
        }
        if (mCurrentCertainty != null) {
            certainty = getString(R.string.floor_certainty_is, mCurrentCertainty * 100.0f);
        }
        mUiVenue.setText(venue);
        mUiFloorPlan.setText(floorPlan);
        mUiFloorLevel.setText(level);
        mUiFloorCertainty.setText(certainty);
    }
    
}
