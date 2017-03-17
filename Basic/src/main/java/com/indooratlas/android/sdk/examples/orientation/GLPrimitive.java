package com.indooratlas.android.sdk.examples.orientation;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

/**
 * Contains a primitive shape that can be drawn using GLES20.glDrawArrays
 */
class GLPrimitive {

    private int mNumVertices;
    private FloatBuffer mPosition;
    private FloatBuffer mTexCoord;

    GLPrimitive(float [] coordinates, float [] texCoords) {
        if (coordinates == null) {
            throw new IllegalArgumentException("coordinates cannot be null");
        }
        if (texCoords == null) {
            throw new IllegalArgumentException("texCoords cannot be null");
        }
        if (coordinates.length % 3 != 0) {
            throw new IllegalArgumentException("coordinates.length not dividable by three (" +
                    coordinates.length + ")");
        }
        if (texCoords.length % 2 != 0) {
            throw new IllegalArgumentException("texCoords.length not dividable by two (" +
                    texCoords.length + ")");
        }

        mNumVertices = coordinates.length / 3;
        if (texCoords.length / 2 != mNumVertices) {
            throw new IllegalArgumentException("different number of points in text coords");
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(coordinates.length * 4);
        bb.order(ByteOrder.nativeOrder());
        mPosition = bb.asFloatBuffer();
        mPosition.put(coordinates);
        mPosition.position(0);

        ByteBuffer bb2 = ByteBuffer.allocateDirect(texCoords.length * 4);
        bb2.order(ByteOrder.nativeOrder());
        mTexCoord = bb2.asFloatBuffer();
        mTexCoord.put(texCoords);
        mTexCoord.position(0);
    }

    public void drawArrayWithPos(int handlePosition, int type) {
        GLES20.glVertexAttribPointer(handlePosition, 3, GLES20.GL_FLOAT, false, 0, mPosition);
        GLES20.glDrawArrays(type, 0, mNumVertices);
    }

    public void drawArrayWithPosTexCoord(int handlePosition, int handleTexCoords, int type) {
        GLES20.glVertexAttribPointer(handlePosition, 3, GLES20.GL_FLOAT, false, 0, mPosition);
        GLES20.glVertexAttribPointer(handleTexCoords, 2, GLES20.GL_FLOAT, false, 0, mTexCoord);
        GLES20.glDrawArrays(type, 0, mNumVertices);
    }

    public static class Builder {

        private ArrayList<Float> mGivenPositions = new ArrayList<>();
        private ArrayList<Float> mGivenTexCoords = new ArrayList<>();

        public Builder posAndTexCoord(float x, float y, float z, float tx, float ty) {
            mGivenPositions.add(x);
            mGivenPositions.add(y);
            mGivenPositions.add(z);
            mGivenTexCoords.add(tx);
            mGivenTexCoords.add(ty);
            return this;
        }

        public GLPrimitive build() {
            float [] coordinates = new float[mGivenPositions.size()];
            for (int i = 0; i < mGivenPositions.size(); i++) {
                coordinates[i] = mGivenPositions.get(i);
            }
            float tex [] = new float[mGivenTexCoords.size()];
            for (int i = 0; i < mGivenTexCoords.size(); i++) {
                tex[i] = mGivenTexCoords.get(i);
            }
            return new GLPrimitive(coordinates, tex);
        }
    }
}
