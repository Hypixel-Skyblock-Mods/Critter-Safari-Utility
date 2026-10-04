package org.hypixelskyblockmods.crittersafariesp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SafariRunTest {
    @Test void capturesAndLootShareResolveTheCritterAndRequireMatchingShard() {
        assertEquals(new SafariRun.Catch(SafariSpecies.FOXTROT,false),SafariRun.parse("§a§lCAPTURE! §7You caught a §fFoxtrot§7 and gained a §fFoxtrot Shard§7!"));
        assertEquals(new SafariRun.Catch(SafariSpecies.SOLSNATCHER,false),SafariRun.parse("CAPTURE! You caught a Solsnatcher and gained 2x Solsnatcher Shard! (2)"));
        assertEquals(new SafariRun.Catch(SafariSpecies.PARAKEET,true),SafariRun.parse("LOOT SHARE! You received 2x Parakeet Shard from meowgirlemily catching a Parakeet!"));
        assertEquals(new SafariRun.Catch(SafariSpecies.AREITA,true),SafariRun.parse("LOOT SHARE! You received an Areita Shard from VirulentNyx catching an Areita!"));
        assertEquals(new SafariRun.Catch(SafariSpecies.HIDEYHO,false),SafariRun.parse("CAPTURE! You found the Hideyho, and as a reward it gave you 4x Hideyho Shard!"));
        assertEquals(new SafariRun.Catch(SafariSpecies.TREEFROG,false),SafariRun.parse("CAPTURE! You caught a Sparkling Tree Frog and gained a Treefrog Shard!"));
        assertEquals(SafariSpecies.FLITTER,SafariSpecies.named("Yubat"));
    }
    @Test void unrelatedRewardsChatAndEscapesNeverMarkACatch() {
        for(String message:new String[]{"FLOOR DROP! You found Foxtrot Shard on the ground!",
            "LOOT SHARE You received a Glacite Walker Shard for assisting Mealoan!",
            "CAPTURE! You caught a Foxtrot and gained a Honeybug Shard!",
            "[VIP] Player: CAPTURE! You caught a Foxtrot and gained a Foxtrot Shard!",
            "The Foxtrot escaped your Critter Capsule!","You threw a Critter Capsule at the Foxtrot!"})
            assertNull(SafariRun.parse(message),message);
    }
    @Test void modeHidesOnlyCaughtSpeciesWithSessionOverridesAndNewRunReset() {
        var run=new SafariRun(); Object world=new Object(); run.sync(world,true,false);
        run.receive("CAPTURE! You caught a Foxtrot and gained a Foxtrot Shard!");
        assertTrue(run.enabled(SafariSpecies.FOXTROT));
        run.sync(world,true,true); assertFalse(run.enabled(SafariSpecies.FOXTROT));
        assertTrue(run.enabled(SafariSpecies.HONEYBUG));
        run.toggle(SafariSpecies.FOXTROT); assertTrue(run.enabled(SafariSpecies.FOXTROT));
        run.receive("CAPTURE! You caught a Foxtrot and gained a Foxtrot Shard!");
        assertFalse(run.enabled(SafariSpecies.FOXTROT));
        run.setAll(SafariSpecies.Biome.FOREST,true); assertTrue(run.enabled(SafariSpecies.FOXTROT));
        run.setAll(SafariSpecies.Biome.CAVERN,false);
        assertFalse(run.enabled(SafariSpecies.FLITTER)); assertTrue(run.enabled(SafariSpecies.WUMPA));
        run.sync(world,true,true); assertFalse(run.enabled(SafariSpecies.FLITTER));
        run.sync(new Object(),true,true);
        for(var species:SafariSpecies.values()) { assertTrue(run.enabled(species)); assertFalse(run.caught(species)); }
        run.setAll(SafariSpecies.Biome.ICY,false); run.sync(run,false,true); run.sync(run,true,true);
        assertTrue(run.enabled(SafariSpecies.WUMPA));
    }
    @Test void sharedCatchAndUnknownModelsAreHandledWithoutInventedMappings() {
        var run=new SafariRun(); run.sync(new Object(),true,true);
        run.receive("LOOT SHARE! You received a Parakeet Shard from Someone catching a Parakeet!");
        assertTrue(run.caught(SafariSpecies.PARAKEET)); assertTrue(run.shared(SafariSpecies.PARAKEET));
        assertFalse(run.enabled(SafariSpecies.PARAKEET));
        assertNull(SafariSpecies.named("bat")); assertTrue(run.enabled(null));
        assertEquals(37,SafariSpecies.values().length);
        assertEquals(10,SafariSpecies.Biome.HAUNTED.species().size());
    }
    @Test void disabledModeDoesNotAutoHideCatchesAndPanelOrdersStayRarityThenAlphabetical() {
        var run=new SafariRun(); var world=new Object(); run.sync(world,true,false);
        run.receive("LOOT SHARE! You received a Parakeet Shard from Someone catching a Parakeet!");
        assertTrue(run.enabled(SafariSpecies.PARAKEET));
        run.toggle(SafariSpecies.PARAKEET);
        assertFalse(BiomePanels.rows(SafariSpecies.Biome.FOREST,run,false).contains(SafariSpecies.PARAKEET));
        assertTrue(BiomePanels.rows(SafariSpecies.Biome.FOREST,run,true).contains(SafariSpecies.PARAKEET));
        for(var biome:SafariSpecies.Biome.values()) {
            var order=biome.species();
            assertEquals(order.stream().sorted(java.util.Comparator.comparingInt((SafariSpecies s) -> s.rarity.ordinal()).reversed().thenComparing(s -> s.title)).toList(),order);
            assertEquals(order,BiomePanels.rows(biome,run,true));
            for(var species:order) {
                assertEquals(0xFF000000|species.rarity.rgb,species.rarity.color(true));
                int dim=species.rarity.color(false);
                for(int shift:new int[]{0,8,16}) assertEquals(((species.rarity.rgb>>shift)&255)/2,(dim>>shift)&255);
            }
        }
        run.toggle(SafariSpecies.PARAKEET);
        run.receive("CAPTURE! You caught a Parakeet and gained a Parakeet Shard!");
        assertTrue(run.enabled(SafariSpecies.PARAKEET));
    }
    @Test void onlyOwnSafariEntryResetsTheRun() {
        assertTrue(SafariRun.ownEntry("-----------------------------\n[MVP+] Kappppu entered Critter Safari!\n-----------------------------","Kappppu"));
        assertFalse(SafariRun.ownEntry("[MVP+] Other entered Critter Safari!","Kappppu"));
        assertFalse(SafariRun.ownEntry("[VIP] Other: Kappppu entered Critter Safari!","Kappppu"));
    }
}
