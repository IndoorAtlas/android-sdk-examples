package com.indooratlas.android.sdk.examples.ar.wrapper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.opengl.Matrix;
import android.util.Log;

import com.huawei.hiar.ARCamera;
import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.AREnginesApk;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARLightEstimate;
import com.huawei.hiar.ARPlane;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARTrackable;
import com.huawei.hiar.ARWorldTrackingConfig;
import com.huawei.hiar.exceptions.ARCameraNotAvailableException;
import com.huawei.hiar.exceptions.ARUnSupportedConfigurationException;
import com.huawei.hiar.exceptions.ARUnavailableClientSdkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceNotInstalledException;
import com.indooratlas.android.sdk.examples.ar.helpers.CameraPermissionHelper;
import com.indooratlas.android.sdk.examples.ar.rendering.ObjectRenderer;

import java.util.ArrayList;
import java.util.List;

// See https://github.com/HMS-Core/hms-AREngine-demo/blob/master/HwAREngineDemo/src/main/java/com/huawei/arengine/demos/java/world/WorldActivity.java

public class ArEngine implements Api {
    private static final String TAG = ArEngine.class.getSimpleName();
    private ARSession session;
    private ARFrame frame;
    private ARCamera camera;
    private boolean isRemindInstall = false;
    // Transforms from IMU (Android Sensor coordinates) to the coordinate system used by
    // AR Engine camera.getPose(). Column-major.
    private static final float[] LOCAL_COORD_TRANS = new float[]{
            0, 1, 0, 0,  -1, 0, 0, 0,  0, 0, 1, 0,  0, 0, 0, 1
    };

    private float[] tmpMatrix = new float[16];

    private TextureDisplay backgroundRenderer = new TextureDisplay();

    @Override
    public ResumeResult handleInstallFlowAndResume(Activity activity) {
        if (session == null) {
            try {
                ResumeResult result = arEngineAbilityCheck(activity);
                if (result.status != ResumeResult.Status.SUCCESS) return result;

                if (!CameraPermissionHelper.hasCameraPermission(activity)) {
                    CameraPermissionHelper.requestCameraPermission(activity);
                    return new ResumeResult(ResumeResult.Status.PENDING);
                }

                session = new ARSession(activity);
                ARWorldTrackingConfig config = new ARWorldTrackingConfig(session);
                config.setFocusMode(ARConfigBase.FocusMode.AUTO_FOCUS);
                config.setSemanticMode(ARWorldTrackingConfig.SEMANTIC_PLANE);
                session.configure(config);
            } catch (Exception capturedException) {
                ResumeResult result = setMessageWhenError(activity, capturedException);
                if (result.status == ResumeResult.Status.ERROR) {
                    stopArSession();
                }
                return result;
            }
        }
        try {
            session.resume();
        } catch (ARCameraNotAvailableException e) {
            session = null;
            return new ResumeResult("Camera open failed, please restart the app");
        }
        return new ResumeResult(ResumeResult.Status.SUCCESS);
    }

    private void stopArSession() {
        Log.i(TAG, "stopArSession start.");
        if (session != null) {
            session.stop();
            session = null;
        }
        Log.i(TAG, "stopArSession end.");
    }

    private ResumeResult arEngineAbilityCheck(Context context) {
        boolean isInstallArEngineApk = AREnginesApk.isAREngineApkReady(context);
        if (!isInstallArEngineApk && isRemindInstall) {
            return new ResumeResult("Please agree to install.");
        }
        Log.d(TAG, "Is Install AR Engine Apk: " + isInstallArEngineApk);
        if (!isInstallArEngineApk) {
            context.startActivity(new Intent(context, ConnectAppMarketActivity.class));
            isRemindInstall = true;
            return new ResumeResult(ResumeResult.Status.PENDING);
        }
        if (AREnginesApk.isAREngineApkReady(context)) {
            return new ResumeResult(ResumeResult.Status.SUCCESS);
        }
        return new ResumeResult("AR Engine not available");
    }

    private ResumeResult setMessageWhenError(Context context, Exception catchException) {
        Log.w(TAG, "Creating session error", catchException);
        if (catchException instanceof ARUnavailableServiceNotInstalledException) {
            context.startActivity(new Intent(context, ConnectAppMarketActivity.class));
            return new ResumeResult(ResumeResult.Status.PENDING);
        } else if (catchException instanceof ARUnavailableServiceApkTooOldException) {
            return new ResumeResult("Please update HuaweiARService.apk");
        } else if (catchException instanceof ARUnavailableClientSdkTooOldException) {
            return new ResumeResult("Please update this app");
        } else if (catchException instanceof ARUnSupportedConfigurationException) {
            return new ResumeResult("The configuration is not supported by the device!");
        } else {
            return new ResumeResult("exception throw");
        }
    }

    @Override
    public boolean isRunning() {
        return session != null;
    }

    @Override
    public void pause() {
        session.pause();
    }

    @Override
    public void onFrame() {
        session.setCameraTextureName(backgroundRenderer.getExternalTextureId());

        // Obtain the current frame from ARSession. When the configuration is set to
        // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
        // camera framerate.
        frame = session.update();
        camera = frame.getCamera();

        // no depth API

        backgroundRenderer.onDrawFrame(frame);
    }

    @Override
    public void renderPointCloud(float[] viewmtx, float[] projmtx) {
        // TODO
    }

    @Override
    public void renderPlanes(float[] projmtx) {
        // TODO
    }

    @Override
    public float[] getUpdatedUvTransformMatrix() {
        return null; // TODO
    }

    @Override
    public boolean getColorCorrection(float[] rgba) {
        ARLightEstimate lightEstimate = frame.getLightEstimate();
        float lightPixelIntensity = 1;
        if (lightEstimate.getState() != ARLightEstimate.State.NOT_VALID) {
            lightPixelIntensity = lightEstimate.getPixelIntensity();
            // note: brightness only instead of RGB(A) in ARCore
            for (int i = 0; i < 3; ++i) rgba[i] = lightPixelIntensity;
            rgba[3] = 1;
            return true;
        }
        return false;
    }

    @Override
    public void getViewMatrix(float[] mat) {
        camera.getViewMatrix(mat, 0);
    }

    @Override
    public void getProjectionMatrix(float[] mat, float nearClip, float farClip) {
        camera.getProjectionMatrix(mat, 0, nearClip, farClip);
    }

    @Override
    public void getCameraToWorldMatrix(float[] mat) {
        camera.getPose().toMatrix(mat, 0);
    }

    @Override
    public void getImuToWorldMatrix(float[] mat) {
        camera.getPose().toMatrix(tmpMatrix, 0);
        Matrix.multiplyMM(mat, 0, tmpMatrix, 0, LOCAL_COORD_TRANS, 0);
    }

    @Override
    public List<HorizontalPlane> getHorizontalPlanes() {
        List<HorizontalPlane> planeList = new ArrayList<>();
        for (ARPlane plane : session.getAllTrackables(ARPlane.class)) {
            if (plane.getType() == ARPlane.PlaneType.HORIZONTAL_UPWARD_FACING
                    && plane.getTrackingState() == ARTrackable.TrackingState.TRACKING) {
                HorizontalPlane h = new HorizontalPlane();
                // note: slightly different convention from ARCore
                h.xyz = new float[3];
                plane.getCenterPose().getTranslation(h.xyz, 0);
                h.extentX = plane.getExtentX();
                h.extentZ = plane.getExtentZ();
                planeList.add(h);
            }
        }
        return planeList;
    }

    @Override
    public void onSurfaceCreated(Context context) {
        backgroundRenderer.init();
        // TODO: plane and point cloud renderer
    }

    @Override
    public void setupObject(Context context, ObjectRenderer obj) {
        // no depth API -> do nothing
    }

    @Override
    public void setDisplayGeometry(int rot, int w, int h) {
        session.setDisplayGeometry(rot, w, h);
        // note: this should be called from the GL thread
        backgroundRenderer.onSurfaceChanged(w, h);
    }

    @Override
    public String getTrackingFailureReasonString() {
        if (camera.getTrackingState() == ARTrackable.TrackingState.TRACKING) return null;
        return "Lost tracking"; // TODO
    }

    @Override
    public boolean shouldKeepScreenOn() {
        switch (camera.getTrackingState()) {
            case PAUSED:
            case STOPPED:
                return false;
            case TRACKING:
                return true;
        }
        return false;
    }
}
