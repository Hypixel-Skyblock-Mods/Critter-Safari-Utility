package org.hypixelskyblockmods.crittersafariesp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class StatusHud {
    public static final int HEIGHT = 16;
    private StatusHud() {}
    public static String text(int count, Settings settings) { return "Critters " + count + "  ·  " + (int)settings.radius + "m"; }
    public static String text(int count,int drops,Settings settings) {
        if(drops==0) return text(count,settings);
        return (count>0?"Critters "+count+"  ·  ":"")+"Drops "+drops+"  ·  "+(int)settings.radius+"m";
    }
    public static int width(String text) { return Minecraft.getInstance().font.width(text) + 10; }
    public static int displayWidth(String text,Settings settings) { return (int)Math.ceil(width(text)*settings.hudScale); }
    public static int displayHeight(Settings settings) { return (int)Math.ceil(HEIGHT*settings.hudScale); }
    public static void draw(GuiGraphicsExtractor graphics, Settings settings, String text) {
        int x = HudPlacement.pixel(settings.hudX, graphics.guiWidth(), displayWidth(text,settings));
        int y = HudPlacement.pixel(settings.hudY, graphics.guiHeight(), displayHeight(settings));
        graphics.pose().pushMatrix(); graphics.pose().translate(x,y).scale(settings.hudScale);
        graphics.fill(0,0,width(text),HEIGHT,0xA6101814);
        graphics.fill(0,0,2,HEIGHT,0xFF000000 | settings.rgb(""));
        graphics.text(Minecraft.getInstance().font,text,6,4,0xFFE3EDE6,false);
        graphics.pose().popMatrix();
    }
}
