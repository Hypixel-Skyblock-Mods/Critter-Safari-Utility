package org.hypixelskyblockmods.crittersafariesp;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SegmentGroupingTest {
    private SegmentGrouping.Node part(int id,String name,double x,boolean head) {
        return new SegmentGrouping.Node(id,"minecraft:silverfish",name,new Vec3(x,0,0),head);
    }
    @Test void groupsOneShywormButPreservesAnotherHeadAndOtherSpecies() {
        var groups=SegmentGrouping.group(List.of(part(1,"Shyworm",0,true),part(2,"silverfish",.7,false),
            part(3,"silverfish",1.4,false),part(4,"Shyworm",4,true),part(5,"silverfish",4.7,false),
            part(6,"Rockmite",.3,false),part(7,"silverfish",20,false)));
        assertEquals(4,groups.size());
        assertEquals(List.of(1,2,3),groups.get(0).stream().map(SegmentGrouping.Node::id).toList());
        assertEquals(List.of(4,5),groups.get(1).stream().map(SegmentGrouping.Node::id).toList());
    }
    @Test void repeatedShywormSegmentNamesProduceOneMarker() {
        assertEquals(1,SegmentGrouping.group(List.of(part(1,"Shyworm",0,false),part(2,"Shyworm",.7,false),
            part(3,"Shyworm",1.4,false))).size());
    }
}
