package org.hypixelskyblockmods.crittersafariesp;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.Items;
import org.hypixelskyblockmods.crittersafariesp.mixin.DisplayAccessor;

public final class RockmiteMounds {
    public static final String TEXTURE="5dbaab74d1acd0abe9d04abe9928725de5d4495fcb63b647228caf6944c20800";
    public static final String NAME="Rockmite Mound";
    public static final int COLOR=0xFFBF40;
    private RockmiteMounds() {}
    public static boolean matches(Display.ItemDisplay display) {
        if(display.itemRenderState()==null || display.getEntityData().get(DisplayAccessor.critterEsp$movementDuration())!=0
            || SafariAreas.at(ModelBounds.of(display,1).getCenter())!=SafariSpecies.Biome.CAVERN) return false;
        var item=display.itemRenderState().itemStack();
        if(!item.is(Items.PLAYER_HEAD)) return false;
        var profile=item.get(DataComponents.PROFILE);
        if(profile==null) return false;
        return profile.partialProfile().properties().get("textures").stream().anyMatch(p -> NpcCritters.texture(p.value(),TEXTURE));
    }
    public static boolean isMound(CritterTracker.Target target) { return target.name().equals(NAME); }
}
