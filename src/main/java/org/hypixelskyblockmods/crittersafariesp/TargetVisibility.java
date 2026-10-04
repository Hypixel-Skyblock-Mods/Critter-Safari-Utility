package org.hypixelskyblockmods.crittersafariesp;

import net.minecraft.world.phys.Vec3;

/** Angular selection only; ESP labels deliberately do not test terrain visibility. */
public final class TargetVisibility {
    private TargetVisibility() {}
    public static double nameHalfAngle(double distance) {
        return Math.min(40, 5 + Math.toDegrees(Math.atan2(3, Math.max(0, distance))));
    }
    public static boolean showName(Vec3 eye, Vec3 look, Vec3 target) {
        Vec3 offset=target.subtract(eye);
        return offset.lengthSqr()>1e-8 && offset.normalize().dot(look.normalize())
            >= Math.cos(Math.toRadians(nameHalfAngle(offset.length())));
    }
    public static boolean inFront(Vec3 eye, Vec3 look, Vec3 target) {
        return target.subtract(eye).dot(look)>0;
    }
}
