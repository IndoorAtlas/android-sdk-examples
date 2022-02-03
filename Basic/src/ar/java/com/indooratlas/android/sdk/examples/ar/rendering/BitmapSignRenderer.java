package com.indooratlas.android.sdk.examples.ar.rendering;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.Matrix;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BitmapSignRenderer extends BitmapRenderer {
    public static class Cache {
        private final Map<String, BitmapSignRenderer> store = new HashMap<>();

        private static BitmapSignRenderer load(String assetName, Activity activity) {
            BitmapSignRenderer r = new BitmapSignRenderer();
            try {
                // note load bitmaps synchronously, not optimal
                r.onBitmapReady(BitmapFactory.decodeStream(
                        activity.getAssets().open("models/" + assetName)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return r;
        }

        public BitmapSignRenderer get(String assetName, Activity activity) {
            if (store.containsKey(assetName)) {
            } else {
                store.put(assetName, load(assetName, activity));
            }
            return store.get(assetName);
        }
    }

    private final float[][] cornerData = new float[4][3];
    private final float[] tmpMatrix2 = new float[16];
    private final float[] cornerMatrix = new float[] {
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1 };

    private float aspectRatio;
    private boolean glInitialized = false;

    @Override
    public void createOnGlThread(Context context) {
        if (glInitialized) return;
        glInitialized = true;
        super.createOnGlThread(context);
    }

    public void draw(float[] cameraView, float[] cameraPerspective, float [] modelMatrix, float scale) {
        Matrix.multiplyMM(tmpMatrix2, 0, cameraView, 0, modelMatrix, 0);
        boolean flip = !isObjectZAxisFacingTheViewer(tmpMatrix2);

        for (int i = 0; i < 4; ++i) {
            float texX = QUAD_TEXCOORDS[2 * i];
            float texY = QUAD_TEXCOORDS[2 * i + 1];

            float relX = aspectRatio * (0.5f - texX);
            float relY = 1f - texY;

            // mirror along X if not facing the viewer
            if (flip) relX = -relX;

            cornerMatrix[4 * 3 + 0] = relX * scale;
            cornerMatrix[4 * 3 + 1] = relY * scale;
            Matrix.multiplyMM(tmpMatrix2, 0, modelMatrix, 0, cornerMatrix, 0);

            for (int j=0; j<3; ++j) {
                cornerData[i][j] = tmpMatrix2[4 * 3 + j];
            }
        }

        draw(cameraView, cameraPerspective, cornerData);
    }

    void onBitmapReady(Bitmap bitmap) {
        setBitmap(bitmap);
        aspectRatio = ((float) bitmap.getWidth()) / bitmap.getHeight();
    }

    private static boolean isObjectZAxisFacingTheViewer(float [] modelViewMatrix) {
        float dot = 0;
        // computes the dot product of:
        //  a: the vector from the camera to the bitmap plane origin in camera coordinates,
        //     which is stored in the first 3 elements of the 4th column of the model-view-matrix
        //  b: the bitmap plane normal (local z-axis) in camera coordinates
        //     which is the first 3 elements of the 3rd column of the model-view-matrix
        // The sign of this vector determines if the normal is facing the camera or not.
        for (int i = 0; i < 3; ++i)
            dot += modelViewMatrix[4 * 3 + i] * modelViewMatrix[4 * 2 + i];
        // There are several sign flips involved here, e.g., projection matrix z-axis dir and
        // QUAD_TEXCOORDS configuration, and it's safe to pick the correct sign by trial and error
        return dot > 0;
    }
}