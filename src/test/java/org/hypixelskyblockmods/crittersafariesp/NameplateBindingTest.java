package org.hypixelskyblockmods.crittersafariesp;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class NameplateBindingTest {
    @Test void labelsStayWithTheirOwnHeadsAndNeverRenameADifferentKnownCritter() {
        var gazer=new NameplateBinding.Plate(1,"Gazer",new Vec3(0,2,0));
        var gimmiegold=new NameplateBinding.Plate(2,"Gimmiegold",new Vec3(.65,2,0));
        var first=new NameplateBinding.Model(10,null,new AABB(-.2,1,-.2,.2,1.6,.2),true,false);
        var second=new NameplateBinding.Model(11,null,new AABB(.45,1,-.2,.85,1.6,.2),true,false);
        var bindings=NameplateBinding.bind(List.of(gazer,gimmiegold),List.of(second,first));
        assertEquals(10,bindings.get(1)); assertEquals(11,bindings.get(2));
        var namedOther=new NameplateBinding.Model(12,"Gimmiegold",first.bounds(),true,false);
        assertTrue(NameplateBinding.bind(List.of(gazer),List.of(namedOther)).isEmpty());
    }
    @Test void anonymousAndAmbiguousHeadsWaitForANameAndNpcsRemainRestricted() {
        var first=new NameplateBinding.Model(10,null,new AABB(-.4,1,-.2,0,1.6,.2),true,false);
        var second=new NameplateBinding.Model(11,null,new AABB(0,1,-.2,.4,1.6,.2),true,false);
        assertTrue(NameplateBinding.bind(List.of(),List.of(first,second)).isEmpty());
        var gazer=new NameplateBinding.Plate(1,"Gazer",new Vec3(0,2,0));
        assertTrue(NameplateBinding.bind(List.of(gazer),List.of(first,second)).isEmpty());
        var npc=new NameplateBinding.Model(12,null,new AABB(-.3,0,-.3,.3,1.8,.3),false,true);
        assertTrue(NameplateBinding.bind(List.of(gazer),List.of(npc)).isEmpty());
        assertEquals(12,NameplateBinding.bind(List.of(new NameplateBinding.Plate(2,"Scrappy",gazer.position())),List.of(npc)).get(2));
    }
    @Test void aTranslatedHeadUsesItsVisiblePositionRatherThanItsEntityAnchor() {
        var plate=new Vec3(4,2,0);
        var local=new AABB(-.5,-.5,-.5,.5,.5,.5);
        assertFalse(Double.isFinite(NameplateBinding.score(plate,local,true)));
        var visible=ModelBounds.transformed(local,new Matrix4f().translation(4,1,0));
        assertEquals(.0625,NameplateBinding.score(plate,visible,true),1e-6);
    }
    @Test void aDifferentHeadBesideTheNameplateOrAnUnrelatedVerticalModelIsRejected() {
        var plate=new Vec3(0,2,0);
        assertTrue(Double.isFinite(NameplateBinding.score(plate,new AABB(-.3,1,-.3,.3,1.6,.3),true)));
        assertFalse(Double.isFinite(NameplateBinding.score(plate,new AABB(1,1,-.3,1.6,1.6,.3),true)));
        assertFalse(Double.isFinite(NameplateBinding.score(plate,new AABB(-.3,4,-.3,.3,4.6,.3),true)));
        assertFalse(Double.isFinite(NameplateBinding.score(plate,new AABB(-.3,-3,-.3,.3,-2.4,.3),true)));
    }
}
