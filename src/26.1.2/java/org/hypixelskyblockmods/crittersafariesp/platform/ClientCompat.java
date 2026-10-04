package org.hypixelskyblockmods.crittersafariesp.platform;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class ClientCompat {
    private ClientCompat() {}
    public static Camera camera(Minecraft mc) { return mc.gameRenderer.getMainCamera(); }
    public static Screen screen(Minecraft mc) { return mc.screen; }
    public static void setScreen(Minecraft mc, Screen screen) { mc.setScreen(screen); }
}
