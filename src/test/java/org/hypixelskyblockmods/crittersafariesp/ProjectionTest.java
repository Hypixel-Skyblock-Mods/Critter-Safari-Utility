package org.hypixelskyblockmods.crittersafariesp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProjectionTest {
    @Test void mapsFrontFacingPointsToScreenAndFlipsY() {
        var center = Projection.toScreen(0, 0, 1, 800, 600, false);
        assertEquals(400, center.x()); assertEquals(300, center.y()); assertTrue(center.onScreen());
        var aboveRight = Projection.toScreen(0.5f, 0.5f, 1, 800, 600, false);
        assertEquals(600, aboveRight.x()); assertEquals(150, aboveRight.y());
    }
    @Test void offscreenAndBehindPointsProduceBoundedEdgeIndicators() {
        var right = Projection.toScreen(4, 0, 1, 800, 600, true);
        assertEquals(790, right.x()); assertEquals(300, right.y()); assertFalse(right.onScreen());
        var behind = Projection.toScreen(1, 1, -1, 800, 600, true);
        assertNotNull(behind); assertFalse(behind.onScreen());
        assertTrue(behind.x() >= 10 && behind.x() <= 790);
        assertTrue(behind.y() >= 10 && behind.y() <= 590);
        assertNull(Projection.toScreen(1, 1, -1, 800, 600, false));
    }
    @Test void directlyBehindAndNearPlaneNeverGenerateNaN() {
        var behind = Projection.toScreen(0, 0, -1, 800, 600, true);
        assertEquals(400, behind.x()); assertEquals(590, behind.y());
        var near = Projection.toScreen(1, 0, 0, 800, 600, true);
        assertEquals(790, near.x()); assertTrue(Float.isFinite(near.y()));
        assertNull(Projection.toScreen(Float.NaN, 0, 1, 800, 600, true));
    }
}
