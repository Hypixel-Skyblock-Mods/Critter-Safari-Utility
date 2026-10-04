package org.hypixelskyblockmods.crittersafariesp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MotionSmoothingTest {
    @Test void sameResponseAcrossFrameRates() {
        double twoFrames = 1 - Math.pow(1 - MotionSmoothing.factor(16, 70), 2);
        assertEquals(MotionSmoothing.factor(32, 70), twoFrames, 1e-12);
    }
    @Test void zeroResponseUsesVanillaInterpolationWithoutAdditionalLag() {
        assertEquals(1, MotionSmoothing.factor(16, 0));
        assertEquals(0, MotionSmoothing.factor(0, 70));
        assertTrue(MotionSmoothing.factor(16, 70) > 0 && MotionSmoothing.factor(16, 70) < 1);
    }
}
