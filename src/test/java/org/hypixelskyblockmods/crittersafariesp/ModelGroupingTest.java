package org.hypixelskyblockmods.crittersafariesp;
import net.minecraft.world.phys.AABB;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ModelGroupingTest {
    @Test void helperAndMobBecomeOneLargerBoxButAnotherNamedCritterStaysSeparate() {
        var helper=new ModelGrouping.Node(1,"Flitter",new AABB(0,0,0,1,1,1),true,true,true);
        var mob=new ModelGrouping.Node(2,"bat",new AABB(.2,0,.2,.7,.6,.7),false,false,false);
        var neighbor=new ModelGrouping.Node(3,"Flitter",new AABB(.8,0,0,1.8,1,1),true,true,true);
        var groups=ModelGrouping.group(List.of(helper,mob,neighbor));
        assertEquals(2,groups.size()); assertEquals(1,groups.getFirst().representative().id());
        assertEquals(2,groups.getFirst().parts().size());
    }
    @Test void unrelatedCreaturesAndDistantHelpersNeverMerge() {
        var a=new ModelGrouping.Node(1,"Honeybug",new AABB(0,0,0,1,1,1),true,false,true);
        var b=new ModelGrouping.Node(2,"Flitter",new AABB(0,0,0,1,1,1),true,true,false);
        var c=new ModelGrouping.Node(3,"bee",new AABB(10,0,0,11,1,1),false,false,false);
        assertEquals(3,ModelGrouping.group(List.of(a,b,c)).size());
    }
}
