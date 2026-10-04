package org.hypixelskyblockmods.crittersafariesp;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

/** A swept trajectory estimate; the first terrain/entity collision ends the path. */
public final class Ballistics {
    public record Body(int id, AABB bounds, Vec3 velocity) {}
    public record Path(List<Vec3> points, Integer hitEntity, boolean impact) {}
    @FunctionalInterface public interface Terrain { Vec3 clip(Vec3 from, Vec3 to); }
    private Ballistics() {}

    public static Path predict(Vec3 start, Vec3 velocity, double gravity, double drag, double radius,
                               int ticks, List<Body> bodies, Terrain terrain) {
        return predict(start,velocity,gravity,drag,radius,Double.POSITIVE_INFINITY,ticks,bodies,terrain);
    }
    public static Path predict(Vec3 start,Vec3 velocity,double gravity,double drag,double radius,double maxFallSpeed,
                               int ticks,List<Body> bodies,Terrain terrain) {
        var expanded=bodies.stream().map(body -> new Body(body.id(),body.bounds().inflate(radius),body.velocity())).toList();
        var points = new ArrayList<Vec3>(); points.add(start);
        Vec3 position = start;
        for (int tick = 0; tick < ticks; tick++) {
            Vec3 next = position.add(velocity);
            Vec3 blockHit = terrain.clip(position, next);
            double first = blockHit == null ? Double.POSITIVE_INFINITY
                : fraction(position, next, blockHit);
            Integer hitId = null;
            for (Body body : expanded) {
                boolean moving=body.velocity().lengthSqr()>1e-12;
                Vec3 relativeStart=moving?position.subtract(body.velocity().scale(tick)):position;
                Vec3 relativeEnd=moving?next.subtract(body.velocity().scale(tick+1)):next;
                AABB box=body.bounds();
                Vec3 hit = box.contains(relativeStart) ? relativeStart : box.clip(relativeStart, relativeEnd).orElse(null);
                if (hit == null) continue;
                double at = fraction(relativeStart, relativeEnd, hit);
                // Terrain wins a tie, preventing a target just beyond a wall from turning green.
                if (at < first) { first = at; hitId = body.id(); }
            }
            if (Double.isFinite(first)) {
                points.add(position.lerp(next, first));
                return new Path(List.copyOf(points), hitId, true);
            }
            points.add(next); position = next;
            Vec3 slowed=velocity.scale(drag);
            velocity = new Vec3(slowed.x,Math.max(-maxFallSpeed,slowed.y-gravity),slowed.z);
        }
        return new Path(List.copyOf(points), null, false);
    }
    private static double fraction(Vec3 from, Vec3 to, Vec3 point) {
        double length = from.distanceToSqr(to);
        return length < 1e-12 ? 0 : Math.clamp(Math.sqrt(from.distanceToSqr(point) / length), 0, 1);
    }
}
