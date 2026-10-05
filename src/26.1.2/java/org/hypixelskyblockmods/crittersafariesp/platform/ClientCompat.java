package org.hypixelskyblockmods.crittersafariesp.platform;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class ClientCompat {
    private ClientCompat() {}
    public static Camera camera(Minecraft mc) { return mc.gameRenderer.getMainCamera(); }
    public static Screen screen(Minecraft mc) { return mc.screen; }
    public static void setScreen(Minecraft mc, Screen screen) { mc.setScreen(screen); }
    public static com.mojang.blaze3d.platform.InputConstants.Type keyboardType() { return com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM; }
    public static net.minecraft.core.particles.ParticleOptions particle(net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket packet) { return packet.getParticle(); }
    public static net.minecraft.world.phys.Vec3 particlePosition(net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket packet) { return new net.minecraft.world.phys.Vec3(packet.getX(), packet.getY(), packet.getZ()); }
    public static com.mojang.blaze3d.platform.InputConstants.Key configKey(int code) {
        var unknown = com.mojang.blaze3d.platform.InputConstants.UNKNOWN;
        if (code == -1) return unknown;
        return (code >= 0 && code <= 9 ? com.mojang.blaze3d.platform.InputConstants.Type.MOUSE : keyboardType()).getOrCreate(code);
    }
    public static int configCode(com.mojang.blaze3d.platform.InputConstants.Key key) {
        if (key.equals(com.mojang.blaze3d.platform.InputConstants.UNKNOWN)) return -1;
        return key.getValue();
    }
}
