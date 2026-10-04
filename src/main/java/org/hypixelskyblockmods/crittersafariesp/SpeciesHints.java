package org.hypixelskyblockmods.crittersafariesp;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Session model observations, never guessed catalog assignments. */
public final class SpeciesHints {
    private record Key(String type,SafariSpecies.Biome biome) {}
    // Generic models and ambient fish require per-entity names/recognized models.
    // Parrots share several Forest species; type plus biome cannot distinguish them.
    private static final Set<String> UNSAFE=Set.of("minecraft:player","minecraft:item_display","minecraft:block_display",
        "minecraft:text_display","minecraft:interaction","minecraft:armor_stand","minecraft:tropical_fish","minecraft:parrot");
    private final Map<Key,EnumSet<SafariSpecies>> observed=new HashMap<>();
    public void clear() { observed.clear(); }
    public void observe(String type,SafariSpecies species) {
        if(species==null || UNSAFE.contains(type)) return;
        observed.computeIfAbsent(new Key(type,species.biome),key -> EnumSet.noneOf(SafariSpecies.class)).add(species);
    }
    public SafariSpecies infer(String type,SafariSpecies.Biome biome) {
        if(biome==null || UNSAFE.contains(type)) return null;
        var candidates=observed.get(new Key(type,biome));
        return candidates!=null && candidates.size()==1?candidates.iterator().next():null;
    }
}
