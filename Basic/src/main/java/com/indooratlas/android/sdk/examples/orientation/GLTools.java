package com.indooratlas.android.sdk.examples.orientation;

import android.opengl.GLES20;

/**
 * Static class containing OpenGL resources
 */
public class GLTools {

    // Shader program for uniform color.
    // vs: mat4 uMatrix     matrix
    //     vec4 vPosition   vertex position
    // fs: vec4 uColor      uniform color
    public static int sProgSimple;
    public static final String sVertexSimple =
            "" +
                    "uniform    mat4        uMatrix;" +
                    "attribute  vec4        vPosition;" +
                    "void main() {" +
                    "  gl_Position = uMatrix * vPosition;" +
                    "}";
    public static final String sFragmentSimple =
            "" +
                    "precision mediump float;" +
                    "uniform vec4 uColor;" +
                    "void main() {" +
                    "  gl_FragColor = uColor;" +
                    "}";

    // Various shapes
    public static OpenGLShape sShapeSky;
    public static OpenGLShape sShapeGround;
    public static OpenGLShape sShapeGrid;
    public static OpenGLShape sShapeNorthNeedle;
    public static OpenGLShape sShapeSouthNeedle;

    /**
     * Create shapes and shader programs
     */
    public static void setup() {

        // Define various simple shapes
        sShapeSky = new OpenGLShape(
                0.0f, 20.0f, 0.0f,
                -20.0f, 0.0f, -20.0f,
                20.0f, 0.0f, -20.0f,
                20.0f, 0.0f, 20.0f,
                -20.0f, 0.0f, 20.0f,
                -20.0f, 0.0f, -20.0f);
        sShapeGround = new OpenGLShape(
                0.0f, -20.0f, 0.0f,
                -20.0f, 0.0f, -20.0f,
                20.0f, 0.0f, -20.0f,
                20.0f, 0.0f, 20.0f,
                -20.0f, 0.0f, 20.0f,
                -20.0f, 0.0f, -20.0f);
        sShapeNorthNeedle = new OpenGLShape(
                -1.0f, 0.0f, -15.0f,
                1.0f, 0.0f, -15.0f,
                0.0f, 3.0f, -15.0f);
        sShapeSouthNeedle = new OpenGLShape(
                -1.0f, 0.0f, 15.0f,
                1.0f, 0.0f, 15.0f,
                0.0f, 3.0f, 15.0f);

        // Define the spherical grid
        OpenGLShape.Builder grid = new OpenGLShape.Builder();
        int numLon = 8;
        int numLat = 8;
        int numLonFine = 36;
        int numLatFine = 18;
        // vertical lines
        for (int lon = 0; lon < numLon; lon++) {
            float alon = 2.0f * (float) Math.PI * lon / ((float) numLon);
            float coslon = (float) Math.cos(alon);
            float sinlon = (float) Math.sin(alon);
            for (int lat = 0; lat < numLatFine; lat++) {
                float alat0 = (float) Math.PI * (float) lat / ((float) numLatFine);
                float alat1 = (float) Math.PI * (float) (lat + 1) / ((float) numLatFine);
                float coslat0 = (float) Math.cos(alat0);
                float sinlat0 = (float) Math.sin(alat0);
                float coslat1 = (float) Math.cos(alat1);
                float sinlat1 = (float) Math.sin(alat1);
                grid.addPoint(10.0f * coslon * sinlat0 , 10.0f * coslat0, 10.0f * sinlon * sinlat0);
                grid.addPoint(10.0f * coslon * sinlat1, 10.0f * coslat1, 10.0f * sinlon * sinlat1);
            }
        }
        // horizontal lines
        for (int lat = 1; lat < numLat - 1; lat++) {
            float alat = (float) Math.PI * lat / ((float) numLat);
            float coslat = (float) Math.cos(alat);
            float sinlat = (float) Math.sin(alat);
            for (int lon = 0; lon < numLonFine; lon++) {
                float alon0 = 2.0f * (float) Math.PI * (float) lon / ((float) numLonFine);
                float alon1 = 2.0f * (float) Math.PI * (float) (lon + 1) / ((float) numLonFine);
                float coslon0 = (float) Math.cos(alon0);
                float sinlon0 = (float) Math.sin(alon0);
                float coslon1 = (float) Math.cos(alon1);
                float sinlon1 = (float) Math.sin(alon1);
                grid.addPoint(10.0f * coslon0 * sinlat , 10.0f * coslat, 10.0f * sinlon0 * sinlat);
                grid.addPoint(10.0f * coslon1 * sinlat, 10.0f * coslat, 10.0f * sinlon1 * sinlat);

            }

        }
        sShapeGrid = grid.build();

        // Create programs
        sProgSimple = createProgram(GLTools.sVertexSimple, GLTools.sFragmentSimple);
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


    public static int loadShader(int type, String shaderCode){
        int shader = GLES20.glCreateShader(type);
        if (shader == 0) {
            throw new IllegalArgumentException("Failed to create shader");
        }
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
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

    private static void ensureValidArray(int len, float ... values) {
        if (values == null) {
            throw new IllegalArgumentException("values cannot be null");
        }
        if (values.length != len) {
            throw new IllegalArgumentException("length not " + len + "(" + values.length + ")");
        }
    }

}
