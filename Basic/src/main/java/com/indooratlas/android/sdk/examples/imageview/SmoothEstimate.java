package com.indooratlas.android.sdk.examples.imageview;

/**
 * Simple smooth estimate for creating pleasant animation for estimates. The smoothness of the
 * animation is controlled by setting the speedRatio parameter. This controls how "close" to the
 * new estimate the smooth estimate will travel in one second.
 *
 * The x and y coordinates are unit independent, so you can use them with pixel, metric, or even
 * latitude-longitude coordinates.
 */
public class SmoothEstimate {
    private float x = 0f;
    private float y = 0f;
    private float a = 0f; // Heading in radians
    private float r = 0f; // Uncertainty radius
    private long t = -1; // -1 stands for uninitialized
    private double smoothnessInMs;

    /**
     * Default constructor setting the speed ratio towards the new estimate to 0.9 per second
     */
    public SmoothEstimate() {
        this(0.9);
    }

    /**
     * Constructor with speedRatio parameter
     * @param speedRatio Should be on interval [0,1]. When updating, controls the ratio of movement
     *                  towards the new estimate in 1 second time
     */
    public SmoothEstimate(double speedRatio) {
        this.smoothnessInMs = Math.pow(1.0 - speedRatio, 0.001);
    }

    /**
     * Smoothly updates the estimate coordinates according to the speed ratio and time elapsed
     * from last update
     * @param x x coordinate
     * @param y y coordinate
     * @param a Heading in radians
     * @param r Estimate uncertainty radius
     * @param t Timestamp in milliseconds
     */
    public void update(float x, float y, float a, float r, long t) {
        if (this.t != -1) {
            long dt = t - this.t;
            this.t = t;
            float ratio = (float) Math.pow(smoothnessInMs, dt);
            this.x = ratio * this.x + (1f - ratio) * x;
            this.y = ratio * this.y + (1f - ratio) * y;
            this.r = ratio * this.r + (1f - ratio) * r;
            this.a = weightedAngle(this.a, a, ratio, 1f - ratio);
        } else {
            this.x = x;
            this.y = y;
            this.a = a;
            this.r = r;
            this.t = t;
        }
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getHeading() {
        return a;
    }

    public float getRadius() {
        return r;
    }

    private static float weightedAngle(float a1, float a2, float w1, float w2) {
        double angleDiff = normalizeAngle(a2 - a1);
        double normalizedW2 = w2 / (w1 + w2);
        return (float) normalizeAngle(a1 + normalizedW2 * angleDiff);
    }

    private static double normalizeAngle(double a) {
        while (a > Math.PI) a -= 2 * Math.PI;
        while (a < -Math.PI) a += 2 * Math.PI;
        return a;
    }
}
