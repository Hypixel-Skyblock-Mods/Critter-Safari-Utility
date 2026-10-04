package org.hypixelskyblockmods.crittersafariesp;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TargetVisibilityTest {
    private static Vec3 at(double angle,double distance) {
        return new Vec3(Math.sin(Math.toRadians(angle))*distance,0,Math.cos(Math.toRadians(angle))*distance);
    }
    @Test void distantDirectLookShowsNameWhileNearbyConeIsWider() {
        var look=new Vec3(0,0,1);
        assertTrue(TargetVisibility.showName(Vec3.ZERO,look,at(0,50)));
        assertTrue(TargetVisibility.showName(Vec3.ZERO,look,at(20,5)));
        assertFalse(TargetVisibility.showName(Vec3.ZERO,look,at(20,50)));
        assertTrue(TargetVisibility.nameHalfAngle(5)>TargetVisibility.nameHalfAngle(10));
        assertTrue(TargetVisibility.nameHalfAngle(10)>TargetVisibility.nameHalfAngle(50));
        assertEquals(40,TargetVisibility.nameHalfAngle(0));
        assertFalse(TargetVisibility.showName(Vec3.ZERO,look,at(180,5)));
        assertFalse(TargetVisibility.showName(Vec3.ZERO,look,Vec3.ZERO));
    }
    @Test void aimCandidatesHaveNoThirtyDegreeCutoff() {
        var look=new Vec3(0,0,1);
        assertTrue(TargetVisibility.inFront(Vec3.ZERO,look,at(45,10)));
        assertTrue(TargetVisibility.inFront(Vec3.ZERO,look,at(80,10)));
        assertFalse(TargetVisibility.inFront(Vec3.ZERO,look,at(120,10)));
    }
}
