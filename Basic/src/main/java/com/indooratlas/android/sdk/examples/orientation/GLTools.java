package com.indooratlas.android.sdk.examples.orientation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.opengl.GLES20;
import android.opengl.GLUtils;

/**
 * Static class containing OpenGL resources
 */
public class GLTools {

    // Shader program for uniform color.
    // vs: mat4 uMatrix     matrix
    //     vec4 vPosition   vertex position
    // fs: vec4 uColor      uniform color
    public static int sProgSimple;
    private static final String sVertexSimple =
            "" +
                    "uniform    mat4        uMatrix;" +
                    "attribute  vec4        vPosition;" +
                    "void main() {" +
                    "  gl_Position = uMatrix * vPosition;" +
                    "}";
    private static final String sFragmentSimple =
            "" +
                    "precision mediump float;" +
                    "uniform vec4 uColor;" +
                    "void main() {" +
                    "  gl_FragColor = uColor;" +
                    "}";


    // Shader program for textured surfaces
    public static int sProgTexture;
    private static final String sVertexTexture =
            "" +
                    "uniform    mat4        uMatrix;" +
                    "attribute  vec4        vPosition;" +
                    "attribute  vec2        aTexCoordinate;" +
                    "varying    vec2        vTexCoordinate;" +
                    "void main() {" +
                    "  gl_Position = uMatrix * vPosition;" +
                    "  vTexCoordinate = aTexCoordinate;" +
                    "}";
    private static final String sFragmentTexture =
            "" +
                    "precision mediump float;" +
                    "uniform    sampler2D   uTexture;" +
                    "varying    vec2        vTexCoordinate;" +
                    "void main() {" +
                    "  gl_FragColor = texture2D(uTexture, vTexCoordinate);" +
                    "}";

    /**
     * Create shapes and shader programs
     */
    public static void setup() {
        // Create programs
        sProgSimple = createProgram(GLTools.sVertexSimple, GLTools.sFragmentSimple);
        GLES20.glLinkProgram(GLTools.sProgSimple);
        sProgTexture = createProgram(GLTools.sVertexTexture, GLTools.sFragmentTexture);
        GLES20.glLinkProgram(GLTools.sProgTexture);
    }

    /**
     * Create a panoramic sphere containing vertex and texture coordinates. Should be drawn using
     * GL_TRIANGLES. Texture coordinate [0.5, 0.5] corresponds to the negative z-axis, i.e. forward
     * points towards the center of the texture. The first texture coordinate increases when
     * "turning right" meaning the positive x-axis corresponds to [0.75, 0.5]. The positive y-axis
     * points to the point where the second texture coordinate is one.
     */
    public static GLPrimitive createPanoramaSphere(int latCount, int lonCount, float radius) {
        GLPrimitive.Builder builder = new GLPrimitive.Builder();

        for (int iLon = 0; iLon < lonCount; iLon++) {

            float lon0 = 2.0f * (float) Math.PI * (float) iLon / (float) lonCount;
            float lon1 = 2.0f * (float) Math.PI * (float) (iLon + 1) / (float) lonCount;
            float u0 = 0.5f + (float) iLon / (float) lonCount;
            float u1 = 0.5f + (float) (iLon +1) / (float) lonCount;

            float coslon0 = (float) Math.cos(lon0);
            float sinlon0 = (float) Math.sin(lon0);
            float coslon1 = (float) Math.cos(lon1);
            float sinlon1 = (float) Math.sin(lon1);
            for (int iLat = 0; iLat < latCount; iLat++) {

                float lat0 = (float) Math.PI * (float) iLat / (float) latCount;
                float lat1 = (float) Math.PI * (float) (iLat + 1) / (float) latCount;
                float v0 = (float) iLat / (float) latCount;
                float v1 = (float) (iLat + 1) / (float) latCount;

                float coslat0 = (float) Math.cos(lat0);
                float sinlat0 = (float) Math.sin(lat0);
                float coslat1 = (float) Math.cos(lat1);
                float sinlat1 = (float) Math.sin(lat1);
                float x0 = radius * sinlon0 * sinlat0;
                float z0 = -radius * coslon0 * sinlat0;
                float y0 = -radius * coslat0;
                float x1 = radius * sinlon1 * sinlat0;
                float z1 = -radius * coslon1 * sinlat0;
                float y1 = -radius * coslat0;
                float x2 = radius * sinlon0 * sinlat1;
                float z2 = -radius * coslon0 * sinlat1;
                float y2 = -radius * coslat1;
                float x3 = radius * sinlon1 * sinlat1;
                float z3 = -radius * coslon1 * sinlat1;
                float y3 = -radius * coslat1;

                builder.posAndTexCoord(x0, y0, z0, u0, v0);
                builder.posAndTexCoord(x1, y1, z1, u1, v0);
                builder.posAndTexCoord(x2, y2, z2, u0, v1);

                builder.posAndTexCoord(x2, y2, z2, u0, v1);
                builder.posAndTexCoord(x1, y1, z1, u1, v0);
                builder.posAndTexCoord(x3, y3, z3, u1, v1);

            }
        }
        return builder.build();
    }


    /**
     * Create an OpenGL program from a given vertex and fragment shader.
     */
    public static int createProgram(String vertexShader, String fragmentShader) {
        int vs = GLTools.loadShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        int fs = GLTools.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vs);
        GLES20.glAttachShader(program, fs);
        return program;
    }


    private static int loadShader(int type, String shaderCode){
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    // *** Texture related tools *** //

    /**
     * Load a texture
     */
    public static int loadTexture(Context context, int resourceId) {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] == 0) {
            throw new RuntimeException("failed to generate texture");
        }


        // Load the bitmap and flip it to be consistent with OpenGL texture coordinates
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;   // No pre-scaling
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
        Matrix flip = new Matrix();
        flip.postScale(1f, -1f);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), flip,
                false);

        // Bind to the texture in OpenGL
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        // Recycle the bitmap, since its data has been loaded into OpenGL.
        bitmap.recycle();

        return textureHandle[0];
    }


    // *** Math related tools *** //


    /**
     * Creates a quaternion with the given vector part and zero real part. Needed in quaternion
     * rotations.
     *
     * @param vec A vector with 3 values.
     * @return A quaternion with 4 values.
     */
    public static float [] vecToQuat(float ... vec) {
        ensureValidArray(3, vec);
        float [] output = new float[4];
        for (int i = 0; i < 3; i++) {
            output[i + 1] = vec[i];
        }
        return output;
    }


    /**
     * Takes the vector part of a quaternion. Needed in quaternion rotations.
     *
     * @param quat A quaternion with 4 values.
     * @return A vector with 3 values.
     */
    public static float [] quatToVec(float ... quat) {
        ensureValidArray(4, quat);
        float [] output = new float[3];
        for (int i = 0; i < 3; i++) {
            output[i] = quat[i + 1];
        }
        return output;
    }

    /**
     * Performs quaternion multiplication.
     *
     * @param q Left hand side quaternion.
     * @param r Right hand side quaternion.
     * @return Result quaternion.
     */
    public static float [] quatMult(float [] q, float [] r) {
        ensureValidArray(4, q);
        ensureValidArray(4, q);
        float [] output = new float[4];
        output[0] = r[0] * q[0] - r[1] * q[1] - r[2] * q[2] - r[3] * q[3];
        output[1] = r[0] * q[1] + r[1] * q[0] - r[2] * q[3] + r[3] * q[2];
        output[2] = r[0] * q[2] + r[1] * q[3] + r[2] * q[0] - r[3] * q[1];
        output[3] = r[0] * q[3] - r[1] * q[2] + r[2] * q[1] + r[3] * q[0];
        return output;
    }

    /**
     * Transposes a quaternion.
     *
     * @param q Initial quaternion.
     * @return Transposed quaternion.
     */
    public static float [] quatTranspose(float [] q) {
        ensureValidArray(4, q);
        float [] output = new float[4];
        output[0] = q[0];
        output[1] = -q[1];
        output[2] = -q[2];
        output[3] = -q[3];
        return output;
    }

    /**
     * Takes a vector and rotates it by a given unit quaternion.
     *
     * @param quat Quaternion determining rotation. Needs to be an unit quaternion.
     * @param direction The vector to be rotated.
     * @return The rotated vector.
     */
    public static float [] rotate(float [] quat, float ... direction) {
        ensureValidArray(4, quat);
        ensureValidArray(3, direction);
        float [] dirQuat = vecToQuat(direction);
        return quatToVec(quatMult(quatMult(quat, dirQuat), quatTranspose(quat)));
    }

    // *** other utilities ** //

    /**
     * Check that array input is not null and has the correct length.
     * @param len expected length
     * @param values float array
     */
    public static void ensureValidArray(int len, float [] values) {
        if (values == null) {
            throw new IllegalArgumentException("values cannot be null");
        }
        if (values.length != len) {
            throw new IllegalArgumentException("length not " + len + "(" + values.length + ")");
        }
    }

}
