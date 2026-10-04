package org.hypixelskyblockmods.crittersafariesp;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Comparator;
import java.util.List;

public final class CapsulePreview {
    public record Preview(Ballistics.Path path,AimPrediction.Aim aim,Integer aimEntity) {}
    private record Lead(AimPrediction.Aim aim,int entity) {}
    private record Scene(Vec3 start,List<Ballistics.Body> bodies,Ballistics.Terrain terrain) {}
    private record Candidate(CritterTracker.Target target,Vec3 center,double alignment) {}
    private CapsulePreview() {}
    public static boolean isCapsule(Entity entity) {
        if (entity instanceof Display.ItemDisplay display && display.itemRenderState() != null)
            return isCapsule(display.itemRenderState().itemStack());
        return entity instanceof ItemEntity item && isCapsule(item.getItem());
    }
    public static boolean isCapsule(ItemStack item) {
        if (item.isEmpty()) return false;
        var data = item.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            var tag = data.copyTag();
            String id = tag.getCompoundOrEmpty("ExtraAttributes").getStringOr("id", tag.getStringOr("id", ""));
            if (id.equals("CRITTER_CAPSULE") || id.equals("MASTERFUL_CRITTER_CAPSULE")) return true;
            if (!id.isEmpty()) return false;
        }
        String name = DetectionRules.plain(item.getHoverName().getString());
        return name.equalsIgnoreCase("Critter Capsule") || name.equalsIgnoreCase("Masterful Critter Capsule");
    }
    public static Ballistics.Path predict(Minecraft mc, Settings settings, float partialTick) {
        var scene=scene(mc,settings,partialTick);
        return scene==null?null:predict(scene,settings,mc.player.getViewVector(partialTick));
    }
    public static Preview preview(Minecraft mc,Settings settings,float partialTick) {
        var scene=scene(mc,settings,partialTick);
        if(scene==null) return new Preview(null,null,null);
        var lead=lead(mc,settings,partialTick,scene);
        return new Preview(predict(scene,settings,mc.player.getViewVector(partialTick)),
            lead==null?null:lead.aim(),lead==null?null:lead.entity());
    }
    private static Scene scene(Minecraft mc,Settings settings,float partialTick) {
        if (!settings.trajectory || mc.player==null || mc.level==null || !CritterSafariClient.tracker.allowed()
            || (!isCapsule(mc.player.getMainHandItem()) && !isCapsule(mc.player.getOffhandItem()))) return null;
        Vec3 start = mc.player.getEyePosition(partialTick).add(0, -0.1, 0);
        var bodies = new ArrayList<Ballistics.Body>();
        var selected = new HashMap<Integer,Integer>();
        double reach = Math.max(100, settings.radius);
        for (var target : CritterSafariClient.tracker.targets()) for(var part:target.parts()) selected.put(part.getId(),target.entity().getId());
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || entity.isRemoved() || isCapsule(entity)
                || (!selected.containsKey(entity.getId()) && !entity.isPickable())) continue;
            var bounds = ModelBounds.of(entity,partialTick);
            if (bounds.getCenter().distanceToSqr(mc.player.position()) > reach * reach) continue;
            Vec3 motion = settings.predictMotion ? CritterSafariClient.tracker.motion(entity) : Vec3.ZERO;
            if (motion.lengthSqr() > 4) motion = Vec3.ZERO; // Teleports cannot supply a useful velocity estimate.
            if (selected.containsKey(entity.getId())) bounds=bounds.inflate(settings.hitTolerance);
            bodies.add(new Ballistics.Body(selected.getOrDefault(entity.getId(),entity.getId()), bounds, motion));
        }
        return new Scene(start,List.copyOf(bodies),(from, to) -> {
                if (!mc.level.hasChunkAt(BlockPos.containing(to))) return to;
                var hit = mc.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
                return hit.getType() == HitResult.Type.MISS ? null : hit.getLocation();
            });
    }
    private static Ballistics.Path predict(Scene scene,Settings settings,Vec3 direction) {
        return Ballistics.predict(scene.start(),direction.scale(settings.ballSpeed),settings.ballGravity,settings.ballDrag,
            settings.ballRadius,settings.predictionTicks,scene.bodies(),scene.terrain());
    }
    public static AimPrediction.Aim lead(Minecraft mc,Settings settings,float partialTick) {
        var scene=scene(mc,settings,partialTick);
        var lead=scene==null?null:lead(mc,settings,partialTick,scene);
        return lead==null?null:lead.aim();
    }
    private static Lead lead(Minecraft mc,Settings settings,float partialTick,Scene scene) {
        if (!settings.aimCircle) return null;
        Vec3 start=scene.start(),look=mc.player.getViewVector(partialTick);
        var candidates=new ArrayList<Candidate>();
        for(var target:CritterSafariClient.tracker.targets()) {
            if(NpcCritters.interactionOnly(target)) continue;
            Vec3 center=target.bounds(partialTick).getCenter();
            double targetAlignment=center.subtract(start).normalize().dot(look);
            if(TargetVisibility.inFront(start,look,center)) candidates.add(new Candidate(target,center,targetAlignment));
        }
        candidates.sort(Comparator.comparingDouble(Candidate::alignment).reversed());
        for(var candidate:candidates) {
            var target=candidate.target();
            Vec3 motion=settings.predictMotion?CritterSafariClient.tracker.motion(target.entity()):Vec3.ZERO;
            var aim=AimPrediction.solve(start,candidate.center(),motion,Vec3.ZERO,
                settings.ballSpeed,settings.ballGravity,settings.ballDrag,settings.predictionTicks);
            if(aim!=null && hits(scene,settings,aim,target.entity().getId())) return new Lead(aim,target.entity().getId());
            // A low arc blocked by a short obstacle can still have a clear high arc.
            var high=AimPrediction.solveHighArc(start,candidate.center(),motion,settings.ballSpeed,
                settings.ballGravity,settings.ballDrag,settings.predictionTicks);
            if(high!=null && hits(scene,settings,high,target.entity().getId())) return new Lead(high,target.entity().getId());
        }
        return null;
    }
    private static boolean hits(Scene scene,Settings settings,AimPrediction.Aim aim,int target) {
        return Integer.valueOf(target).equals(predict(scene,settings,aim.direction()).hitEntity());
    }
}
