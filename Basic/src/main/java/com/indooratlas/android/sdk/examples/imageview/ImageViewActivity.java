package com.indooratlas.android.sdk.examples.imageview;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.davemorrissey.labs.subscaleview.ImageSource;
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

import java.io.File;

@SdkExample(description = R.string.example_imageview_description)
public class ImageViewActivity extends FragmentActivity {
    // blue dot radius in meters
    private static final float dotRadius = 1.0f;
    String floorPlanId;

    private static final String TAG = "IndoorAtlasExample";

    public IALocationManager mIALocationManager;
    public IAResourceManager mFloorPlanManager;
    private IATask<IAFloorPlan> mPendingAsyncResult;
    private IAFloorPlan mFloorPlan;
    public BlueDotView imageView;
    long downloadId;
    DownloadManager downloadManager;


    private IALocationListener mLocationListener = new IALocationListenerSupport() {
        @Override
        public void onLocationChanged(IALocation location) {
            Log.d(TAG, "location is: " + location.getLatitude() + "," + location.getLongitude());
            if (imageView != null && imageView.isReady()) {
                IALatLng latLng = new IALatLng(location.getLatitude(), location.getLongitude());
                PointF point = mFloorPlan.coordinateToPoint(latLng);
                imageView.setDotCenter(point);
                imageView.postInvalidate();
            }
        }
    };

    private IARegion.Listener mRegionListener = new IARegion.Listener() {

        @Override
        public void onEnterRegion(IARegion region) {
            if (region.getType() == IARegion.TYPE_FLOOR_PLAN) {
                String id = region.getId();
                Log.d(TAG, "floorPlan changed to " + id);
                Toast.makeText(ImageViewActivity.this, id, Toast.LENGTH_SHORT).show();
                fetchFloorPlan(id);
            }
        }

        @Override
        public void onExitRegion(IARegion region) {
            // leaving a previously entered region
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);
        // prevent the screen going to sleep while app is on foreground
        findViewById(android.R.id.content).setKeepScreenOn(true);

        imageView = (BlueDotView) findViewById(R.id.imageView);

        // Creates IndoorAtlas location manager
        mIALocationManager = IALocationManager.create(this);
        /* optional setup of floor plan id
           if setLocation is not called, then location manager tries to find
           location automatically */
        floorPlanId = getResources().getString(R.string.indooratlas_floor_plan_id);
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

        // creates IndoorAtlas location request
        IALocationRequest request = IALocationRequest.create();
        // starts receiving location updates
        mIALocationManager.requestLocationUpdates(request, mLocationListener);
        mIALocationManager.registerRegionListener(mRegionListener);
        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIALocationManager.removeLocationUpdates(mLocationListener);
        mIALocationManager.unregisterRegionListener(mRegionListener);
        unregisterReceiver(onComplete);
    }

    /**
     * Methods for fetching floor plan data and bitmap image.
     * Method {@link #fetchFloorPlan(String id)} fetches floor plan data including URL to bitmap
     */

     /*  Broadcast receiver for floor plan image download */
    private BroadcastReceiver onComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
            if (id != downloadId) {
                Log.w(TAG, "Ignore unrelated download");
                return;
            }
            Log.w(TAG, "Image download completed");
            Bundle extras = intent.getExtras();
            DownloadManager.Query q = new DownloadManager.Query();
            q.setFilterById(extras.getLong(DownloadManager.EXTRA_DOWNLOAD_ID));
            Cursor c = downloadManager.query(q);

            if (c.moveToFirst()) {
                int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    // process download
                    String filePath = c.getString(c.getColumnIndex(
                        DownloadManager.COLUMN_LOCAL_FILENAME));
                    showFloorPlanImage(filePath);
                }
            }
            c.close();
        }
    };

    private void showFloorPlanImage(String filePath) {
        Log.w(TAG, "showFloorPlanImage: " + filePath);
        imageView.setRadius(mFloorPlan.getMetersToPixels() * dotRadius);
        imageView.setImage(ImageSource.uri(filePath));
    }

    // fetches floor plan data from IndoorAtlas server
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
                        String fileName = mFloorPlan.getId() + ".img";
                        String filePath = Environment.getExternalStorageDirectory() + "/"
                            + Environment.DIRECTORY_DOWNLOADS + "/" + fileName;
                        File file = new File(filePath);
                        if (!file.exists()) {
                            DownloadManager.Request request =
                                new DownloadManager.Request(Uri.parse(mFloorPlan.getUrl()));
                            request.setDescription("IndoorAtlas floor plan");
                            request.setTitle("Floor plan");
                            // requires android 3.2 or later to compile
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                                request.allowScanningByMediaScanner();
                                request.setNotificationVisibility(DownloadManager.
                                    Request.VISIBILITY_HIDDEN);
                            }
                            request.setDestinationInExternalPublicDir(Environment.
                                DIRECTORY_DOWNLOADS, fileName);

// get download service and enqueue file
                            downloadManager = (DownloadManager) getSystemService(Context.
                                DOWNLOAD_SERVICE);
                            downloadId = downloadManager.enqueue(request);
                        } else {
                            showFloorPlanImage(filePath);
                        }
                    } else {
                        // do something with error
                        if (!asyncResult.isCancelled()) {
                            Toast.makeText(ImageViewActivity.this,
                                (result.getError() != null
                                    ? "error loading floor plan: " + result.getError()
                                    : "access to floor plan denied"), Toast.LENGTH_LONG)
                                .show();
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

