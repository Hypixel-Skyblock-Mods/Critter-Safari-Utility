package org.hypixelskyblockmods.crittersafariesp;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DetectionRulesTest {
    @Test void shardTradeAndClickLabelsAreNotCritterNames() {
        assertFalse(DetectionRules.critterLabel("Snoozle Shard"));
        assertFalse(DetectionRules.critterLabel("Litterbug CLICK"));
        assertFalse(DetectionRules.critterLabel("[NPC] Macaw Trader"));
        assertTrue(DetectionRules.critterLabel("SPARKLING Honeybug 20❤"));
    }
    @Test void distinguishesSafariFromEntranceAndOtherAreas() {
        assertTrue(DetectionRules.safariArea("§r§bArea: §aSafari§r"));
        assertTrue(DetectionRules.safariArea("Area: Critter Safari"));
        assertTrue(DetectionRules.safariArea("Area: Safari Zone"));
        assertFalse(DetectionRules.safariArea("Area: Safari Zone Entrance"));
        assertFalse(DetectionRules.safariArea("Area: Torrhus Canyon"));
        assertFalse(DetectionRules.safariArea("Safari"));
    }
    @Test void hostMatchingDoesNotAcceptLookalikeDomains() {
        assertTrue(DetectionRules.hypixelAddress("mc.hypixel.net:25565"));
        assertTrue(DetectionRules.hypixelAddress("HYPIXEL.NET"));
        assertFalse(DetectionRules.hypixelAddress("hypixel.net.example.com"));
        assertFalse(DetectionRules.hypixelAddress("fakehypixel.net"));
    }
    @Test void namesMatchWordBoundariesAndSparklingPrefixes() {
        var names = List.of("Macaw", "Mantis Shrimp", "Tepid");
        assertEquals("Macaw", DetectionRules.matchingName("§aSparkling Macaw §c20❤", names));
        assertEquals("Mantis Shrimp", DetectionRules.matchingName("[Lv10] MANTIS SHRIMP", names));
        assertNull(DetectionRules.matchingName("Macawkeeper", names));
        assertNull(DetectionRules.matchingName("The Tepidly Named NPC", names));
    }
    @Test void sphericalRangeIncludesExactBoundaryAndRejectsInvalidDistances() {
        assertTrue(DetectionRules.inRange(10000, 100));
        assertFalse(DetectionRules.inRange(10000.01, 100));
        assertFalse(DetectionRules.inRange(80 * 80 + 80 * 80, 100));
        assertTrue(DetectionRules.inRange(60 * 60 + 80 * 80, 100));
        assertFalse(DetectionRules.inRange(Double.NaN, 100));
        assertFalse(DetectionRules.inRange(-1, 100));
    }
    @Test void genericModelsCannotUseTheMixedModeTypeFallback() {
        for (String type : List.of("minecraft:player", "minecraft:item_display", "minecraft:interaction")) {
            assertFalse(DetectionRules.typeFallback(type, "NAME"));
            assertFalse(DetectionRules.typeFallback(type, "BOTH"));
            assertTrue(DetectionRules.typeFallback(type, "TYPE"));
        }
        assertTrue(DetectionRules.typeFallback("minecraft:bee", "BOTH"));
        assertFalse(DetectionRules.typeFallback("minecraft:bee", "NAME"));
    }
}
