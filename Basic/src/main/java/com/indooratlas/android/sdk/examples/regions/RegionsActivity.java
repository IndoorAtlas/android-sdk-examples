package com.indooratlas.android.sdk.examples.regions;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.view.animation.AnimationUtils;
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
    TextView mUiVenueId;
    TextView mUiFloorPlan;
    TextView mUiFloorPlanId;
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
        mUiVenueId = (TextView) findViewById(R.id.text_view_venue_id);
        mUiFloorPlan = (TextView) findViewById(R.id.text_view_floor_plan);
        mUiFloorPlanId = (TextView) findViewById(R.id.text_view_floor_plan_id);
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
        String venueId = "";
        String floorPlan = "";
        String floorPlanId = "";
        String level = "";
        String certainty = "";
        if (mCurrentVenue != null) {
            venue = getString(R.string.venue_inside);
            venueId = mCurrentVenue.getId();
            if (mCurrentFloorPlan != null) {
                floorPlan = getString(R.string.floor_plan_inside);
                floorPlanId = mCurrentFloorPlan.getId();
            } else {
                floorPlan = getString(R.string.floor_plan_outside);
            }
        }
        if (mCurrentFloorLevel != null) {
            level = mCurrentFloorLevel.toString();
        }
        if (mCurrentCertainty != null) {
            certainty = getString(R.string.floor_certainty_percentage, mCurrentCertainty * 100.0f);
        }
        setText(mUiVenue, venue, true);
        setText(mUiVenueId, venueId, true);
        setText(mUiFloorPlan, floorPlan, true);
        setText(mUiFloorPlanId, floorPlanId, true);
        setText(mUiFloorLevel, level, true);
        setText(mUiFloorCertainty, certainty, false); // do not animate as changes can be frequent
    }

    /**
     * Set the text of a TextView and make a animation to notify when the value has changed
     */
    void setText(@NonNull TextView view, @NonNull String text, boolean animateWhenChanged) {
        if (!view.getText().toString().equals(text)) {
            view.setText(text);
            if (animateWhenChanged) {
                view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.notify_change));
            }
        }
    }

}
