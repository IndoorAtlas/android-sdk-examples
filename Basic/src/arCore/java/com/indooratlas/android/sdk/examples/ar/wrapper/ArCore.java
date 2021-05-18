package com.indooratlas.android.sdk.examples.ar.wrapper;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Session;

import com.google.ar.core.TrackingFailureReason;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.indooratlas.android.sdk.examples.ar.helpers.CameraPermissionHelper;
import com.indooratlas.android.sdk.examples.ar.helpers.TrackingStateHelper;
import com.indooratlas.android.sdk.examples.ar.rendering.BackgroundRenderer;
import com.indooratlas.android.sdk.examples.ar.rendering.ObjectRenderer;
import com.indooratlas.android.sdk.examples.ar.rendering.PlaneRenderer;
import com.indooratlas.android.sdk.examples.ar.rendering.PointCloudRenderer;
import com.indooratlas.android.sdk.examples.ar.rendering.Texture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ArCore implements Api {
    private static final String TAG = Api.class.getSimpleName();

    private Session session;
    private Frame frame;
    private Camera camera;
    private float[] newUvTransforms = null;

    private boolean installRequested = false;
    private boolean calculateUVTransform = true;
    private final Texture depthTexture = new Texture();
    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final PlaneRenderer planeRenderer = new PlaneRenderer();
    private final PointCloudRenderer pointCloudRenderer = new PointCloudRenderer();

    // The depth API can sometimes look cool, but it can also DOUBLE THE BATTERY CONSUMPTION!
    private static final boolean USE_ARCORE_DEPTH_API = false;

    @Override
    public ResumeResult handleInstallFlowAndResume(Activity activity) {
        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(activity, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return new ResumeResult(ResumeResult.Status.PENDING);
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(activity)) {
                    CameraPermissionHelper.requestCameraPermission(activity);
                    return new ResumeResult(ResumeResult.Status.PENDING);
                }

                // Create the session.
                session = new Session(/* context= */ activity);
                Config config = session.getConfig();
                if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC) && USE_ARCORE_DEPTH_API) {
                    config.setDepthMode(Config.DepthMode.AUTOMATIC);
                } else {
                    config.setDepthMode(Config.DepthMode.DISABLED);
                }
                session.configure(config);
            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (UnavailableDeviceNotCompatibleException e) {
                message = "This device does not support AR";
                exception = e;
            } catch (Exception e) {
                message = "Failed to create AR session";
                exception = e;
            }

            if (message != null) {
                Log.e(TAG, "Exception creating session", exception);
                return new ResumeResult(message);
            }
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session.resume();
        } catch (CameraNotAvailableException e) {
            session = null;
            return new ResumeResult("Camera not available. Try restarting the app.");
        }
        return new ResumeResult(ResumeResult.Status.SUCCESS);
    }

    @Override
    public void pause() {
        session.pause();
    }

    @Override
    public boolean isRunning() {
        return session != null;
    }

    @Override
    public void onSurfaceCreated(Context context) throws IOException {
        // Create the texture and pass it to ARCore session to be filled during update().
        depthTexture.createOnGlThread();
        backgroundRenderer.createOnGlThread(/*context=*/ context, depthTexture.getTextureId());
        planeRenderer.createOnGlThread(/*context=*/ context, "models/trigrid.png");
        pointCloudRenderer.createOnGlThread(/*context=*/ context);
    }

    @Override
    public void setupObject(Context context, ObjectRenderer obj) throws IOException {
        obj.setDepthTexture(depthTexture.getTextureId(), depthTexture.getWidth(), depthTexture.getHeight());
        obj.setUseDepthForOcclusion(context, USE_ARCORE_DEPTH_API);
    }

    @Override
    public void setDisplayGeometry(int rot, int w, int h) {
        session.setDisplayGeometry(rot, w, h);
    }

    @Override
    public String getTrackingFailureReasonString() {
        if (camera.getTrackingState() != TrackingState.PAUSED) return null;

        TrackingFailureReason reason = camera.getTrackingFailureReason();
        switch (reason) {
            case NONE:
                return "";
            case BAD_STATE:
                return TrackingStateHelper.BAD_STATE_MESSAGE;
            case INSUFFICIENT_LIGHT:
                return TrackingStateHelper.INSUFFICIENT_LIGHT_MESSAGE;
            case EXCESSIVE_MOTION:
                return TrackingStateHelper.EXCESSIVE_MOTION_MESSAGE;
            case INSUFFICIENT_FEATURES:
                return TrackingStateHelper.INSUFFICIENT_FEATURES_MESSAGE;
            case CAMERA_UNAVAILABLE:
                return TrackingStateHelper.CAMERA_UNAVAILABLE_MESSAGE;
        }
        return "Unknown tracking failure reason: " + reason;
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

    @Override
    public void onFrame() {
        try {
            session.setCameraTextureName(backgroundRenderer.getTextureId());
            newUvTransforms = null;

            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            frame = session.update();
            camera = frame.getCamera();

            if (frame.hasDisplayGeometryChanged() || calculateUVTransform) {
                // The UV Transform represents the transformation between screenspace in normalized units
                // and screenspace in units of pixels.  Having the size of each pixel is necessary in the
                // virtual object shader, to perform kernel-based blur effects.
                calculateUVTransform = false;
                newUvTransforms = getTextureTransformMatrix(frame);
            }

            if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC) && USE_ARCORE_DEPTH_API) {
                depthTexture.updateWithDepthImageOnGlThread(frame);
            }

            // If frame is ready, render camera preview image to the GL surface.
            final boolean SHOW_DEPTH_IMAGE = false;
            backgroundRenderer.draw(frame, SHOW_DEPTH_IMAGE);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void renderPointCloud(float[] viewmtx, float[] projmtx) {
        try (PointCloud pointCloud = frame.acquirePointCloud()) {
            pointCloudRenderer.update(pointCloud);
            pointCloudRenderer.draw(viewmtx, projmtx);
        }
    }

    @Override
    public void renderPlanes(float[] projmtx) {
        planeRenderer.drawPlanes(
                session.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projmtx);
    }

    @Override
    public float[] getUpdatedUvTransformMatrix() {
        return newUvTransforms;
    }

    @Override
    public boolean getColorCorrection(float[] colorCorrectionRgba) {
        frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);
        return true;
    }

    @Override
    public void getViewMatrix(float[] viewmtx) {
        camera.getViewMatrix(viewmtx, 0);
    }

    @Override
    public void getProjectionMatrix(float[] projmtx, float nearClip, float farClip) {
        camera.getProjectionMatrix(projmtx, 0, nearClip, farClip);
    }

    @Override
    public void getCameraToWorldMatrix(float[] invViewMat) {
        camera.getPose().toMatrix(invViewMat, 0);
    }

    @Override
    public void getImuToWorldMatrix(float[] mat) {
        frame.getAndroidSensorPose().toMatrix(mat, 0);
    }

    @Override
    public List<HorizontalPlane> getHorizontalPlanes() {
        List<HorizontalPlane> planeList = new ArrayList<>();
        for (Plane plane : session.getAllTrackables(Plane.class)) {
            if (plane.getType() == Plane.Type.HORIZONTAL_UPWARD_FACING) {
                HorizontalPlane h = new HorizontalPlane();
                h.xyz = plane.getCenterPose().getTranslation();
                h.extentX = plane.getExtentX();
                h.extentZ = plane.getExtentZ();
                planeList.add(h);
            }
        }
        return planeList;
    }

    /**
     * Returns a transformation matrix that when applied to screen space uvs makes them match
     * correctly with the quad texture coords used to render the camera feed. It takes into account
     * device orientation.
     */
    private static float[] getTextureTransformMatrix(Frame frame) {
        float[] frameTransform = new float[6];
        float[] uvTransform = new float[9];
        // XY pairs of coordinates in NDC space that constitute the origin and points along the two
        // principal axes.
        float[] ndcBasis = {0, 0, 1, 0, 0, 1};

        // Temporarily store the transformed points into outputTransform.
        frame.transformCoordinates2d(
                Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
                ndcBasis,
                Coordinates2d.TEXTURE_NORMALIZED,
                frameTransform);

        // Convert the transformed points into an affine transform and transpose it.
        float ndcOriginX = frameTransform[0];
        float ndcOriginY = frameTransform[1];
        uvTransform[0] = frameTransform[2] - ndcOriginX;
        uvTransform[1] = frameTransform[3] - ndcOriginY;
        uvTransform[2] = 0;
        uvTransform[3] = frameTransform[4] - ndcOriginX;
        uvTransform[4] = frameTransform[5] - ndcOriginY;
        uvTransform[5] = 0;
        uvTransform[6] = ndcOriginX;
        uvTransform[7] = ndcOriginY;
        uvTransform[8] = 1;

        return uvTransform;
    }
}
