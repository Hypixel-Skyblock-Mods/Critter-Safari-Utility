package org.hypixelskyblockmods.crittersafariesp;

public final class Projection {
    public record Point(float x, float y, boolean onScreen) {}
    private Projection() {}
    /** Project homogeneous clip coordinates; behind-camera points become directional edge markers. */
    public static Point toScreen(float x, float y, float w, int width, int height, boolean edgeIndicators) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(w) || width < 24 || height < 24) return null;
        float halfW = width / 2f, halfH = height / 2f;
        boolean front = w > 0.001f;
        float divisor = Math.max(Math.abs(w), 0.001f);
        float dx = x / divisor * halfW, dy = -y / divisor * halfH;
        boolean inside = front && Math.abs(dx) <= halfW - 10 && Math.abs(dy) <= halfH - 10;
        if (inside) return new Point(halfW + dx, halfH + dy, true);
        if (!edgeIndicators) return null;
        if (Math.abs(dx) + Math.abs(dy) < 0.001f) dy = halfH;
        float scale = Math.min((halfW - 10) / Math.max(Math.abs(dx), 0.001f),
            (halfH - 10) / Math.max(Math.abs(dy), 0.001f));
        return new Point(halfW + dx * scale, halfH + dy * scale, false);
    }
}
