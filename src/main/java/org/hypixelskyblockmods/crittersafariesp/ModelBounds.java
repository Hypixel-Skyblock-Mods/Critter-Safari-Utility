package org.hypixelskyblockmods.crittersafariesp;

import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Display anchors have zero-sized collision boxes; use their transformed visible model instead. */
public final class ModelBounds {
    private ModelBounds() {}
    public static AABB of(Entity entity, float partialTick) {
        var position = entity.getPosition(partialTick);
        if (entity instanceof Display display && display.renderState() != null) {
            var transformation = display.renderState().transformation().get(display.calculateInterpolationProgress(partialTick));
            AABB local = entity instanceof Display.BlockDisplay ? new AABB(0,0,0,1,1,1) : new AABB(-.5,-.5,-.5,.5,.5,.5);
            Matrix4f matrix = new Matrix4f().rotateY((float)Math.toRadians(-entity.getYRot()))
                .rotateX((float)Math.toRadians(entity.getXRot())).mul(transformation.getMatrix());
            return transformed(local, matrix).move(position);
        }
        return entity.getBoundingBox().move(position.subtract(entity.position()));
    }
    public static AABB transformed(AABB box, Matrix4f matrix) {
        double x0=Double.POSITIVE_INFINITY,y0=x0,z0=x0,x1=Double.NEGATIVE_INFINITY,y1=x1,z1=x1;
        for (int i=0;i<8;i++) {
            var p=matrix.transformPosition(new Vector3f((float)((i&1)==0?box.minX:box.maxX),
                (float)((i&2)==0?box.minY:box.maxY),(float)((i&4)==0?box.minZ:box.maxZ)));
            x0=Math.min(x0,p.x); y0=Math.min(y0,p.y); z0=Math.min(z0,p.z);
            x1=Math.max(x1,p.x); y1=Math.max(y1,p.y); z1=Math.max(z1,p.z);
        }
        return new AABB(x0,y0,z0,x1,y1,z1);
    }
    public static double distanceSquared(Entity entity, Entity observer) { return of(entity,1).getCenter().distanceToSqr(observer.position()); }
}
