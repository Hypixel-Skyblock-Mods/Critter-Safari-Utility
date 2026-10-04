package org.hypixelskyblockmods.crittersafariesp;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class NpcCrittersTest {
    private static String skin(String url) {
        return Base64.getEncoder().encodeToString(("{\"textures\":{\"SKIN\":{\"url\":\""+url+"\"}}}").getBytes(StandardCharsets.UTF_8));
    }
    @Test void onlyExactPublishedSkinIdentifiesAnUnnamedHideyhoNpc() {
        assertTrue(NpcCritters.hideyhoTexture(skin("http://textures.minecraft.net/texture/"+NpcCritters.HIDEYHO_TEXTURE)));
        assertTrue(NpcCritters.hideyhoTexture(skin("https://textures.minecraft.net/texture/"+NpcCritters.HIDEYHO_TEXTURE)));
        assertFalse(NpcCritters.hideyhoTexture(skin("https://example.com/"+NpcCritters.HIDEYHO_TEXTURE)));
        assertFalse(NpcCritters.hideyhoTexture(skin("https://textures.minecraft.net/texture/other")));
        assertFalse(NpcCritters.hideyhoTexture("broken")); assertFalse(NpcCritters.hideyhoTexture(null));
        assertFalse(NpcCritters.hideyhoTexture(Base64.getEncoder().encodeToString("{}".getBytes(StandardCharsets.UTF_8))));
    }
}
