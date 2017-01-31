package com.indooratlas.android.sdk.examples.sharelocation.view;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.examples.R;
import com.indooratlas.android.sdk.examples.sharelocation.channel.LocationEvent;
import com.indooratlas.android.sdk.examples.sharelocation.channel.LocationSource;
import com.indooratlas.android.sdk.resources.IAFloorPlan;
import com.indooratlas.android.sdk.resources.IALatLng;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.HashMap;
import java.util.Iterator;

import static com.indooratlas.android.sdk.examples.sharelocation.SharingUtils.TAG;

/**
 *
 */
public class MultiLocationMapView extends SubsamplingScaleImageView {

    private float mRadius;

    private IAFloorPlan mFloorPlan;

    private HashMap<String, LocationEntry> mKnownLocations = new HashMap<>();

    private PointF mRecyclePoint = new PointF();

    private Paint mDotPaint;

    private Paint mDotPaintAccuracy;

    private Paint mTextPaint;

    private Target mTarget;

    public MultiLocationMapView(Context context) {
        this(context, null);
    }

    public MultiLocationMapView(Context context, AttributeSet attr) {
        super(context, attr);
        initialize();
    }

    public void updateLocation(LocationEvent event) {
        if (mFloorPlan != null && isReady()) {
            updateLocationInternal(event);
            invalidate();
        }
    }

    public void setFloorPlan(IAFloorPlan floorPlan) {
        mKnownLocations.clear();
        mFloorPlan = floorPlan;
        loadBitmap(floorPlan.getUrl());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!isReady()) {
            Log.w(TAG, "drawing not yet ready, skipping drawing locations");
            return;
        }

        final float scaledRadius = getScale() * mRadius;
        Iterator<LocationEntry> iterator = mKnownLocations.values().iterator();
        while (iterator.hasNext()) {
            LocationEntry entry = iterator.next();

            final float scaledRadiusAccuracy = getScale() * (mRadius + entry.mAccuracy);

            sourceToViewCoord(entry.mPoint, mRecyclePoint);
            mDotPaint.setColor(entry.mSource.color);
            mTextPaint.setColor(entry.mSource.color);
            canvas.drawCircle(mRecyclePoint.x, mRecyclePoint.y, scaledRadius, mDotPaint);

            mDotPaintAccuracy.setColor(entry.mSource.color);
            mDotPaintAccuracy.setAlpha(50);
            canvas.drawCircle(mRecyclePoint.x, mRecyclePoint.y, scaledRadiusAccuracy, mDotPaintAccuracy);

            canvas.drawText(entry.mSource.name,
                    mRecyclePoint.x + (scaledRadius * 1.2f),
                    mRecyclePoint.y + (scaledRadius / 2),
                    mTextPaint);
        }

    }

    private void initialize() {
        setWillNotDraw(false);
        setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_CENTER);
        mRadius = getResources().getDimensionPixelSize(R.dimen.dot_radius);
        mDotPaint = new Paint();
        mDotPaint.setAntiAlias(true);
        mDotPaint.setStyle(Paint.Style.FILL);
        mDotPaintAccuracy = new Paint();
        mDotPaintAccuracy.setAntiAlias(true);
        mDotPaintAccuracy.setStyle(Paint.Style.FILL);
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(getResources().getDimensionPixelSize(R.dimen.location_text_size));
    }

    private void loadBitmap(String url) {

        // hold strong reference into target so that it does not get GC'd
        mTarget = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                setImage(ImageSource.bitmap(bitmap.copy(bitmap.getConfig(), true)));
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
            }
        };

        Picasso.with(getContext())
                .load(url)
                .into(mTarget);

    }

    private void updateLocationInternal(LocationEvent event) {

        final String identity = event.source.id;
        final IALocation location = event.location;

        IALatLng latLng = new IALatLng(location.getLatitude(), location.getLongitude());
        PointF point = mFloorPlan.coordinateToPoint(latLng);
        float accuracy = location.getAccuracy() * mFloorPlan.getMetersToPixels();

        Log.d(TAG, "converted location ("
                + event.location.getLatitude() + "," + event.location.getLongitude()
                + ") to point: " + point
                + " with floorplan: " + mFloorPlan.getId()
                + ", name: " + mFloorPlan.getName());

        mKnownLocations.put(identity, new LocationEntry(event.source, point, accuracy));
    }

    static class LocationEntry {

        PointF mPoint;

        float mAccuracy;

        LocationSource mSource;

        LocationEntry(LocationSource source, PointF point, float accuracy) {
            mPoint = point;
            mAccuracy = accuracy;
            mSource = source;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("LocationEntry{");
            sb.append("mPoint=").append(mPoint);
            sb.append("mAccuracy=").append(mAccuracy);
            sb.append(", mSource=").append(mSource);
            sb.append('}');
            return sb.toString();
        }
    }
}
