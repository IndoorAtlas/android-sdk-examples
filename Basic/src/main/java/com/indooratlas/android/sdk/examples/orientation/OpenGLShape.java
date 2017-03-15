package com.indooratlas.android.sdk.examples.orientation;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

/**
 * Utility class for storing a FloatBuffer containing vertices.
 */
class OpenGLShape {

    int numVertices;
    FloatBuffer vertices;

    OpenGLShape(float ... coordinates) {

        if (coordinates == null) {
            throw new IllegalArgumentException("coordinates cannot be null");
        }
        if (coordinates.length % 3 != 0) {
            throw new IllegalArgumentException("coordinates.length not dividable by three (" +
                    coordinates.length + ")");
        }
        numVertices = coordinates.length / 3;

        ByteBuffer bb = ByteBuffer.allocateDirect(coordinates.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertices = bb.asFloatBuffer();
        vertices.put(coordinates);
        vertices.position(0);
    }

    public void drawAs(int handlePosition, int type) {
        GLES20.glVertexAttribPointer(handlePosition, 3, GLES20.GL_FLOAT, false, 0, vertices);
        GLES20.glDrawArrays(type, 0, numVertices);
    }

    public static class Builder {

        private ArrayList<Float> mGiven = new ArrayList<>();

        public Builder addPoint(float ... coordinates) {
            if (coordinates == null) {
                throw new IllegalArgumentException("coordinates cannot be null");
            }
            if (coordinates.length != 3) {
                throw new IllegalArgumentException("coordinates.length != 3 (" +
                        coordinates.length + ")");
            }
            for (int i = 0; i< coordinates.length; i++) {
                mGiven.add(coordinates[i]);
            }
            return this;
        }

        public OpenGLShape build() {
            float [] coordinates = new float[mGiven.size()];
            for (int i = 0; i < mGiven.size(); i++) {
                coordinates[i] = mGiven.get(i);
            }
            return new OpenGLShape(coordinates);
        }
    }
}
