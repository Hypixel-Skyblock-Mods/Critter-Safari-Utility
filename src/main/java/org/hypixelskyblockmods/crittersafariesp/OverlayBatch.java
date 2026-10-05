package org.hypixelskyblockmods.crittersafariesp;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;

import org.hypixelskyblockmods.crittersafariesp.mixin.GuiGraphicsAccessor;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import java.util.Arrays;

/** A perspective-projected box face submitted to Minecraft's backend-neutral GUI pipeline. */
public final class OverlayBatch {
    private OverlayBatch() {}
    private static Batch batch;
    static int lastQuadCount, lastSubmissionCount;
    public static void buildVertices(VertexConsumer consumer, Matrix3x2fc pose, float[] vertices, int[] colors, int count) {
        for (int i=0; i<count; i++) consumer.addVertexWith2DPose(pose,vertices[i*2],vertices[i*2+1]).setColor(colors[i]);
    }
    public static void begin(GuiGraphicsExtractor graphics) {
        batch=new Batch(graphics); lastQuadCount=0; lastSubmissionCount=0;
    }
    public static void finish() {
        Batch completed=batch; batch=null;
        if(completed!=null) completed.submit();
    }
    public static void submit(GuiGraphicsExtractor graphics, float[] points, int color) {
        submit(graphics, points, new int[]{color, color, color, color});
    }
    public static void submit(GuiGraphicsExtractor graphics, float[] points, int[] colors) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            if (!Float.isFinite(points[i * 2]) || !Float.isFinite(points[i * 2 + 1])) return;
            minX = Math.min(minX, points[i * 2]); maxX = Math.max(maxX, points[i * 2]);
            minY = Math.min(minY, points[i * 2 + 1]); maxY = Math.max(maxY, points[i * 2 + 1]);
        }
        if (maxX - minX > 100_000 || maxY - minY > 100_000) return;
        if(batch!=null && batch.graphics==graphics) { batch.add(points,colors,minX,minY,maxX,maxY); return; }
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle bounds = new ScreenRectangle((int)Math.floor(minX), (int)Math.floor(minY),
            Math.max(1, (int)Math.ceil(maxX) - (int)Math.floor(minX)),
            Math.max(1, (int)Math.ceil(maxY) - (int)Math.floor(minY))).transformMaxBounds(pose);
        ((GuiGraphicsAccessor) graphics).critterEsp$renderState().addGuiElement(new OverlayQuad(pose, points, colors,4,
            new ScreenRectangle(0, 0, graphics.guiWidth(), graphics.guiHeight()), bounds));
    }
    private static final class Batch {
        final GuiGraphicsExtractor graphics;
        final Matrix3x2f pose;
        float[] points=new float[2048]; int[] colors=new int[1024]; int count;
        float x0=Float.MAX_VALUE,y0=x0,x1=-Float.MAX_VALUE,y1=x1;
        Batch(GuiGraphicsExtractor graphics) { this.graphics=graphics; pose=new Matrix3x2f(graphics.pose()); }
        void add(float[] quad,int[] tint,float minX,float minY,float maxX,float maxY) {
            if((count+4)>colors.length) { colors=Arrays.copyOf(colors,colors.length*2); points=Arrays.copyOf(points,points.length*2); }
            System.arraycopy(quad,0,points,count*2,8); System.arraycopy(tint,0,colors,count,4); count+=4;
            x0=Math.min(x0,minX); y0=Math.min(y0,minY); x1=Math.max(x1,maxX); y1=Math.max(y1,maxY);
        }
        void submit() {
            lastQuadCount=count/4;
            if(count==0) return;
            ScreenRectangle bounds=new ScreenRectangle((int)Math.floor(x0),(int)Math.floor(y0),
                Math.max(1,(int)Math.ceil(x1)-(int)Math.floor(x0)),Math.max(1,(int)Math.ceil(y1)-(int)Math.floor(y0))).transformMaxBounds(pose);
            ((GuiGraphicsAccessor)graphics).critterEsp$renderState().addGuiElement(new OverlayQuad(pose,points,colors,count,
                new ScreenRectangle(0,0,graphics.guiWidth(),graphics.guiHeight()),bounds));
            lastSubmissionCount=1;
        }
    }
}
