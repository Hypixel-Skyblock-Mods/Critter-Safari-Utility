package org.hypixelskyblockmods.crittersafariesp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import net.minecraft.world.phys.AABB;

/** A floor-drop spot is confirmed by server happy-villager particles and exactly three string displays. */
public final class FloorDrops {
    private static ClientLevel world;
    private static final Map<BlockPos,List<Display.ItemDisplay>> confirmed = new HashMap<>();
    private FloorDrops() {}
    public static void observe(ClientboundLevelParticlesPacket packet) {
        Minecraft mc=Minecraft.getInstance();
        if (world!=mc.level) { confirmed.clear(); world=mc.level; }
        if (world==null || !CritterSafariClient.settings.floorDrops || !CritterSafariClient.tracker.allowed()
            || org.hypixelskyblockmods.crittersafariesp.platform.ClientCompat.particle(packet).getType()!=ParticleTypes.HAPPY_VILLAGER) return;
        BlockPos position=BlockPos.containing(org.hypixelskyblockmods.crittersafariesp.platform.ClientCompat.particlePosition(packet).add(0,-1,0));
        var displays=world.getEntitiesOfClass(Display.ItemDisplay.class,new AABB(position).inflate(.01),
            display -> valid(display,position));
        if (displays.size()==3) confirmed.put(position,List.copyOf(displays));
    }
    private static boolean valid(Display.ItemDisplay display,BlockPos position) {
        return !display.isRemoved() && BlockPos.containing(display.position()).equals(position)
            && display.itemRenderState()!=null && display.itemRenderState().itemStack().is(Items.STRING);
    }
    public static List<BlockPos> nearby(Minecraft mc, Settings settings) {
        if (world!=mc.level || mc.level==null || mc.player==null || !settings.floorDrops || !CritterSafariClient.tracker.allowed()) {
            confirmed.clear(); world=mc.level; return List.of();
        }
        confirmed.entrySet().removeIf(entry -> !world.hasChunkAt(entry.getKey())
            || entry.getValue().stream().anyMatch(display -> !valid(display,entry.getKey()) || world.getEntity(display.getId())!=display));
        return confirmed.keySet().stream().filter(pos -> DetectionRules.inRange(Vec3.atCenterOf(pos).distanceToSqr(mc.player.position()),settings.radius)).toList();
    }
}
