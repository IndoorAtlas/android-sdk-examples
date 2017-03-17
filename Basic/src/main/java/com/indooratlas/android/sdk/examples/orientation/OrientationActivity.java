package com.indooratlas.android.sdk.examples.orientation;

import android.opengl.GLSurfaceView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IAOrientationListener;
import com.indooratlas.android.sdk.IAOrientationRequest;
import com.indooratlas.android.sdk.examples.R;
import com.indooratlas.android.sdk.examples.SdkExample;

@SdkExample(description = R.string.example_orientation_description)
public class OrientationActivity extends AppCompatActivity implements IALocationListener,
        IAOrientationListener {

    GLSurfaceView mGlView;
    OrientationRenderer mRenderer;

    TextView mTextBearing;
    TextView mTextHeading;
    TextView mTextOrientation;

    IALocationManager mManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orientation);

        mGlView = (GLSurfaceView) findViewById(R.id.gl_view);
        mGlView.setEGLContextClientVersion(2);
        mRenderer = new OrientationRenderer(this, R.raw.panorama);
        mGlView.setRenderer(mRenderer);
        mGlView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        mTextBearing = (TextView) findViewById(R.id.text_bearing);
        mTextHeading = (TextView) findViewById(R.id.text_heading);
        mTextOrientation = (TextView) findViewById(R.id.text_orientation);

        mManager = IALocationManager.create(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mManager.requestLocationUpdates(IALocationRequest.create(), this);
        // trigger heading and orientation updates when they have changed by 5 degrees
        mManager.registerOrientationListener(new IAOrientationRequest(5.0, 5.0), this);
    }

    @Override
    protected void onPause() {
        mManager.unregisterOrientationListener(this);
        mManager.removeLocationUpdates(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mManager.destroy();
        super.onDestroy();
    }

    @Override
    public void onLocationChanged(IALocation iaLocation) {
        mTextBearing.setText(getString(R.string.text_bearing, iaLocation.getBearing()));
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onHeadingChanged(long timestamp, double heading) {
        mTextHeading.setText(getString(R.string.text_heading, heading));
    }

    @Override
    public void onOrientationChange(long timestamp, double[] orientation) {
        mTextOrientation.setText(getString(R.string.text_orientation, orientation[0],
                orientation[1], orientation[2], orientation[3]));
        mRenderer.setOrientation(orientation);
        mGlView.requestRender();
    }
}
