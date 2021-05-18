package com.indooratlas.android.sdk.examples.ar.wrapper;

import android.app.Activity;
import android.content.Context;

import com.indooratlas.android.sdk.examples.ar.rendering.ObjectRenderer;

import java.io.IOException;
import java.util.List;

public interface Api {
    public interface ARAvailabilityCallback {
        void onARAvailability(boolean available);
    }

    class ResumeResult {
        public enum Status {
            SUCCESS,
            PENDING,
            ERROR
        };
        public Status status;
        public String errorMessage;

        ResumeResult(Status status) {
            this.status = status;
        }

        ResumeResult(String errorMessage) {
            this.status = Status.ERROR;
            this.errorMessage = errorMessage;
        }
    }

    class HorizontalPlane {
        public float[] xyz;
        public float extentX, extentZ;
    }

    ResumeResult handleInstallFlowAndResume(Activity activity);
    boolean isRunning();

    void pause();

    void onFrame();
    void renderPointCloud(float[] viewmtx, float[] projmtx);
    void renderPlanes(float[] projmtx);
    float[] getUpdatedUvTransformMatrix();
    boolean getColorCorrection(float[] rgba);

    void getViewMatrix(float[] mat); // world-to-camea matrix
    void getProjectionMatrix(float[] mat, float nearClip, float farClip);

    // these are given to the IA AR SDK
    void getCameraToWorldMatrix(float[] mat);
    void getImuToWorldMatrix(float[] mat);
    List<HorizontalPlane> getHorizontalPlanes();

    void onSurfaceCreated(Context context) throws IOException;
    void setupObject(Context context, ObjectRenderer obj) throws IOException;
    void setDisplayGeometry(int rot, int w, int h);

    // returns null if tracking
    String getTrackingFailureReasonString();
    boolean shouldKeepScreenOn();
}