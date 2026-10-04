package org.hypixelskyblockmods.crittersafariesp;

import io.github.notenoughupdates.moulconfig.ChromaColour;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Settings {
    public boolean enabled = true;
    public boolean safariOnly = true;
    public boolean tracers = true;
    public boolean boxes = true;
    public boolean halo = true;
    public boolean aimNames = true;
    public boolean offscreenIndicators = false;
    public boolean floorDrops = true;
    public boolean hud = true;
    public boolean oneCritterMode = false;
    public boolean biomePanels = true;
    public float[] biomeX = defaultBiomeX();
    public float[] biomeY = defaultBiomeY();
    public float[] biomeScale = new float[]{1,1,1,1};
    public float hudScale = 1;
    public static float[] defaultBiomeX() { return new float[]{0,1,0,1}; }
    public static float[] defaultBiomeY() { return new float[]{.14f,.14f,.58f,.58f}; }
    public float hudX = 0;
    public float hudY = 0;
    public double radius = 100;
    public float lineWidth = 1.25f;
    public float haloStrength = 0.55f;
    public float smoothingMillis = 70;
    public float fillOpacity = 0.10f;
    public boolean filledBoxes = true;
    public String boxStyle = "BOX_3D";
    public float opacity = 0.9f;
    public int scanIntervalTicks = 5;
    public String color = "#FF6464";
    public String chromaColor;
    public String hitColor = "#64FF64";
    public String chromaHitColor;
    public boolean trajectory = true;
    public boolean showTrajectory = true;
    public boolean predictMotion = true;
    public float ballSpeed = 1.5f;
    public float ballGravity = 0.063f;
    public float hitTolerance = 0.5f;
    public boolean aimCircle = true;
    public float ballDrag = 1f;
    public float ballRadius = 0.125f;
    public int predictionTicks = 80;
    private transient String parsedColor;
    private transient ChromaColour chroma;
    private transient String parsedHitColor;
    private transient ChromaColour hitChroma;
    public String origin = "CROSSHAIR";
    public String detection = "BOTH";
    public int detectionRevision = 2;
    public List<String> entityTypes = new ArrayList<>(Catalog.DEFAULTS.entityTypes());
    public List<String> names = new ArrayList<>(Catalog.DEFAULTS.names());
    public List<String> excludedNames = new ArrayList<>();
    public Map<String, String> typeColors = new LinkedHashMap<>();

    public void normalize() {
        radius = Math.round(finiteClamp(radius, 1, 100, 100));
        hudX = (float) finiteClamp(hudX, 0, 1, 0);
        hudY = (float) finiteClamp(hudY, 0, 1, 0);
        if(biomeX==null || biomeX.length!=4) biomeX=defaultBiomeX();
        if(biomeY==null || biomeY.length!=4) biomeY=defaultBiomeY();
        if(biomeScale==null || biomeScale.length!=4) biomeScale=new float[]{1,1,1,1};
        hudScale=(float)finiteClamp(hudScale,.5,2,1);
        for(int i=0;i<4;i++) {
            biomeX[i]=(float)finiteClamp(biomeX[i],0,1,defaultBiomeX()[i]);
            biomeY[i]=(float)finiteClamp(biomeY[i],0,1,.12);
            biomeScale[i]=(float)finiteClamp(biomeScale[i],.5,2,1);
        }
        lineWidth = (float) finiteClamp(lineWidth, 0.5, 5, 1.25);
        haloStrength = (float) finiteClamp(haloStrength, 0, 1, 0.55);
        smoothingMillis = (float) finiteClamp(smoothingMillis, 0, 200, 70);
        fillOpacity = (float) finiteClamp(fillOpacity, 0, 0.6, 0.10);
        if (boxStyle == null || !List.of("BOX_3D", "CORNERS", "BOX").contains(boxStyle)) boxStyle = "BOX_3D";
        opacity = (float) finiteClamp(opacity, 0.1, 1, 0.9);
        scanIntervalTicks = Math.clamp(scanIntervalTicks, 1, 40);
        if (!validColor(color)) color = "#FF6464";
        if (!validColor(hitColor)) hitColor = "#64FF64";
        ballSpeed = (float) finiteClamp(ballSpeed, 0.1, 5, 1.5);
        ballGravity = (float) finiteClamp(ballGravity, 0, 0.2, 0.063);
        hitTolerance = (float) finiteClamp(hitTolerance, 0, 2, 0.5);
        ballDrag = (float) finiteClamp(ballDrag, 0.8, 1, 1);
        ballRadius = (float) finiteClamp(ballRadius, 0, 0.5, 0.125);
        predictionTicks = Math.clamp(predictionTicks, 10, 160);
        if (chromaColor != null) {
            try { ChromaColour.forLegacyString(chromaColor); }
            catch (RuntimeException e) { chromaColor = null; }
        }
        if (chromaHitColor != null) {
            try { ChromaColour.forLegacyString(chromaHitColor); }
            catch (RuntimeException e) { chromaHitColor = null; }
        }
        if (origin == null || !List.of("CROSSHAIR", "BOTTOM").contains(origin)) origin = "CROSSHAIR";
        if (detection == null || !List.of("BOTH", "TYPE", "NAME").contains(detection)) detection = "NAME";
        if (entityTypes == null) entityTypes = new ArrayList<>(Catalog.DEFAULTS.entityTypes());
        if (names == null) names = new ArrayList<>(Catalog.DEFAULTS.names());
        if (excludedNames == null) excludedNames = new ArrayList<>();
        if (typeColors == null) typeColors = new LinkedHashMap<>();
        entityTypes.removeIf(s -> s == null || s.isBlank());
        names.removeIf(s -> s == null || s.isBlank());
        excludedNames.removeIf(s -> s == null || s.isBlank());
        typeColors.entrySet().removeIf(e -> e.getKey() == null || !validColor(e.getValue()));
    }
    private static double finiteClamp(double value, double low, double high, double fallback) {
        return Double.isFinite(value) ? Math.clamp(value, low, high) : fallback;
    }
    public static boolean validColor(String color) { return color != null && color.matches("#[0-9a-fA-F]{6}"); }
    public int rgb(String type) {
        if (typeColors.containsKey(type)) return Integer.parseInt(typeColors.get(type).substring(1), 16);
        if (chromaColor != null) {
            if (!chromaColor.equals(parsedColor)) { parsedColor = chromaColor; chroma = ChromaColour.forLegacyString(chromaColor); }
            return chroma.getEffectiveColourRGB() & 0xFFFFFF;
        }
        return Integer.parseInt(color.substring(1), 16);
    }
    public int argb(String type) { return ((int) (opacity * 255) << 24) | rgb(type); }
    public int hitRgb() {
        if (chromaHitColor != null) {
            if (!chromaHitColor.equals(parsedHitColor)) { parsedHitColor = chromaHitColor; hitChroma = ChromaColour.forLegacyString(chromaHitColor); }
            return hitChroma.getEffectiveColourRGB() & 0xFFFFFF;
        }
        return Integer.parseInt(hitColor.substring(1), 16);
    }
}
