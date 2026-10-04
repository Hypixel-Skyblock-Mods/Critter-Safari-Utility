package org.hypixelskyblockmods.crittersafariesp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudPlacementTest {
    @Test void draggedPositionRoundTripsAndStaysInsideResizedWindows() {
        float position = HudPlacement.fraction(250, 640, 100);
        assertEquals(250, HudPlacement.pixel(position, 640, 100));
        int resized = HudPlacement.pixel(position, 320, 100);
        assertTrue(resized >= 6 && resized + 100 <= 314);
        assertEquals(6, HudPlacement.pixel(-1, 320, 100));
        assertEquals(214, HudPlacement.pixel(2, 320, 100));
    }
    @Test void windowsSmallerThanBadgeDoNotProduceInvalidFractions() {
        assertEquals(0, HudPlacement.fraction(100, 20, 100));
        assertEquals(0, HudPlacement.pixel(1, 20, 100));
    }
}
