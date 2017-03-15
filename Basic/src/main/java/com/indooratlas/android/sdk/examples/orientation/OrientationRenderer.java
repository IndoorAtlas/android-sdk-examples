package com.indooratlas.android.sdk.examples.orientation;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Renders a panorama view
 */
public class OrientationRenderer implements GLSurfaceView.Renderer {

    private final Context mContext;
    private float mUpX, mUpY;
    private float [] mOrientation = new float[] {1.0f, 0.0f, 0.0f, 0.0f};
    private final float[] mMatrixProjection = new float[16];
    private final float[] mMatrixView = new float[16];
    private final float[] mMatrixCombined = new float[16];

    public void setOrientation(double [] quat) {
        mOrientation = new float[4];
        for (int i = 0; i < 4; i++) {
            mOrientation[i] = (float) quat[i];
        }
    }

    public OrientationRenderer(Context context) {
        mContext = context;
    }

    @Override
    public void onDrawFrame(GL10 unused) {

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

        GLES20.glUseProgram(GLTools.sProgSimple);
        int handlePosition = GLES20.glGetAttribLocation(GLTools.sProgSimple, "vPosition");
        int handleMatrix = GLES20.glGetUniformLocation(GLTools.sProgSimple, "uMatrix");
        int handleColor = GLES20.glGetUniformLocation(GLTools.sProgSimple, "uColor");
        GLES20.glEnableVertexAttribArray(handlePosition);

        GLES20.glUniformMatrix4fv(handleMatrix, 1, false, mMatrixCombined, 0);

        GLES20.glUniform4f(handleColor, 0.0f, 165.0f / 255.0f, 245.0f / 255.0f, 1.0f);
        GLTools.sShapeSky.drawAs(handlePosition, GLES20.GL_TRIANGLE_FAN);

        GLES20.glUniform4f(handleColor, 0.1f, 0.5f, 0.0f, 1.0f);
        GLTools.sShapeGround.drawAs(handlePosition, GLES20.GL_TRIANGLE_FAN);

        GLES20.glUniform4f(handleColor, 1.0f, 0.0f, 0.0f, 1.0f);
        GLTools.sShapeNorthNeedle.drawAs(handlePosition, GLES20.GL_TRIANGLES);
        GLES20.glUniform4f(handleColor, 0.0f, 0.0f, 0.0f, 1.0f);
        GLTools.sShapeSouthNeedle.drawAs(handlePosition, GLES20.GL_TRIANGLES);

        GLES20.glUniform4f(handleColor, 1.0f, 1.0f, 1.0f, 1.0f);
        GLTools.sShapeGrid.drawAs(handlePosition, GLES20.GL_LINES);

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

        // Set the drawing program
        GLES20.glLinkProgram(GLTools.sProgSimple);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLTools.setup();
    }

}
