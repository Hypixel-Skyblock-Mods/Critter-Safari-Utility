package org.hypixelskyblockmods.crittersafariesp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpeciesHintsTest {
    @Test void learnedTypeAndBiomeApplyCaughtFiltersBeforeANewBodyHasAName() {
        var hints=new SpeciesHints(); var run=new SafariRun(); run.sync(new Object(),true,true);
        hints.observe("minecraft:bee",SafariSpecies.HONEYBUG);
        run.receive("CAPTURE! You caught a Honeybug and gained a Honeybug Shard!");
        var unnamed=hints.infer("minecraft:bee",SafariSpecies.Biome.FOREST);
        assertEquals(SafariSpecies.HONEYBUG,unnamed); assertFalse(run.enabled(unnamed));
        run.toggle(SafariSpecies.HONEYBUG); assertTrue(run.enabled(unnamed));
        run.reset(); assertTrue(run.enabled(unnamed));
        hints.clear(); assertNull(hints.infer("minecraft:bee",SafariSpecies.Biome.FOREST));
    }
    @Test void sameTypeInDifferentBiomesIsDistinctAndSameBiomeAmbiguityIsRejected() {
        var hints=new SpeciesHints(); hints.observe("minecraft:bat",SafariSpecies.FLITTER);
        hints.observe("minecraft:bat",SafariSpecies.BLOODBAT);
        assertEquals(SafariSpecies.FLITTER,hints.infer("minecraft:bat",SafariSpecies.Biome.CAVERN));
        assertEquals(SafariSpecies.BLOODBAT,hints.infer("minecraft:bat",SafariSpecies.Biome.HAUNTED));
        assertNull(hints.infer("minecraft:bat",SafariSpecies.Biome.FOREST));
        hints.observe("minecraft:bat",SafariSpecies.GEMZIE);
        assertNull(hints.infer("minecraft:bat",SafariSpecies.Biome.CAVERN));
        assertEquals(SafariSpecies.BLOODBAT,hints.infer("minecraft:bat",SafariSpecies.Biome.HAUNTED));
    }
    @Test void ambientFishGenericNpcsAndSharedForestParrotsCannotBecomeTypeOnlyMatches() {
        var hints=new SpeciesHints();
        for(String type:java.util.List.of("minecraft:tropical_fish","minecraft:parrot","minecraft:player","minecraft:item_display","minecraft:interaction")) {
            hints.observe(type,SafariSpecies.HONEYBUG);
            assertNull(hints.infer(type,SafariSpecies.Biome.FOREST));
        }
        assertNull(hints.infer("minecraft:bee",null));
    }
}
