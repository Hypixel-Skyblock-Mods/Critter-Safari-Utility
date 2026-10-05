package org.hypixelskyblockmods.crittersafariesp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import org.hypixelskyblockmods.crittersafariesp.platform.ClientCompat;
import java.util.List;

/** Independent three-column panels, with input only when a screen exposes the cursor. */
public final class BiomePanels {
    public static final int CELL=92,COLUMNS=3,WIDTH=12+CELL*COLUMNS,HEADER=16,ROW=12,FOOTER=16;
    public record Rect(int x,int y,int width,int height) {
        public boolean contains(double px,double py) { return px>=x && px<x+width && py>=y && py<y+height; }
    }
    private static SafariSpecies.Biome dragging;
    private static double grabX,grabY;
    private BiomePanels() {}
    public static boolean interactive(Screen screen) {
        return screen!=null && !(screen instanceof SettingsScreen) && !(screen instanceof HudPositionScreen);
    }
    public static boolean visible() {
        var mc=Minecraft.getInstance(); var settings=CritterSafariClient.settings;
        return settings!=null && settings.enabled && settings.biomePanels && CritterTracker.inArea(mc,settings);
    }
    public static List<SafariSpecies> rows(SafariSpecies.Biome biome,SafariRun run,boolean editing) {
        return editing?biome.species():biome.species().stream().filter(run::enabled).toList();
    }
    private static int gridRows(int count) { return (count+COLUMNS-1)/COLUMNS; }
    public static int height(SafariSpecies.Biome biome) { return HEADER+gridRows(biome.species().size())*ROW+FOOTER; }
    public static float scale(Settings settings,SafariSpecies.Biome biome,int width,int height) {
        float base=Math.min(1,(width-18f)/(2*WIDTH+6));
        return Math.max(.15f,Math.min(base*settings.biomeScale[biome.ordinal()],Math.min((width-12f)/WIDTH,(height-12f)/height(biome))));
    }
    public static Rect rect(Settings settings,SafariSpecies.Biome biome,int width,int height) {
        float scale=scale(settings,biome,width,height);
        int w=(int)Math.ceil(WIDTH*scale),h=(int)Math.ceil(height(biome)*scale);
        return new Rect(HudPlacement.pixel(settings.biomeX[biome.ordinal()],width,w),
            HudPlacement.pixel(settings.biomeY[biome.ordinal()],height,h),w,h);
    }
    public static void draw(GuiGraphicsExtractor graphics,int mouseX,int mouseY,boolean edit) {
        var mc=Minecraft.getInstance(); var settings=CritterSafariClient.settings; var run=CritterSafariClient.run;
        for(var biome:SafariSpecies.Biome.values()) {
            var species=rows(biome,run,edit);
            if(!edit && species.isEmpty()) continue;
            var box=rect(settings,biome,graphics.guiWidth(),graphics.guiHeight());
            float scale=scale(settings,biome,graphics.guiWidth(),graphics.guiHeight());
            double mx=(mouseX-box.x())/scale,my=(mouseY-box.y())/scale;
            int color=0xFF000000|biome.color,bodyHeight=HEADER+gridRows(species.size())*ROW+(edit?FOOTER:3);
            graphics.pose().pushMatrix(); graphics.pose().translate(box.x(),box.y()).scale(scale);
            graphics.fill(0,0,WIDTH,bodyHeight,0xB510141B);
            graphics.fill(0,0,2,bodyHeight,color);
            graphics.fill(2,0,WIDTH,HEADER,0xC8212731);
            int caught=(int)biome.species().stream().filter(run::caught).count();
            graphics.text(mc.font,biome.title+"  "+caught+"/"+biome.species().size(),7,4,color,false);
            for(int i=0;i<species.size();i++) {
                var item=species.get(i); int x=7+(i%COLUMNS)*CELL,y=HEADER+(i/COLUMNS)*ROW;
                if(edit && new Rect(x-2,y,CELL-2,ROW).contains(mx,my)) graphics.fill(x-2,y,x+CELL-4,y+ROW,0x334C5967);
                graphics.text(mc.font,item.title,x,y+2,item.rarity.color(run.enabled(item)),false);
            }
            if(edit) {
                int footer=bodyHeight-FOOTER;
                graphics.fill(2,footer,WIDTH,footer+1,0x554C5967);
                graphics.text(mc.font,"Enable all",7,footer+4,0xFFCDD7E2,false);
                graphics.text(mc.font,"Disable all",WIDTH-mc.font.width("Disable all")-7,footer+4,0xFFCDD7E2,false);
            }
            graphics.pose().popMatrix();
        }
    }
    public static boolean click(MouseButtonEvent event,int width,int height,boolean preview) {
        if(event.button()!=com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT || (!preview && (!visible() || !interactive(ClientCompat.screen(Minecraft.getInstance()))))) return false;
        var settings=CritterSafariClient.settings; var biomes=SafariSpecies.Biome.values();
        for(int i=biomes.length-1;i>=0;i--) {
            var biome=biomes[i]; var box=rect(settings,biome,width,height);
            if(!box.contains(event.x(),event.y())) continue;
            float scale=scale(settings,biome,width,height);
            double x=(event.x()-box.x())/scale,y=(event.y()-box.y())/scale;
            if(y<HEADER) { dragging=biome; grabX=event.x()-box.x(); grabY=event.y()-box.y(); }
            else if(!preview) {
                if(y>=height(biome)-FOOTER) CritterSafariClient.run.setAll(biome,x<WIDTH/2f);
                else {
                    int column=(int)((x-5)/CELL),row=(int)((y-HEADER)/ROW),index=row*COLUMNS+column;
                    if(x<5 || column>=COLUMNS || index>=biome.species().size()) return true;
                    CritterSafariClient.run.toggle(biome.species().get(index));
                }
                CritterSafariClient.tracker.refresh();
            }
            return true;
        }
        return false;
    }
    public static boolean drag(MouseButtonEvent event,int width,int height) {
        if(dragging==null || event.button()!=com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) return false;
        var settings=CritterSafariClient.settings; int index=dragging.ordinal(); var box=rect(settings,dragging,width,height);
        settings.biomeX[index]=HudPlacement.fraction(event.x()-grabX,width,box.width());
        settings.biomeY[index]=HudPlacement.fraction(event.y()-grabY,height,box.height());
        return true;
    }
    public static boolean resize(double x,double y,double scroll,int width,int height) {
        if(scroll==0) return false;
        var settings=CritterSafariClient.settings; var biomes=SafariSpecies.Biome.values();
        for(int i=biomes.length-1;i>=0;i--) if(rect(settings,biomes[i],width,height).contains(x,y)) {
            settings.biomeScale[i]=Math.clamp(settings.biomeScale[i]+(float)scroll*.05f,.5f,2f);
            return true;
        }
        return false;
    }
    public static boolean release(MouseButtonEvent event) {
        if(dragging==null || event.button()!=com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) return false;
        finishDrag(); return true;
    }
    public static void finishDrag() { if(dragging!=null) { dragging=null; CritterSafariClient.save(); } }
}
