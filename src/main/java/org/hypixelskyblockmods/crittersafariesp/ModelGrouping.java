package org.hypixelskyblockmods.crittersafariesp;

import net.minecraft.world.phys.AABB;
import java.util.*;

/** Collapse an overlapping helper/model pair, while keeping independently labelled critters separate. */
public final class ModelGrouping {
    public record Node(int id,String name,AABB bounds,boolean named,boolean helper,boolean nameplateHead) {}
    public record Group(Node representative,String name,List<Node> parts) {}
    private ModelGrouping() {}
    public static List<Group> group(List<Node> nodes) {
        var result=new ArrayList<Group>(); var claimed=new HashSet<Integer>();
        var ordered=new ArrayList<>(nodes);
        ordered.sort(Comparator.comparing(Node::nameplateHead).reversed()
            .thenComparing(Comparator.comparing(Node::named).reversed()).thenComparingInt(Node::id));
        for(var seed:ordered) {
            if(!claimed.add(seed.id())) continue;
            var parts=new ArrayList<Node>(); parts.add(seed);
            if(seed.named()) for(var candidate:ordered) {
                if(claimed.contains(candidate.id()) || candidate.nameplateHead()
                    || (candidate.named() && !candidate.name().equalsIgnoreCase(seed.name()))
                    || (!seed.helper() && !candidate.helper())) continue;
                if(seed.bounds().getCenter().distanceToSqr(candidate.bounds().getCenter())>2.25
                    || !seed.bounds().inflate(.2).intersects(candidate.bounds())) continue;
                // An anonymous body nearer a different explicit head belongs to that critter instead.
                double distance=seed.bounds().getCenter().distanceToSqr(candidate.bounds().getCenter());
                if(nodes.stream().anyMatch(other -> other.id()!=seed.id() && other.nameplateHead()
                    && other.bounds().getCenter().distanceToSqr(candidate.bounds().getCenter())<distance)) continue;
                claimed.add(candidate.id()); parts.add(candidate);
            }
            Node representative=parts.stream().max(Comparator.comparingDouble((Node node) -> volume(node.bounds()))
                .thenComparingInt(Node::id)).orElseThrow();
            result.add(new Group(representative,seed.name(),List.copyOf(parts)));
        }
        return List.copyOf(result);
    }
    private static double volume(AABB box) { return box.getXsize()*box.getYsize()*box.getZsize(); }
}
