package com.indooratlas.android.sdk.examples.ar.rendering;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class BitmapRenderer {
    private static final String TAG = BitmapRenderer.class.getSimpleName();

    private static final int TEXCOORDS_PER_VERTEX = 2;
    private static final int COORDS_PER_VERTEX = 3;
    private static final int BYTES_PER_FLOAT = Float.SIZE / 8;
    private static final int SHORT_SIZE = Short.SIZE / 8;

    private FloatBuffer quadTexCoords;
    private ShortBuffer quadIndices;

    // Shader names.
    private static final String VERTEX_SHADER_NAME = "shaders/floor_plan.vert";
    private static final String FRAGMENT_SHADER_NAME = "shaders/floor_plan.frag";

    private int glslProgram;
    private boolean bitmapChanged = false;
    private Bitmap bitmap = null;

    private int vertexCoordAttribute;
    private int texCoordAttribute;

    private int modelViewProjectionUniform;
    private int texCoordUniform;
    private int textureId = -1;
    private int texCoordBufferId;
    private int indexBufferId;
    protected float[] tmpMatrix = new float[16];

    private FloatBuffer vertexBuffer =
            ByteBuffer.allocateDirect(4 * COORDS_PER_VERTEX * BYTES_PER_FLOAT)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();

    protected static final float[] QUAD_TEXCOORDS = new float[]{
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
    };

    private static final short[] QUAD_INDICES = new short[]{
            0, 1, 3,
            1, 2, 3,
    };

    private boolean updateBitmap() {
        final Bitmap bitmap;
        final boolean changed;

        synchronized (this) {
            changed = bitmapChanged;
            bitmap = this.bitmap;
            bitmapChanged = false;
        }

        if (bitmap == null) return false;
        if (!changed) return true;

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        ShaderUtil.checkGLError(TAG, "Floor plan update");

        return true;
    }

    public void createOnGlThread(Context context) {
        // Set up texture
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        textureId = textures[0];

        ShaderUtil.checkGLError(TAG, "Texture loading");

        int numVertices = 4;
        ByteBuffer bbTexCoords = ByteBuffer.allocateDirect(
                numVertices * TEXCOORDS_PER_VERTEX * BYTES_PER_FLOAT);
        bbTexCoords.order(ByteOrder.nativeOrder());
        quadTexCoords = bbTexCoords.asFloatBuffer();
        quadTexCoords.put(QUAD_TEXCOORDS);
        quadTexCoords.position(0);

        ByteBuffer bbIndices = ByteBuffer.allocateDirect(
                QUAD_INDICES.length * SHORT_SIZE);
        bbIndices.order(ByteOrder.nativeOrder());
        quadIndices = bbIndices.asShortBuffer();
        quadIndices.put(QUAD_INDICES);
        quadIndices.position(0);

        int[] buffers = new int[2];
        GLES20.glGenBuffers(2, buffers, 0);
        texCoordBufferId = buffers[0];
        indexBufferId = buffers[1];

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, SHORT_SIZE * quadIndices.limit(),
                quadIndices, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

        // Set the texture coordinates.
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, texCoordBufferId);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, BYTES_PER_FLOAT * quadTexCoords.limit(),
                quadTexCoords, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        final int vertexShader, fragmentShader;
        try {
            vertexShader = ShaderUtil.loadGLShader(TAG, context,
                    GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_NAME);
            fragmentShader = ShaderUtil.loadGLShader(TAG, context,
                    GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_NAME);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        glslProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(glslProgram, vertexShader);
        GLES20.glAttachShader(glslProgram, fragmentShader);
        GLES20.glLinkProgram(glslProgram);
        GLES20.glUseProgram(glslProgram);

        ShaderUtil.checkGLError(TAG, "Program creation");

        vertexCoordAttribute = GLES20.glGetAttribLocation(glslProgram, "a_Position");

        texCoordAttribute = GLES20.glGetAttribLocation(glslProgram, "a_TexCoord");
        if (texCoordAttribute < 0) {
            throw new RuntimeException("Invalid GL attribute");
        }
        modelViewProjectionUniform = GLES20.glGetUniformLocation(glslProgram, "u_ModelViewProjection");
        if (modelViewProjectionUniform < 0) {
            throw new RuntimeException("Invalid GL uniform");
        }
        texCoordUniform = GLES20.glGetUniformLocation(glslProgram, "u_Texture");
        if (texCoordUniform < 0) {
            throw new RuntimeException("Invalid GL uniform");
        }

        ShaderUtil.checkGLError(TAG, "Program parameters");
    }

    public void draw(float[] cameraView, float[] cameraPerspective, float[][] cornerData) {
        if (!updateBitmap()) return;

        vertexBuffer.rewind();
        for (float[] corner : cornerData) {
            for (float c : corner) {
                vertexBuffer.put(c);
            }
        }

        Matrix.multiplyMM(tmpMatrix, 0, cameraPerspective, 0, cameraView, 0);

        GLES20.glUseProgram(glslProgram);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Attach the object texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(texCoordUniform, 0);

        // Set the position of the plane
        vertexBuffer.rewind();
        GLES20.glEnableVertexAttribArray(vertexCoordAttribute);
        GLES20.glVertexAttribPointer(
                vertexCoordAttribute,
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                BYTES_PER_FLOAT * COORDS_PER_VERTEX,
                vertexBuffer);

        GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, tmpMatrix, 0);

        // Set the texture coordinates.
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, texCoordBufferId);
        GLES20.glVertexAttribPointer(texCoordAttribute, TEXCOORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(texCoordAttribute);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, quadIndices.limit(), GLES20.GL_UNSIGNED_SHORT, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(texCoordAttribute);
        GLES20.glDisableVertexAttribArray(vertexCoordAttribute);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glDisable(GLES20.GL_BLEND);
        ShaderUtil.checkGLError(TAG, "Draw");
    }

    protected synchronized void setBitmap(Bitmap bitmap) {
        bitmapChanged = true;
        this.bitmap = bitmap;
    }
}