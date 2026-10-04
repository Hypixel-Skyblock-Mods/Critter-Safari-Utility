package org.hypixelskyblockmods.crittersafariesp;

import net.minecraft.world.phys.Vec3;
import java.util.*;

/** A named Shyworm head owns its adjacent chain of matching anonymous body models. */
public final class SegmentGrouping {
    public record Node(int id,String type,String name,Vec3 point,boolean nameplateHead) {}
    private SegmentGrouping() {}
    public static List<List<Node>> group(List<Node> nodes) {
        var result=new ArrayList<List<Node>>(); var claimed=new HashSet<Integer>();
        var heads=nodes.stream().filter(node -> node.name().equalsIgnoreCase("Shyworm"))
            .sorted(Comparator.comparing(Node::nameplateHead).reversed().thenComparingInt(Node::id)).toList();
        for(var head:heads) {
            if(!claimed.add(head.id())) continue;
            var parts=new ArrayList<Node>(); parts.add(head);
            for(int index=0;index<parts.size();index++) {
                Node last=parts.get(index);
                for(var candidate:nodes) {
                    if(claimed.contains(candidate.id()) || candidate.nameplateHead() || !candidate.type().equals(head.type())) continue;
                    if(!candidate.name().equalsIgnoreCase("Shyworm") && !candidate.name().equals(CritterTracker.readable(candidate.type()))) continue;
                    if(candidate.point().distanceToSqr(head.point())>36 || Math.abs(candidate.point().y-last.point().y)>.5
                        || candidate.point().distanceToSqr(last.point())>1) continue;
                    if(heads.stream().anyMatch(other -> other.id()!=head.id() && other.nameplateHead()
                        && other.point().distanceToSqr(candidate.point())<head.point().distanceToSqr(candidate.point()))) continue;
                    claimed.add(candidate.id()); parts.add(candidate);
                }
            }
            result.add(List.copyOf(parts));
        }
        for(var node:nodes) if(claimed.add(node.id())) result.add(List.of(node));
        return List.copyOf(result);
    }
}
