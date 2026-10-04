package org.hypixelskyblockmods.crittersafariesp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import java.util.*;

public final class CritterTracker {
    public boolean allowed() { return allowed; }
    public record Target(Entity entity, String type, String name,List<Entity> parts) {
        public Target(Entity entity,String type,String name) { this(entity,type,name,List.of(entity)); }
        public SafariSpecies species() { return RockmiteMounds.isMound(this)?SafariSpecies.ROCKMITE:SafariSpecies.named(name); }
        public AABB bounds(float partialTick) {
            AABB box=ModelBounds.of(entity,partialTick);
            if(name.equalsIgnoreCase("Shyworm")) for(var part:parts) if(part!=entity && !part.isRemoved()) box=box.minmax(ModelBounds.of(part,partialTick));
            return box;
        }
    }
    private Map<Integer, Target> targets = Map.of();
    private final Map<Entity,String> speciesNames=new IdentityHashMap<>();
    private final SpeciesHints speciesHints=new SpeciesHints();
    private final Map<Integer, Vec3> positions = new HashMap<>(), motions = new HashMap<>();
    public Vec3 motion(Entity entity) { return motions.getOrDefault(entity.getId(), entity.getDeltaMovement()); }
    private ClientLevel previousWorld;
    private int ticks;
    private boolean allowed;
    private String status = "No world loaded";

    private Map<Integer,Target> filteredSource;
    private long filteredRevision=-1;
    private Collection<Target> visibleTargets=List.of();
    public Collection<Target> targets() {
        var run=CritterSafariClient.run;
        if(filteredSource!=targets || filteredRevision!=run.revision()) {
            filteredSource=targets; filteredRevision=run.revision();
            visibleTargets=targets.values().stream().filter(t -> run.enabled(t.species())
                && !(RockmiteMounds.isMound(t) && run.caught(SafariSpecies.ROCKMITE))).toList();
        }
        return visibleTargets;
    }
    public String status() { return status; }
    public void refresh() { ticks = 0; }
    public void tick(Minecraft mc, Settings settings) {
        if (previousWorld != mc.level) {
            targets = Map.of();
            speciesNames.clear();
            speciesHints.clear();
            positions.clear(); motions.clear();
            previousWorld = mc.level;
            ticks = 0;
        }
        allowed = isAllowed(mc, settings);
        if (!allowed) { targets = Map.of(); positions.clear(); motions.clear(); return; }
        var present = new HashSet<Integer>();
        for (var target:targets.values()) for(Entity entity:target.parts()) {
            present.add(entity.getId());
            Vec3 point=ModelBounds.of(entity,1).getCenter(), previous=positions.put(entity.getId(),point);
            Vec3 motion=previous==null?entity.getDeltaMovement():point.subtract(previous);
            if (motion.lengthSqr()<1e-8) motion=entity.getDeltaMovement();
            motions.put(entity.getId(),motion.lengthSqr()>4?Vec3.ZERO:motion);
        }
        positions.keySet().retainAll(present); motions.keySet().retainAll(present);
        // Remove stale references every tick: a despawned entity ID may be reused immediately.
        Map<Integer, Target> surviving = new LinkedHashMap<>();
        for(var target:targets.values()) {
            var parts=target.parts().stream().filter(entity -> !entity.isRemoved() && !CapsulePreview.isCapsule(entity)
                && mc.level.getEntity(entity.getId())==entity && DetectionRules.inRange(ModelBounds.distanceSquared(entity,mc.player),settings.radius)).toList();
            if(!parts.isEmpty()) {
                Entity head=parts.contains(target.entity())?target.entity():parts.getFirst();
                surviving.put(head.getId(),new Target(head,target.type(),target.name(),parts));
            }
        }
        targets = Map.copyOf(surviving);
        if (ticks++ % settings.scanIntervalTicks != 0) return;
        scan(mc, settings);
    }

    private boolean isAllowed(Minecraft mc, Settings settings) {
        if (mc.level == null || mc.player == null) { status = "No world loaded"; return false; }
        if (!settings.enabled) { status = "Disabled"; return false; }
        if (!settings.safariOnly) { status = "Manual area override"; return true; }
        if (mc.getCurrentServer() == null || !DetectionRules.hypixelAddress(mc.getCurrentServer().ip)) {
            status = "Waiting for Hypixel"; return false;
        }
        if (mc.getConnection() != null) {
            for (var info : mc.getConnection().getOnlinePlayers()) {
                var line = info.getTabListDisplayName();
                if (line != null && DetectionRules.safariArea(line.getString())) {
                    status = "Critter Safari"; return true;
                }
            }
        }
        status = "Waiting for Safari tab Area";
        return false;
    }

    /** Area membership independent of ESP visibility, so disabling ESP doesn't start a new run. */
    public static boolean inArea(Minecraft mc,Settings settings) {
        if(mc.level==null || mc.player==null) return false;
        if(!settings.safariOnly) return true;
        if(mc.getCurrentServer()==null || !DetectionRules.hypixelAddress(mc.getCurrentServer().ip) || mc.getConnection()==null) return false;
        return mc.getConnection().getOnlinePlayers().stream().anyMatch(info -> info.getTabListDisplayName()!=null
            && DetectionRules.safariArea(info.getTabListDisplayName().getString()));
    }

    private void scan(Minecraft mc, Settings settings) {
        speciesNames.keySet().removeIf(entity -> entity.isRemoved() || mc.level.getEntity(entity.getId())!=entity);
        List<Entity> nearby = new ArrayList<>();
        Set<UUID> realPlayers = new HashSet<>();
        if (mc.getConnection() != null) {
            for (var info : mc.getConnection().getListedOnlinePlayers()) realPlayers.add(info.getProfile().id());
        }
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || entity.isRemoved() || !entity.isAlive() || CapsulePreview.isCapsule(entity)) continue;
            if (entity instanceof Display.ItemDisplay display && (display.itemRenderState()==null || display.itemRenderState().itemStack().isEmpty())) continue;
            if (entity instanceof Display.BlockDisplay display && (display.blockRenderState()==null || display.blockRenderState().blockState().isAir())) continue;
            if (entity instanceof Player player && realPlayers.contains(entity.getUUID()) && NpcCritters.identify(player)==null) continue;
            if (DetectionRules.inRange(ModelBounds.distanceSquared(entity,mc.player), settings.radius)) nearby.add(entity);
        }
        Set<String> approvedTypes = new HashSet<>(settings.entityTypes);
        Map<Integer, Target> found = new LinkedHashMap<>();
        Set<Integer> nameplateHeads=new HashSet<>();
        for (Entity entity : nearby) {
            String type = typeId(entity);
            String rawName = label(entity);
            if(!settings.detection.equals("TYPE") && !DetectionRules.critterLabel(rawName)) continue;
            if (DetectionRules.matchingName(rawName, settings.excludedNames) != null) continue;
            String name = DetectionRules.critterLabel(rawName)?DetectionRules.matchingName(rawName, settings.names):null;
            if(name==null && entity instanceof Player player) {
                var species=NpcCritters.identify(player);
                if(species!=null && DetectionRules.matchingName(species.title,settings.names)!=null) name=species.title;
            }
            if (name==null && entity instanceof Display.ItemDisplay display && display.itemRenderState()!=null) {
                var item=display.itemRenderState().itemStack();
                String itemId=BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
                if (itemId.equals("minecraft:green_shulker_box")) name="Hideonfloor";
                else if (itemId.equals("minecraft:purple_shulker_box")) name="Hideonwall";
                else if(RockmiteMounds.matches(display) && DetectionRules.matchingName("Rockmite",settings.names)!=null) name=RockmiteMounds.NAME;
                if (name!=null && DetectionRules.matchingName(name,settings.excludedNames)!=null) continue;
            }
            if(name==null) {
                var species=SpecialModels.identify(entity);
                if(species!=null && DetectionRules.matchingName(species.title,settings.names)!=null) name=species.title;
            }
            boolean typeMatch = approvedTypes.contains(type) && DetectionRules.typeFallback(type, settings.detection);
            if(SafariSpecies.named(name==null?"":name)!=null && entity.getCustomName()!=null) nameplateHeads.add(entity.getId());
            if(name==null) name=speciesNames.get(entity);
            if(name!=null && DetectionRules.matchingName(name,settings.excludedNames)!=null) continue;
            boolean nameMatch = name != null && !settings.detection.equals("TYPE");
            if (typeMatch || nameMatch) {
                if (entity instanceof ArmorStand || entity instanceof Display.TextDisplay) {
                    if (!nameMatch) continue;
                    Entity body = nearestBody(entity, nearby, settings);
                    if (body != null) {
                        found.put(body.getId(), new Target(body, typeId(body), name));
                        nameplateHeads.add(body.getId());
                    }
                } else {
                    Target previous = found.get(entity.getId());
                    if (previous == null || name != null) {
                        found.put(entity.getId(), new Target(entity, type, name != null ? name : readable(type)));
                    }
                }
            }
        }
        var inferredBodies=new HashSet<Integer>();
        for(var target:found.values()) speciesHints.observe(target.type(),SafariSpecies.named(target.name()));
        if(settings.detection.equals("BOTH")) for(var entry:found.entrySet()) {
            var target=entry.getValue();
            if(SafariSpecies.named(target.name())!=null || RockmiteMounds.isMound(target)) continue;
            var species=speciesHints.infer(target.type(),SafariAreas.at(ModelBounds.of(target.entity(),1).getCenter()));
            if(species!=null && DetectionRules.matchingName(species.title,settings.names)!=null
                && DetectionRules.matchingName(species.title,settings.excludedNames)==null) {
                inferredBodies.add(target.entity().getId());
                entry.setValue(new Target(target.entity(),target.type(),species.title,target.parts()));
            }
        }
        var nodes=found.values().stream().map(target -> new SegmentGrouping.Node(target.entity().getId(),target.type(),target.name(),
            ModelBounds.of(target.entity(),1).getCenter(),nameplateHeads.contains(target.entity().getId()))).toList();
        Map<Integer,Target> grouped=new LinkedHashMap<>();
        for(var group:SegmentGrouping.group(nodes)) {
            Target head=found.get(group.getFirst().id());
            var parts=group.stream().map(node -> found.get(node.id()).entity()).toList();
            grouped.put(head.entity().getId(),new Target(head.entity(),head.type(),head.name(),parts));
        }
        var modelNodes=grouped.values().stream().filter(target -> !target.name().equalsIgnoreCase("Shyworm"))
            .map(target -> new ModelGrouping.Node(target.entity().getId(),target.name(),target.bounds(1),
                DetectionRules.matchingName(target.name(),settings.names)!=null,
                target.entity() instanceof Display || target.type().equals("minecraft:interaction"),nameplateHeads.contains(target.entity().getId()))).toList();
        Map<Integer,Target> merged=new LinkedHashMap<>();
        for(var group:ModelGrouping.group(modelNodes)) {
            Target representative=grouped.get(group.representative().id());
            var parts=group.parts().stream().flatMap(node -> grouped.get(node.id()).parts().stream()).toList();
            merged.put(representative.entity().getId(),new Target(representative.entity(),representative.type(),group.name(),parts));
        }
        grouped.values().stream().filter(target -> target.name().equalsIgnoreCase("Shyworm"))
            .forEach(target -> merged.put(target.entity().getId(),target));
        for(var target:merged.values()) {
            var species=SafariSpecies.named(target.name());
            if(species!=null) for(var part:target.parts()) {
                if(!inferredBodies.contains(part.getId())) {
                    speciesNames.put(part,species.title);
                    speciesHints.observe(typeId(part),species);
                }
            }
        }
        // Anonymous types are temporary grouping candidates, not final default-mode markers.
        // Resolve learned biome-specific models before the run's caught/manual species filter.
        if(!settings.detection.equals("TYPE")) merged.values().removeIf(target -> DetectionRules.matchingName(target.name(),settings.names)==null);
        targets = Map.copyOf(merged);
    }

    private Entity nearestBody(Entity label, List<Entity> nearby, Settings settings) {
        Entity nearest = null;
        double best = Double.MAX_VALUE;
        for (Entity candidate : nearby) {
            if (candidate == label || candidate instanceof ArmorStand || candidate instanceof Display.TextDisplay) continue;
            String candidateLabel=label(candidate);
            if (!DetectionRules.critterLabel(candidateLabel)) continue;
            // A critter-named trade label must never attach to the merchant standing below it.
            if (candidate instanceof Player && DetectionRules.matchingName(candidateLabel,settings.names)==null) {
                var plateSpecies=SafariSpecies.named(DetectionRules.matchingName(label(label),settings.names)==null?"":DetectionRules.matchingName(label(label),settings.names));
                if((plateSpecies!=SafariSpecies.SCRAPPY && plateSpecies!=SafariSpecies.HIDEYHO)
                    || candidate.getCustomName()!=null) continue;
            }
            if (DetectionRules.matchingName(label(candidate), settings.excludedNames) != null) continue;
            double dx = candidate.getX() - label.getX(), dz = candidate.getZ() - label.getZ();
            double dy = label.getY() - candidate.getY();
            // Bind a floating nameplate to the closest entity below it, not to a distant neighbour.
            if (dx * dx + dz * dz > 4 || dy < -0.5 || dy > candidate.getBbHeight() + 3) continue;
            double score = dx * dx + dz * dz + Math.pow(dy - candidate.getBbHeight(), 2) * 0.25;
            if (score < best) { best = score; nearest = candidate; }
        }
        return nearest;
    }

    public static String typeId(Entity entity) { return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(); }
    public static String readable(String type) { return type.substring(type.indexOf(':') + 1).replace('_', ' '); }
    public static String label(Entity entity) {
        if (entity instanceof Display.TextDisplay text && text.textRenderState() != null) {
            return text.textRenderState().text().getString();
        }
        return entity.getCustomName() != null ? entity.getCustomName().getString() : entity.getName().getString();
    }
}
