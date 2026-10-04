package org.hypixelskyblockmods.crittersafariesp;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Associate a known floating name with the visible model, not a display's untransformed anchor. */
public final class NameplateBinding {
    public record Plate(int id,String name,Vec3 position) {}
    public record Model(int id,String name,AABB bounds,boolean head,boolean anonymousNpc) {}
    private record Choice(Model model,double score,double second) {}
    private record Owner(String name,double score,boolean ambiguous) {}
    private NameplateBinding() {}
    /** One geometry pass per plate/model pair; no repeated entity scans or display transforms. */
    public static Map<Integer,Integer> bind(List<Plate> plates,List<Model> models) {
        var choices=new HashMap<Integer,Choice>(); var owners=new HashMap<Integer,Owner>();
        for(var plate:plates) {
            Model nearest=null; double best=Double.POSITIVE_INFINITY,second=best;
            for(var model:models) {
                if(model.name()!=null && !model.name().equalsIgnoreCase(plate.name())) continue;
                if(model.anonymousNpc() && !plate.name().equalsIgnoreCase("Hideyho") && !plate.name().equalsIgnoreCase("Scrappy")) continue;
                double score=score(plate.position(),model.bounds(),model.head());
                if(!Double.isFinite(score)) continue;
                var owner=owners.get(model.id());
                if(owner==null || score<owner.score()-.04) owners.put(model.id(),new Owner(plate.name(),score,false));
                else if(Math.abs(score-owner.score())<=.04 && !owner.name().equalsIgnoreCase(plate.name()))
                    owners.put(model.id(),new Owner(owner.name(),Math.min(score,owner.score()),true));
                if(score<best) { second=best; best=score; nearest=model; }
                else second=Math.min(second,score);
            }
            if(nearest!=null) choices.put(plate.id(),new Choice(nearest,best,second));
        }
        var result=new HashMap<Integer,Integer>();
        for(var entry:choices.entrySet()) {
            var choice=entry.getValue(); var owner=owners.get(choice.model().id());
            if(owner.ambiguous() || choice.score()>owner.score()+.04) continue;
            // Two equally plausible heads give no identity: wait instead of marking the wrong one.
            if(choice.model().head() && choice.second()-choice.score()<=.04) continue;
            result.put(entry.getKey(),choice.model().id());
        }
        return Map.copyOf(result);
    }
    public static double score(Vec3 plate,AABB model,boolean head) {
        var center=model.getCenter();
        double dx=center.x-plate.x,dz=center.z-plate.z,dy=plate.y-model.maxY;
        double horizontal=dx*dx+dz*dz;
        double radius=head?.75:2;
        if(horizontal>radius*radius || dy<-.5 || dy>3) return Double.POSITIVE_INFINITY;
        return horizontal+dy*dy*.25;
    }
}
