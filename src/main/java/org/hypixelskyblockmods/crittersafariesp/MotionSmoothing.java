package org.hypixelskyblockmods.crittersafariesp;

public final class MotionSmoothing {
    private MotionSmoothing() {}
    /** Exponential smoothing gives the same response time at different frame rates. */
    public static double factor(double elapsedMillis, double responseMillis) {
        if (responseMillis <= 0) return 1;
        return 1 - Math.exp(-Math.max(0, elapsedMillis) / responseMillis);
    }
}
