package com.indooratlas.android.sdk.examples.imageview_animate;


import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.examples.R;

/**
 * Extends great ImageView library by Dave Morrissey. See more:
 * https://github.com/davemorrissey/subsampling-scale-image-view.
 */
public class BlueDotAnimateView extends SubsamplingScaleImageView {

    private float radius = 0.5f;
    private PointF dotCenter = null;

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public void setDotCenter(PointF dotCenter) {
        setLocation(dotCenter);
    }

    private static final String TAG = "BlueSphereHelper";

    private PointF mCurrentLocation;

    private ValueAnimator mLocationChangeAnimator;

    private Interpolator mInterpolator;

    public BlueDotAnimateView(Context context) {
        this(context, null);
    }

    Paint mPaint;

    public BlueDotAnimateView(Context context, AttributeSet attr) {
        super(context, attr);
        initialise();
    }

    private void initialise() {
        setWillNotDraw(false);
        setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_CENTER);
        mInterpolator = new AccelerateDecelerateInterpolator();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(getResources().getColor(R.color.ia_blue));

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!isReady()) {
            return;
        }

        if (dotCenter != null) {
            PointF vPoint = sourceToViewCoord(dotCenter);
            float scaledRadius = getScale() * radius;
            canvas.drawCircle(vPoint.x, vPoint.y, scaledRadius, mPaint);
        }
    }

    void applyLocation(float x, float y) {
        this.invalidate();
        dotCenter = new PointF(x, y);
    }

    void setLocation(PointF location) {

        if (mCurrentLocation != null) {

            if (mLocationChangeAnimator != null) {
                mLocationChangeAnimator.end();
            }

            mLocationChangeAnimator = ValueAnimator.ofFloat(0, 1);
            mLocationChangeAnimator.setDuration(500);
            mLocationChangeAnimator.setInterpolator(mInterpolator);
            mLocationChangeAnimator.addUpdateListener(new LocationAnimator(mCurrentLocation,
                    location));
            mLocationChangeAnimator.start();



        } else {
            applyLocation(location.x, location.y);
        }
        mCurrentLocation = location;
    }

    private class LocationAnimator implements ValueAnimator.AnimatorUpdateListener {

        private float mAnimateDeltaX;
        private float mAnimateDeltaY;
        private PointF mFromLocation;

        private LocationAnimator(PointF fromLocation, PointF toLocation) {
            mFromLocation = fromLocation;
            mAnimateDeltaX = toLocation.x - fromLocation.x;
            mAnimateDeltaY = toLocation.y - fromLocation.y;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            float fraction = valueAnimator.getAnimatedFraction();

            float newX = (mFromLocation.x + (mAnimateDeltaX * fraction));
            float newY = (mFromLocation.y + (mAnimateDeltaY * fraction));

            applyLocation(newX, newY);
        }
    }
}
