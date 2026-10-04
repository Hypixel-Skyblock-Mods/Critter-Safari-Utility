package org.hypixelskyblockmods.crittersafariesp;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ModelBoundsTest {
    @Test void translatedRotatedScaledDisplayFitsVisibleModelRatherThanAnchor() {
        var bounds=ModelBounds.transformed(new AABB(0,0,0,1,1,1),new Matrix4f().translation(8,2,-3).rotateY((float)Math.PI/2).scale(2,1,3));
        assertEquals(8,bounds.minX,1e-5); assertEquals(11,bounds.maxX,1e-5);
        assertEquals(2,bounds.minY,1e-5); assertEquals(3,bounds.maxY,1e-5);
        assertEquals(-5,bounds.minZ,1e-5); assertEquals(-3,bounds.maxZ,1e-5);
    }
}
