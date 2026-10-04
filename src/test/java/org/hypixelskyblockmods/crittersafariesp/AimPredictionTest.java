package org.hypixelskyblockmods.crittersafariesp;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AimPredictionTest {
    @Test void highArcClearsObstacleBlockingLowArcAndStillHitsMovingTarget() {
        Vec3 target=new Vec3(0,0,15),motion=new Vec3(.05,0,0);
        var low=AimPrediction.solve(Vec3.ZERO,target,motion,Vec3.ZERO,1.5,.063,1,80);
        var high=AimPrediction.solveHighArc(Vec3.ZERO,target,motion,1.5,.063,1,80);
        assertNotNull(low); assertNotNull(high); assertTrue(high.ticks()>low.ticks());
        var body=new Ballistics.Body(7,new net.minecraft.world.phys.AABB(-.5,-.5,14.5,.5,.5,15.5),motion);
        var wall=new net.minecraft.world.phys.AABB(-10,-10,6,10,4,7);
        Ballistics.Terrain terrain=(a,b)->wall.clip(a,b).orElse(null);
        assertNull(Ballistics.predict(Vec3.ZERO,low.direction().scale(1.5),.063,1,.125,80,java.util.List.of(body),terrain).hitEntity());
        assertEquals(7,Ballistics.predict(Vec3.ZERO,high.direction().scale(1.5),.063,1,.125,80,java.util.List.of(body),terrain).hitEntity());
        assertNull(AimPrediction.solveHighArc(Vec3.ZERO,new Vec3(0,0,200),Vec3.ZERO,1.5,.063,1,80));
    }
    @Test void fullParabolicLeadRespondsToSpeedAndAllMovementDirections() {
        Vec3 target=new Vec3(0,0,16);
        var slow=AimPrediction.solve(Vec3.ZERO,target,new Vec3(.08,0,0),Vec3.ZERO,1.5,.06,1,80);
        var fast=AimPrediction.solve(Vec3.ZERO,target,new Vec3(.3,0,0),Vec3.ZERO,1.5,.06,1,80);
        assertNotNull(slow); assertNotNull(fast); assertTrue(fast.direction().x>slow.direction().x);
        for(Vec3 motion:new Vec3[]{new Vec3(.3,0,0),new Vec3(-.3,0,0),new Vec3(0,.08,.2),new Vec3(0,-.08,-.2),Vec3.ZERO}) {
            var aim=AimPrediction.solve(Vec3.ZERO,target,motion,Vec3.ZERO,1.5,.06,1,80);
            assertNotNull(aim);
            Vec3 point=Vec3.ZERO,velocity=aim.direction().scale(1.5);
            for(int i=0;i<(int)aim.ticks();i++) { point=point.add(velocity); velocity=velocity.add(0,-.06,0); }
            point=point.add(velocity.scale(aim.ticks()-(int)aim.ticks()));
            assertTrue(point.distanceTo(target.add(motion.scale(aim.ticks())))<1e-6);
            if(motion.x!=0) assertEquals(Math.signum(motion.x),Math.signum(aim.direction().x));
        }
    }
    @Test void terminalFallAimMatchesForwardPhysicsAboveAndBelowIncludingMovingTargets() {
        for(double drag:new double[]{1,.99,.8}) for(double y:new double[]{-3,0,3}) {
            Vec3 target=new Vec3(0,y,16),motion=new Vec3(.15,.01,0);
            var aim=AimPrediction.solve(Vec3.ZERO,target,motion,Vec3.ZERO,1.5,.06,drag,.35,120);
            if(drag==.8) { assertNull(aim); continue; } // Horizontal reach is less than eight blocks.
            assertNotNull(aim,"No intercept at drag="+drag+", y="+y);
            Vec3 point=Vec3.ZERO,velocity=aim.direction().scale(1.5);
            int whole=(int)aim.ticks();
            for(int i=0;i<whole;i++) {
                point=point.add(velocity);
                Vec3 slowed=velocity.scale(drag); velocity=new Vec3(slowed.x,Math.max(-.35,slowed.y-.06),slowed.z);
            }
            point=point.add(velocity.scale(aim.ticks()-whole));
            assertTrue(point.distanceTo(target.add(motion.scale(aim.ticks())))<1e-6);
            assertTrue(aim.ticks()<30,"Selected a high arc instead of the early intercept");
        }
    }
    @Test void circleLeadsSidewaysMotionAndCompensatesForDrop() {
        Vec3 target=new Vec3(0,0,16),motion=new Vec3(.15,0,0);
        var aim=AimPrediction.solve(Vec3.ZERO,target,motion,Vec3.ZERO,1.5,.06,.99,80);
        assertNotNull(aim); assertTrue(aim.direction().x>0); assertTrue(aim.direction().y>0);
        Vec3 point=Vec3.ZERO,velocity=aim.direction().scale(1.5);
        int whole=(int)aim.ticks();
        for(int i=0;i<whole;i++) { point=point.add(velocity); velocity=velocity.scale(.99).add(0,-.06,0); }
        point=point.add(velocity.scale(aim.ticks()-whole));
        assertTrue(point.distanceTo(target.add(motion.scale(aim.ticks())))<1e-6);
    }
    @Test void impossibleOrTeleportingTargetsHaveNoAimCircle() {
        assertNull(AimPrediction.solve(Vec3.ZERO,new Vec3(0,0,100),new Vec3(0,0,1.9),Vec3.ZERO,1.5,.06,.99,80));
        assertNull(AimPrediction.solve(Vec3.ZERO,new Vec3(0,0,5),new Vec3(10,0,0),Vec3.ZERO,1.5,.06,.99,80));
    }
}
