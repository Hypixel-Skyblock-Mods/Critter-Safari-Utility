package org.hypixelskyblockmods.crittersafariesp;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BallisticsTest {
    @Test void revisedGravityLowersLateFlightMoreThanTheStart() {
        var settings=new Settings(); assertEquals(.063,settings.ballGravity,1e-6);
        var old=Ballistics.predict(Vec3.ZERO,new Vec3(0,0,1.5),.06,1,0,30,List.of(),(a,b)->null);
        var revised=Ballistics.predict(Vec3.ZERO,new Vec3(0,0,1.5),settings.ballGravity,1,0,30,List.of(),(a,b)->null);
        assertEquals(old.points().get(1),revised.points().get(1));
        double early=old.points().get(5).y-revised.points().get(5).y;
        double late=old.points().get(30).y-revised.points().get(30).y;
        assertTrue(early>0 && early<.04); assertTrue(late>1.3 && late<1.31);
        assertEquals(old.points().getLast().z,revised.points().getLast().z,1e-6);
    }
    @Test void defaultTrajectoryKeepsCurvingAfterTheMidpointWithoutAFallSpeedCap() {
        var settings=new Settings();
        var path=Ballistics.predict(Vec3.ZERO,new Vec3(0,0,settings.ballSpeed),settings.ballGravity,settings.ballDrag,0,40,List.of(),(a,b)->null);
        for(int tick=1;tick<=40;tick++) {
            assertEquals(-settings.ballGravity*tick*(tick-1)/2,path.points().get(tick).y,1e-5);
            assertEquals(settings.ballSpeed*tick,path.points().get(tick).z,1e-5);
        }
        assertTrue(path.points().get(40).y-path.points().get(39).y < path.points().get(20).y-path.points().get(19).y);
    }
    @Test void revisedTailKeepsEarlyDropButTravelsFurtherWithoutAnIncreasingFallSpeed() {
        var revised=Ballistics.predict(Vec3.ZERO,new Vec3(0,0,1.5),.06,1,0,.35,30,List.of(),(a,b)->null);
        var unbounded=Ballistics.predict(Vec3.ZERO,new Vec3(0,0,1.5),.06,1,0,30,List.of(),(a,b)->null);
        var old=Ballistics.predict(Vec3.ZERO,new Vec3(0,0,1.5),.06,.99,0,30,List.of(),(a,b)->null);
        for(int i=0;i<=6;i++) assertEquals(unbounded.points().get(i),revised.points().get(i));
        assertTrue(revised.points().getLast().z>old.points().getLast().z);
        assertTrue(revised.points().getLast().y>unbounded.points().getLast().y);
        for(int i=7;i<revised.points().size();i++)
            assertEquals(-.35,revised.points().get(i).y-revised.points().get(i-1).y,1e-6);
    }
    @Test void firstEntityWinsAndFastThrowsCannotTunnelThroughIt() {
        var near = new Ballistics.Body(1, new AABB(3,-1,-1,4,1,1), Vec3.ZERO);
        var far = new Ballistics.Body(2, new AABB(6,-1,-1,7,1,1), Vec3.ZERO);
        var path = Ballistics.predict(Vec3.ZERO, new Vec3(10,0,0), 0,1,0,10,List.of(far,near),(a,b)->null);
        assertEquals(1, path.hitEntity()); assertEquals(3, path.points().getLast().x, 1e-6);
    }
    @Test void wallPreventsAFalseGreenTargetBehindIt() {
        var target = new Ballistics.Body(1, new AABB(3,-1,-1,4,1,1), Vec3.ZERO);
        var path = Ballistics.predict(Vec3.ZERO,new Vec3(10,0,0),0,1,0,10,List.of(target),(a,b)->new Vec3(2,0,0));
        assertNull(path.hitEntity()); assertTrue(path.impact()); assertEquals(2,path.points().getLast().x,1e-6);
    }
    @Test void predictsRelativeMotionInsteadOfUsingOnlyTheCurrentHitbox() {
        var moving = new Ballistics.Body(9,new AABB(2,-.25,1.5,3,.25,2.5),new Vec3(0,0,-2));
        var path = Ballistics.predict(Vec3.ZERO,new Vec3(2.5,0,0),0,1,.125,10,List.of(moving),(a,b)->null);
        assertEquals(9,path.hitEntity());
    }
    @Test void dragAndGravityProduceACurvedMissPath() {
        var path = Ballistics.predict(Vec3.ZERO,new Vec3(1,0,0),.1,.9,0,3,List.of(),(a,b)->null);
        assertFalse(path.impact()); assertNull(path.hitEntity());
        assertEquals(2.71,path.points().getLast().x,1e-6);
        assertEquals(-.29,path.points().getLast().y,1e-6);
    }
}
