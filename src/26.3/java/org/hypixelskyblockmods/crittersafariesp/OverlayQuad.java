package org.hypixelskyblockmods.crittersafariesp;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;

/** Version-specific pipeline type; geometry and batching are shared. */
public record OverlayQuad(Matrix3x2fc pose, float[] vertices, int[] colors,
                          int vertexCount, ScreenRectangle scissorArea, ScreenRectangle bounds) implements GuiElementRenderState {
    @Override public RenderPipeline pipeline() { return RenderPipelines.GUI; }
    @Override public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
    @Override public void buildVertices(VertexConsumer consumer) {
        OverlayBatch.buildVertices(consumer,pose,vertices,colors,vertexCount);
    }
}
