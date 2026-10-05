package org.hypixelskyblockmods.crittersafariesp;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
public final class GameTestCompat {
    public static void waitForChunks(TestSingleplayerContext singleplayer) { singleplayer.getConnection().waitForChunksRender(); }
}
