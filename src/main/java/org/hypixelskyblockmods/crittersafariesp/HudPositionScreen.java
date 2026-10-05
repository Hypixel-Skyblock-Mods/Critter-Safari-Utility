package org.hypixelskyblockmods.crittersafariesp;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.hypixelskyblockmods.crittersafariesp.platform.ClientCompat;

/** A placement canvas opened by the standard MoulConfig menu. */
public final class HudPositionScreen extends Screen {
    private final Screen parent;
    private boolean dragging;
    private double grabX, grabY;
    public HudPositionScreen(Screen parent) {
        super(Component.literal("Edit Critter HUD"));
        this.parent = parent;
    }
    private Settings settings() { return CritterSafariClient.settings; }
    private String preview() { return StatusHud.text(3, settings()); }
    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Reset"), button -> {
            settings().hudX = 0; settings().hudY = 0;
            settings().biomeX=Settings.defaultBiomeX(); settings().biomeY=Settings.defaultBiomeY();
            settings().hudScale=1; settings().biomeScale=new float[]{1,1,1,1};
        }).bounds(width / 2 - 104, height - 30, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
            .bounds(width / 2 + 4, height - 30, 100, 20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0x50101814);
        String help = "Drag to move · Scroll over a HUD to resize · Esc to save";
        graphics.text(font, help, (width - font.width(help)) / 2, height - 45, 0xFFE3EDE6, false);
        StatusHud.draw(graphics, settings(), preview());
        BiomePanels.draw(graphics,mouseX,mouseY,true);
        int x = HudPlacement.pixel(settings().hudX, width, StatusHud.displayWidth(preview(),settings()));
        int y = HudPlacement.pixel(settings().hudY, height, StatusHud.displayHeight(settings()));
        int right = x + StatusHud.displayWidth(preview(),settings()), bottom = y + StatusHud.displayHeight(settings());
        int border = 0xFF000000 | settings().hitRgb();
        graphics.fill(x - 1, y - 1, right + 1, y, border);
        graphics.fill(x - 1, bottom, right + 1, bottom + 1, border);
        graphics.fill(x - 1, y, x, bottom, border);
        graphics.fill(right, y, right + 1, bottom, border);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if(super.mouseClicked(event,doubleClick)) return true;
        if(BiomePanels.click(event,width,height,true)) return true;
        int x = HudPlacement.pixel(settings().hudX, width, StatusHud.displayWidth(preview(),settings()));
        int y = HudPlacement.pixel(settings().hudY, height, StatusHud.displayHeight(settings()));
        if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && event.x() >= x && event.x() <= x + StatusHud.displayWidth(preview(),settings())
            && event.y() >= y && event.y() <= y + StatusHud.displayHeight(settings())) {
            grabX = event.x() - x; grabY = event.y() - y; dragging = true;
            return true;
        }
        return false;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if(BiomePanels.drag(event,width,height)) return true;
        if (!dragging || event.button() != com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) return super.mouseDragged(event, dx, dy);
        settings().hudX = HudPlacement.fraction(event.x() - grabX, width, StatusHud.displayWidth(preview(),settings()));
        settings().hudY = HudPlacement.fraction(event.y() - grabY, height, StatusHud.displayHeight(settings()));
        return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if(BiomePanels.release(event)) return true;
        if (dragging && event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) { dragging = false; CritterSafariClient.save(); return true; }
        return super.mouseReleased(event);
    }
    @Override public boolean mouseScrolled(double mouseX,double mouseY,double scrollX,double scrollY) {
        if(BiomePanels.resize(mouseX,mouseY,scrollY,width,height)) return true;
        int x=HudPlacement.pixel(settings().hudX,width,StatusHud.displayWidth(preview(),settings()));
        int y=HudPlacement.pixel(settings().hudY,height,StatusHud.displayHeight(settings()));
        if(new BiomePanels.Rect(x,y,StatusHud.displayWidth(preview(),settings()),StatusHud.displayHeight(settings())).contains(mouseX,mouseY)) {
            settings().hudScale=Math.clamp(settings().hudScale+(float)scrollY*.05f,.5f,2f); return true;
        }
        return super.mouseScrolled(mouseX,mouseY,scrollX,scrollY);
    }
    @Override public void removed() { BiomePanels.finishDrag(); CritterSafariClient.save(); super.removed(); }
    @Override public void onClose() { ClientCompat.setScreen(minecraft, new SettingsScreen(parent)); }
    @Override public boolean isPauseScreen() { return false; }
}
