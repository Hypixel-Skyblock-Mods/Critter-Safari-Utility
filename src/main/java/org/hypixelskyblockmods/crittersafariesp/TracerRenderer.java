package org.hypixelskyblockmods.crittersafariesp;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.hypixelskyblockmods.crittersafariesp.platform.ClientCompat;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ArrayList;

public final class TracerRenderer {
    private static final int[][] EDGES = {
        {0,1},{0,2},{0,4},{1,3},{1,5},{2,3},{2,6},{3,7},{4,5},{4,6},{5,7},{6,7}
    };
    private static final int[][] FACES = {
        {0,4,6,2}, {1,3,7,5}, {0,1,5,4}, {2,6,7,3}, {0,2,3,1}, {4,5,7,6}
    };
    private record Smoothed(Entity entity, Vec3 position) {}
    private record Label(String text,int x,int y,int rgb) {}
    private static final Map<Integer, Smoothed> smoothed = new HashMap<>();
    private static ClientLevel world;
    private static long lastFrame;
    static java.util.List<String> lastLabelNames=java.util.List.of();
    private TracerRenderer() {}
    public static boolean screenAllowsEsp(Screen screen) { return screen==null || screen instanceof ChatScreen; }

    public static void extract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        lastLabelNames=java.util.List.of();
        Minecraft mc = Minecraft.getInstance();
        Settings settings = CritterSafariClient.settings;
        if (world != mc.level) { world = mc.level; smoothed.clear(); lastFrame = 0; }
        if (settings == null || mc.level == null || mc.player == null || !screenAllowsEsp(ClientCompat.screen(mc))) return;
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        int count = CritterSafariClient.tracker.targets().size();
        var drops=FloorDrops.nearby(mc,settings);
        if (settings.hud && settings.enabled && (count > 0 || !drops.isEmpty()))
            StatusHud.draw(graphics, settings, StatusHud.text(count,drops.size(), settings));
        if (!settings.enabled) { smoothed.clear(); return; }
        OverlayBatch.begin(graphics);
        var labels=new ArrayList<Label>();
        long now = System.nanoTime();
        double elapsed = lastFrame == 0 ? 16 : Math.min(100, (now - lastFrame) / 1_000_000.0);
        lastFrame = now;
        double smoothing = MotionSmoothing.factor(elapsed, settings.smoothingMillis);
        float pixel = width / (float)Math.max(1, mc.getWindow().getWidth());
        var camera = ClientCompat.camera(mc);
        Matrix4f matrix = camera.getViewRotationProjectionMatrix(new Matrix4f());
        Vec3 cameraPos = camera.position();
        float partialTick = delta.getGameTimeDeltaPartialTick(true);
        var prediction=CapsulePreview.preview(mc,settings,partialTick);
        var preview=prediction.path();
        var lead=prediction.aim();
        if (lead!=null) {
            var projected=clip(lead.point(),cameraPos,matrix);
            var point=Projection.toScreen(projected.x,projected.y,projected.w,width,height,false);
            if (point!=null && point.onScreen()) {
                int color=0xDF000000|settings.hitRgb(); float radius=8*pixel;
                for(int i=0;i<32;i++) {
                    double a=i*Math.PI/16,b=(i+1)*Math.PI/16;
                    stroke(graphics,point.x()+(float)Math.cos(a)*radius,point.y()+(float)Math.sin(a)*radius,
                        point.x()+(float)Math.cos(b)*radius,point.y()+(float)Math.sin(b)*radius,1.25f,color,settings,pixel);
                }
            }
        }
        for(var pos:drops) drawBox(graphics,new AABB(pos).inflate(.02),cameraPos,matrix,width,height,settings,
            ((int)(settings.opacity*255)<<24)|0xFFBF40,pixel);
        Integer hitId = preview == null ? null : preview.hitEntity();
        boolean critterHit = hitId != null && CritterSafariClient.tracker.targets().stream().anyMatch(t -> t.entity().getId() == hitId && !NpcCritters.interactionOnly(t));
        if (preview != null && settings.showTrajectory) {
            int pathColor = ((int)(settings.opacity * 255) << 24) | (critterHit ? settings.hitRgb() : settings.rgb(""));
            for (int i = 1; i < preview.points().size(); i++) {
                Vector4f a = clip(preview.points().get(i - 1), cameraPos, matrix);
                Vector4f b = clip(preview.points().get(i), cameraPos, matrix);
                if (a.w > 0.05f && b.w > 0.05f) stroke(graphics,
                    (a.x / a.w + 1) * width / 2, (1 - a.y / a.w) * height / 2,
                    (b.x / b.w + 1) * width / 2, (1 - b.y / b.w) * height / 2,
                    settings.lineWidth, pathColor, settings, pixel);
            }
            if (preview.impact()) {
                Vector4f end = clip(preview.points().getLast(), cameraPos, matrix);
                var point = Projection.toScreen(end.x, end.y, end.w, width, height, false);
                if (point != null && point.onScreen()) {
                    float x = point.x(), y = point.y(), size = 4 * pixel;
                    stroke(graphics,x-size,y,x,y-size,settings.lineWidth,pathColor,settings,pixel);
                    stroke(graphics,x,y-size,x+size,y,settings.lineWidth,pathColor,settings,pixel);
                    stroke(graphics,x+size,y,x,y+size,settings.lineWidth,pathColor,settings,pixel);
                    stroke(graphics,x,y+size,x-size,y,settings.lineWidth,pathColor,settings,pixel);
                }
            }
        }
        float originX = width / 2f, originY = settings.origin.equals("BOTTOM") ? height - 18 * pixel : height / 2f;
        var activeIds = new HashSet<Integer>();
        for (var target : CritterSafariClient.tracker.targets()) {
            Entity entity = target.entity();
            if (entity.isRemoved() || mc.level.getEntity(entity.getId()) != entity
                || !DetectionRules.inRange(ModelBounds.distanceSquared(entity,mc.player), settings.radius)) continue;
            activeIds.add(entity.getId());
            AABB actualBox=target.bounds(partialTick);
            Vec3 actual = actualBox.getCenter();
            Smoothed previous = smoothed.get(entity.getId());
            Vec3 interpolated = previous == null || previous.entity() != entity || previous.position().distanceToSqr(actual) > 64
                ? actual : previous.position().lerp(actual, smoothing);
            smoothed.put(entity.getId(), new Smoothed(entity, interpolated));
            Vec3 center = interpolated;
            Vector4f clip = clip(center, cameraPos, matrix);
            var point = Projection.toScreen(clip.x, clip.y, clip.w, width, height, settings.offscreenIndicators);
            if (point == null) continue;
            int rgb = RockmiteMounds.isMound(target)?RockmiteMounds.COLOR:
                NpcCritters.interactionOnly(target)?NpcCritters.INTERACTION_COLOR:
                hitId != null && hitId == entity.getId() ? settings.hitRgb() : settings.rgb(target.type());
            int color = ((int)(settings.opacity * 255) << 24) | rgb;
            if (point.onScreen() && (settings.boxes || settings.filledBoxes)) {
                AABB box = actualBox.move(interpolated.subtract(actual)).inflate(0.06);
                if (box.getSize() < 0.2) box = box.inflate(0.2);
                drawBox(graphics,box,cameraPos,matrix,width,height,settings,color,pixel);
            }
            if (settings.tracers) stroke(graphics, originX, originY, point.x(), point.y(), settings.lineWidth, color, settings, pixel);
            if (!point.onScreen()) chevron(graphics, point.x(), point.y(), width, height, color, settings, pixel);
            if (settings.aimNames && point.onScreen()
                && TargetVisibility.showName(cameraPos,mc.player.getViewVector(partialTick),actual)) {
                String text=target.name();
                float top=point.y();
                AABB labelBox=actualBox.move(interpolated.subtract(actual)).inflate(.06);
                for(int i=0;i<8;i++) {
                    var corner=clip(new Vec3((i&1)==0?labelBox.minX:labelBox.maxX,(i&2)==0?labelBox.minY:labelBox.maxY,
                        (i&4)==0?labelBox.minZ:labelBox.maxZ),cameraPos,matrix);
                    if(corner.w>.05f) top=Math.min(top,(1-corner.y/corner.w)*height/2);
                }
                int tx=Math.clamp((int)(point.x()-mc.font.width(text)/2f),4,Math.max(4,width-mc.font.width(text)-6));
                int ty=Math.clamp((int)(top-13),4,height-14);
                labels.add(new Label(text,tx,ty,rgb));
            }
        }
        smoothed.keySet().retainAll(activeIds);
        OverlayBatch.finish();
        lastLabelNames=labels.stream().map(Label::text).toList();
        for(var label:labels) {
            graphics.fill(label.x()-3,label.y()-2,label.x()+mc.font.width(label.text())+3,label.y()+10,0xAA101814);
            graphics.text(mc.font,label.text(),label.x(),label.y(),0xFF000000|label.rgb(),false);
        }
    }

    private static void drawBox(GuiGraphicsExtractor graphics,AABB box,Vec3 camera,Matrix4f matrix,int width,int height,Settings settings,int color,float pixel) {
        var center=clip(box.getCenter(),camera,matrix);
        var point=Projection.toScreen(center.x,center.y,center.w,width,height,false);
        if (point==null || !point.onScreen()) return;
        float[][] projected=new float[8][];
        for(int i=0;i<8;i++) {
            var corner=clip(new Vec3((i&1)==0?box.minX:box.maxX,(i&2)==0?box.minY:box.maxY,(i&4)==0?box.minZ:box.maxZ),camera,matrix);
            if(corner.w>.05f) projected[i]=new float[]{(corner.x/corner.w+1)*width/2,(1-corner.y/corner.w)*height/2};
        }
        if(settings.filledBoxes && settings.fillOpacity>0) filledFaces(graphics,box,camera,projected,((int)(settings.fillOpacity*255)<<24)|(color&0xFFFFFF));
        if(settings.boxes) outline(graphics,projected,settings,color,pixel);
    }

    private static void filledFaces(GuiGraphicsExtractor graphics, AABB box, Vec3 camera, float[][] corners, int color) {
        // Only the front-facing sides are filled: back faces would stack opacity and obscure the model.
        boolean[] visible = {camera.x < box.minX, camera.x > box.maxX, camera.y < box.minY,
            camera.y > box.maxY, camera.z < box.minZ, camera.z > box.maxZ};
        for (int i = 0; i < FACES.length; i++) {
            if (!visible[i]) continue;
            float[] quad = new float[8];
            boolean valid = true;
            for (int v = 0; v < 4; v++) {
                float[] p = corners[FACES[i][v]];
                if (p == null) { valid = false; break; }
                quad[v * 2] = p[0]; quad[v * 2 + 1] = p[1];
            }
            if (valid) OverlayBatch.submit(graphics, quad, color);
        }
    }
    private static void outline(GuiGraphicsExtractor graphics, float[][] points, Settings settings, int color, float pixel) {
        if (settings.boxStyle.equals("BOX_3D")) {
            for (int[] edge : EDGES) {
                float[] a = points[edge[0]], b = points[edge[1]];
                if (a != null && b != null) stroke(graphics, a[0], a[1], b[0], b[1], settings.lineWidth, color, settings, pixel);
            }
            return;
        }
        float x0 = Float.MAX_VALUE, y0 = Float.MAX_VALUE, x1 = -Float.MAX_VALUE, y1 = -Float.MAX_VALUE;
        for (float[] p : points) {
            if (p == null) return;
            x0 = Math.min(x0, p[0]); x1 = Math.max(x1, p[0]); y0 = Math.min(y0, p[1]); y1 = Math.max(y1, p[1]);
        }
        if (settings.boxStyle.equals("BOX")) {
            stroke(graphics, x0,y0,x1,y0,settings.lineWidth,color,settings,pixel);
            stroke(graphics, x1,y0,x1,y1,settings.lineWidth,color,settings,pixel);
            stroke(graphics, x1,y1,x0,y1,settings.lineWidth,color,settings,pixel);
            stroke(graphics, x0,y1,x0,y0,settings.lineWidth,color,settings,pixel);
        } else {
            float arm = Math.min(12 * pixel, Math.min(x1 - x0, y1 - y0) * 0.3f);
            for (float x : new float[]{x0, x1}) for (float y : new float[]{y0, y1}) {
                stroke(graphics, x,y,x + (x == x0 ? arm : -arm),y,settings.lineWidth,color,settings,pixel);
                stroke(graphics, x,y,x,y + (y == y0 ? arm : -arm),settings.lineWidth,color,settings,pixel);
            }
        }
    }
    private static void chevron(GuiGraphicsExtractor graphics, float x, float y, int width, int height, int color, Settings settings, float pixel) {
        float dx = x - width / 2f, dy = y - height / 2f, length = (float)Math.hypot(dx,dy);
        if (length < 0.01f) return;
        dx /= length; dy /= length;
        float size = 6 * pixel;
        stroke(graphics,x,y,x - dx * size - dy * size * 0.65f,y - dy * size + dx * size * 0.65f,1.25f,color,settings,pixel);
        stroke(graphics,x,y,x - dx * size + dy * size * 0.65f,y - dy * size - dx * size * 0.65f,1.25f,color,settings,pixel);
    }
    private static Vector4f clip(Vec3 world, Vec3 camera, Matrix4f matrix) {
        Vec3 relative = world.subtract(camera);
        return matrix.transform(new Vector4f((float)relative.x, (float)relative.y, (float)relative.z, 1));
    }
    private static void stroke(GuiGraphicsExtractor graphics, float ax, float ay, float bx, float by,
                               float widthPixels, int color, Settings settings, float pixel) {
        int alpha = color >>> 24, rgb = color & 0xFFFFFF;
        if (settings.halo && settings.haloStrength > 0) {
            for (int layer = 5; layer >= 1; layer--) {
                float featherAlpha = (float)(Math.exp(-layer * layer / 8.0) * 0.11 * settings.haloStrength);
                line(graphics, ax,ay,bx,by,(widthPixels + layer * 1.2f) * pixel, ((int)(alpha * featherAlpha) << 24) | rgb, pixel);
            }
        }
        line(graphics,ax,ay,bx,by,widthPixels * pixel,color,pixel);
    }
    private static void line(GuiGraphicsExtractor graphics, float ax, float ay, float bx, float by, float width, int color, float pixel) {
        float dx = bx - ax, dy = by - ay, length = (float)Math.hypot(dx, dy);
        if (!Float.isFinite(length) || length < 0.01f || length > 100_000 || (color >>> 24) == 0) return;
        float nx = -dy / length, ny = dx / length;
        float inner = Math.max(0, (width - pixel) / 2), outer = inner + pixel;
        // Interpolated vertex alpha approximates pixel coverage along each edge, independent of angle.
        int core = ((int)((color >>> 24) * Math.min(1, width / pixel)) << 24) | (color & 0xFFFFFF);
        int transparent = color & 0xFFFFFF;
        if (inner > 0) strip(graphics, ax,ay,bx,by,nx,ny,-inner,inner,core,core);
        strip(graphics,ax,ay,bx,by,nx,ny,inner,outer,core,transparent);
        strip(graphics,ax,ay,bx,by,nx,ny,-outer,-inner,transparent,core);
    }
    private static void strip(GuiGraphicsExtractor graphics, float ax, float ay, float bx, float by,
                              float nx, float ny, float from, float to, int fromColor, int toColor) {
        OverlayBatch.submit(graphics, new float[]{
            ax + nx * from, ay + ny * from, ax + nx * to, ay + ny * to,
            bx + nx * to, by + ny * to, bx + nx * from, by + ny * from
        }, new int[]{fromColor, toColor, toColor, fromColor});
    }
}
