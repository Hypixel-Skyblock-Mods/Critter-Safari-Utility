package org.hypixelskyblockmods.crittersafariesp;

import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import net.minecraft.world.entity.player.Player;

/** Exact published Hideyho skin identity, rather than broad NPC/player type matching. */
public final class NpcCritters {
    public static final String HIDEYHO_TEXTURE="3504f1f2327a5110e643bb8667082512815fa434a29ed37f4ca83bb16d2db533";
    public static final int INTERACTION_COLOR=0xC184FF;
    private NpcCritters() {}
    public static SafariSpecies identify(Player player) {
        for(var property:player.getGameProfile().properties().get("textures"))
            if(hideyhoTexture(property.value())) return SafariSpecies.HIDEYHO;
        return null;
    }
    public static boolean hideyhoTexture(String encoded) {
        return texture(encoded,HIDEYHO_TEXTURE);
    }
    public static boolean texture(String encoded,String hash) {
        if(encoded==null || encoded.length()>16384) return false;
        try {
            var json=JsonParser.parseString(new String(Base64.getDecoder().decode(encoded),StandardCharsets.UTF_8)).getAsJsonObject();
            String url=json.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
            return url.equals("http://textures.minecraft.net/texture/"+hash)
                || url.equals("https://textures.minecraft.net/texture/"+hash);
        } catch(RuntimeException malformed) { return false; }
    }
    public static boolean interactionOnly(CritterTracker.Target target) {
        return RockmiteMounds.isMound(target) || target.species()==SafariSpecies.HIDEYHO || target.species()==SafariSpecies.SCRAPPY;
    }
}
