package org.hypixelskyblockmods.crittersafariesp;

import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import org.hypixelskyblockmods.crittersafariesp.mixin.DisplayAccessor;

/** Narrow published Safari model signatures, rather than generic type matching. */
public final class SpecialModels {
    private SpecialModels() {}
    public static SafariSpecies identify(Entity entity) {
        if(!(entity instanceof Shulker || entity instanceof Bat || entity instanceof Display.ItemDisplay)) return null;
        var biome=SafariAreas.at(ModelBounds.of(entity,1).getCenter());
        if(entity instanceof Shulker shulker) {
            if(biome==SafariSpecies.Biome.FOREST && shulker.getColor()==DyeColor.GREEN) return SafariSpecies.HIDEONFLOOR;
            if(biome==SafariSpecies.Biome.HAUNTED && shulker.getColor()==DyeColor.PURPLE) return SafariSpecies.HIDEONWALL;
        }
        if(entity instanceof Bat && biome==SafariSpecies.Biome.HAUNTED) return SafariSpecies.BLOODBAT;
        if(entity instanceof Display.ItemDisplay display && biome==SafariSpecies.Biome.HAUNTED && display.itemRenderState()!=null) {
            var item=display.itemRenderState().itemStack();
            if(!item.isEmpty() && !item.is(Items.PLAYER_HEAD)
                && !BuiltInRegistries.ITEM.getKey(item.getItem()).toString().equals("minecraft:purple_shulker_box")
                && display.getEntityData().get(DisplayAccessor.critterEsp$movementDuration())==3) return SafariSpecies.DUPLICO;
        }
        return null;
    }
}
