package com.indooratlas.android.sdk.examples.orientation;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.Surface;
import android.view.WindowManager;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Renders a panorama view.
 */
public class OrientationRenderer implements GLSurfaceView.Renderer {

    private final Context mContext;
    private final int mResourceId;

    private float mUpX, mUpY;
    private float [] mOrientation = new float[] {1.0f, 0.0f, 0.0f, 0.0f};
    private final float[] mMatrixProjection = new float[16];
    private final float[] mMatrixView = new float[16];
    private final float[] mMatrixCombined = new float[16];

    private GLPrimitive mPanoramaShape;
    private int mTexturePanorama = 0;

    /**
     * Constructor
     *
     * @param context Context
     * @param panoramaResource Resource to use as panorama image
     */
    public OrientationRenderer(Context context, int panoramaResource) {
        mContext = context;
        mResourceId = panoramaResource;
    }

    /**
     * Set orientation. The quaternion received from IndoorAtlas can be given to the method.
     *
     * @param quat Unit quaternion.
     */
    public void setOrientation(double [] quat) {
        if (quat == null) {
            throw new IllegalArgumentException("Orientation quaternion cannot be null");
        }
        if (quat.length != 4) {
            throw new IllegalArgumentException("Orientation quaternion needs to have 4 elements");
        }
        mOrientation = new float[4];
        for (int i = 0; i < 4; i++) {
            mOrientation[i] = (float) quat[i];
        }
    }

    @Override
    public void onDrawFrame(GL10 unused) {

        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        float [] quat = mOrientation;
        // forward is always where phone negative z axis points
        float [] fwd = GLTools.rotate(quat, 0.0f, 0.0f, -1.0f);
        // up depends on screen orientation
        float [] up = GLTools.rotate(quat, mUpX, mUpY, 0.0f); // up is where phone y points

        // Note difference between ENU coordinates and OpenGL coordinates
        Matrix.setLookAtM(mMatrixView, 0, 0.0f, 0.0f, 0.0f,
                fwd[0], fwd[2], -fwd[1],
                up[0], up[2], -up[1]);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMatrixCombined, 0, mMatrixProjection, 0, mMatrixView, 0);

        // ** panorama ** //

        // Draw using the texture program
        GLES20.glUseProgram(GLTools.sProgTexture);
        int handlePosition = GLES20.glGetAttribLocation(GLTools.sProgTexture, "vPosition");
        int handleTexCoord = GLES20.glGetAttribLocation(GLTools.sProgTexture, "aTexCoordinate");
        int handleMatrix = GLES20.glGetUniformLocation(GLTools.sProgTexture, "uMatrix");
        int handleTexture = GLES20.glGetUniformLocation(GLTools.sProgTexture, "uTexture");

        GLES20.glEnableVertexAttribArray(handlePosition);
        GLES20.glEnableVertexAttribArray(handleTexCoord);
        GLES20.glUniformMatrix4fv(handleMatrix, 1, false, mMatrixCombined, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexturePanorama);
        GLES20.glUniform1i(handleTexture, 0);

        mPanoramaShape.drawArrayWithPosTexCoord(handlePosition, handleTexCoord, GLES20.GL_TRIANGLES);

        GLES20.glDisableVertexAttribArray(handleTexCoord);
        GLES20.glDisableVertexAttribArray(handlePosition);

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

        // The orientation of the phone is given in Android sensor coordinates but the screen
        // coordinates depends on the orientation of the phone.
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        switch (wm.getDefaultDisplay().getRotation()) {
            default:
            case Surface.ROTATION_0:
                mUpX = 0.0f;
                mUpY = 1.0f;
                break;
            case Surface.ROTATION_90:
                mUpX = 1.0f;
                mUpY = 0.0f;
                break;
            case Surface.ROTATION_180:
                mUpX = 0.0f;
                mUpY = -1.0f;
                break;
            case Surface.ROTATION_270:
                mUpX = -1.0f;
                mUpY = 0.0f;
                break;
        }

        // Set viewport
        GLES20.glViewport(0, 0, width, height);
        float near = 0.1f;
        float fow = 1.0f;
        float far = 200.0f;
        float dx = near * fow;
        float dy = near * fow * (float) height / (float) width;
        Matrix.frustumM(mMatrixProjection, 0, -dx, dx, -dy, dy, near, far);

    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLTools.setup();
        mPanoramaShape = GLTools.createPanoramaSphere(50, 50, 5.0f);
        mTexturePanorama = GLTools.loadTexture(mContext, mResourceId);
    }

}
