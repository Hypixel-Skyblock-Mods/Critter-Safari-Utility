package org.hypixelskyblockmods.crittersafariesp;

/** Positions are fractions of the usable area, so the badge stays visible after a resize. */
public final class HudPlacement {
    private HudPlacement() {}
    private static int margin(int screen, int element) { return Math.min(6, Math.max(0, (screen - element) / 2)); }
    private static int span(int screen, int element) { return Math.max(0, screen - element - 2 * margin(screen, element)); }
    public static int pixel(float fraction, int screen, int element) {
        return margin(screen, element) + Math.round(Math.clamp(fraction, 0, 1) * span(screen, element));
    }
    public static float fraction(double pixel, int screen, int element) {
        int span = span(screen, element);
        return span == 0 ? 0 : (float)Math.clamp((pixel - margin(screen, element)) / span, 0, 1);
    }
}
