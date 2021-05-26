package com.indooratlas.android.sdk.examples.ar.rendering;

import android.content.Context;
import android.opengl.GLES20;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Some shader magic, see e.g.
 * https://en.wikibooks.org/wiki/OpenGL_Programming/Post-Processing
 */
public class BorderEffect {
    private static final String TAG = BorderEffect.class.getSimpleName();
    private int fboId = -1, colorTextureId, depthRboId, shaderProgram;
    private float pixelDeltaX, pixelDeltaY;

    private int posAttrib, texCoordAttrib;
    private int colorTextureUniform, pixelDeltaUniform;

    private static final String VERTEX_SHADER_NAME = "shaders/screenquad.vert";
    private static final String FRAGMENT_SHADER_NAME = "shaders/border_effect.frag";
    private FloatBuffer quadCoords, quadTexCoords;

    // See ARCore background renderer
    static final int COORDS_PER_VERTEX = 2;
    static final int TEXCOORDS_PER_VERTEX = 2;
    static final int FLOAT_SIZE = 4;
    static final float[] QUAD_COORDS =
            new float[] {
                    -1.0f, -1.0f, +1.0f, -1.0f, -1.0f, +1.0f, +1.0f, +1.0f,
            };

    static final float[] QUAD_TEX_COORDS =
            new float[] {
                    0f, 0f,
                    1f, 0f,
                    0f, 1f,
                    1f, 1f,
            };

    public void beginCapture() {
        if (fboId < 0) return; // not initialized yet, skip

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        ShaderUtil.checkGLError(TAG, "beginCapture");
    }

    public void endCapture() {
        if (fboId < 0) return; // not initialized yet, skip

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        ShaderUtil.checkGLError(TAG, "endCapture");
    }

    public void onSurfaceChanged(int w, int h) {
        pixelDeltaX = 1f / w;
        pixelDeltaY = 1f / h;

        boolean initialized = fboId >= 0;
        if (!initialized) {
            int[] buf = new int[1];
            GLES20.glGenTextures(1, buf, 0);
            colorTextureId = buf[0];

            GLES20.glGenRenderbuffers(1, buf, 0);
            depthRboId = buf[0];

            GLES20.glGenFramebuffers(1, buf, 0);
            fboId = buf[0];

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0); // should not be needed
            setDefaultTextureParams(colorTextureId);
            ShaderUtil.checkGLError(TAG, "onSurfaceChanged/create textures");
        }

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0); // should not be needed

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, colorTextureId);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, w, h, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        ShaderUtil.checkGLError(TAG, "onSurfaceChanged/color");

        // note: can't render to depth textures (only RBOs) in GLES 2
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthRboId);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, w, h);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
        ShaderUtil.checkGLError(TAG, "onSurfaceChanged/depth");

        if (!initialized) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, colorTextureId, 0);
            ShaderUtil.checkGLError(TAG, "createOnGlThread/fbo/GL_COLOR_ATTACHMENT0");
            GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, depthRboId);
            ShaderUtil.checkGLError(TAG, "createOnGlThread/fbo/GL_DEPTH_ATTACHMENT");
            if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                throw new RuntimeException("incomplete FrameBuffer");
            }
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            ShaderUtil.checkGLError(TAG, "createOnGlThread/fbo");
        }
    }

    public void createOnGlThread(Context context) throws IOException {
        ByteBuffer bbCoords = ByteBuffer.allocateDirect(QUAD_COORDS.length * FLOAT_SIZE);
        bbCoords.order(ByteOrder.nativeOrder());
        quadCoords = bbCoords.asFloatBuffer();
        quadCoords.put(QUAD_COORDS);
        quadCoords.position(0);

        final int numVertices = 4;
        ByteBuffer bbTexCoordsTransformed =
                ByteBuffer.allocateDirect(numVertices * TEXCOORDS_PER_VERTEX * FLOAT_SIZE);
        bbTexCoordsTransformed.order(ByteOrder.nativeOrder());
        quadTexCoords = bbTexCoordsTransformed.asFloatBuffer();
        quadTexCoords.put(QUAD_TEX_COORDS);
        quadTexCoords.position(0);

        int vertexShader =
                ShaderUtil.loadGLShader(TAG, context, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_NAME);
        int fragmentShader =
                ShaderUtil.loadGLShader(
                        TAG, context, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_NAME);

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShader);
        GLES20.glAttachShader(shaderProgram, fragmentShader);
        GLES20.glLinkProgram(shaderProgram);

        GLES20.glUseProgram(shaderProgram);
        posAttrib = GLES20.glGetAttribLocation(shaderProgram, "a_Position");
        texCoordAttrib = GLES20.glGetAttribLocation(shaderProgram, "a_TexCoord");
        ShaderUtil.checkGLError(TAG, "Program creation");

        colorTextureUniform = GLES20.glGetUniformLocation(shaderProgram, "u_colorTexture");
        pixelDeltaUniform = GLES20.glGetUniformLocation(shaderProgram, "u_pixelDelta");

        if (colorTextureUniform < 0 || pixelDeltaUniform < 0) {
            throw new RuntimeException("something wrong with the shader, be sure to use all the uniforms you define");
        }

        GLES20.glUseProgram(0);
        ShaderUtil.checkGLError(TAG, "Program parameters");
    }

    public void render() {
        if (fboId < 0) return; // not initialized yet, skip
        //GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(shaderProgram);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Disable depth test in post processing
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthMask(false);
        ShaderUtil.checkGLError(TAG, "render/0");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        ShaderUtil.checkGLError(TAG, "render/1a");
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, colorTextureId);
        ShaderUtil.checkGLError(TAG, "render/1b");
        GLES20.glUniform1i(colorTextureUniform, 0);
        ShaderUtil.checkGLError(TAG, "render/1u");

        GLES20.glUniform2f(pixelDeltaUniform, pixelDeltaX, pixelDeltaY);

        ShaderUtil.checkGLError(TAG, "render/set uniforms");

        // Set the vertex positions and texture coordinates.
        GLES20.glVertexAttribPointer(
                posAttrib, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, quadCoords);
        GLES20.glVertexAttribPointer(
                texCoordAttrib, TEXCOORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, quadTexCoords);

        GLES20.glEnableVertexAttribArray(posAttrib);
        GLES20.glEnableVertexAttribArray(texCoordAttrib);

        ShaderUtil.checkGLError(TAG, "render/set attributes");

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        ShaderUtil.checkGLError(TAG, "render/glDrawArrays");

        GLES20.glDisableVertexAttribArray(posAttrib);
        GLES20.glDisableVertexAttribArray(texCoordAttrib);

        // Restore the depth state for further drawing.
        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_BLEND);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glUseProgram(0);
        ShaderUtil.checkGLError(TAG, "render");
    }

    private static void setDefaultTextureParams(int textureId) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        ShaderUtil.checkGLError(TAG, "setDefaultTextureParams");
    }
}
