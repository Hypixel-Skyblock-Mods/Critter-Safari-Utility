package org.hypixelskyblockmods.crittersafariesp;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SafariAreasTest {
    @Test void biomeResolutionUsesMapPositionIncludingCavesAndRejectsEntranceAndUnknownSpace() {
        assertEquals(SafariSpecies.Biome.CAVERN,SafariAreas.at(new Vec3(-76,41,36)));
        assertEquals(SafariSpecies.Biome.HAUNTED,SafariAreas.at(new Vec3(47,68,-19)));
        assertEquals(SafariSpecies.Biome.ICY,SafariAreas.at(new Vec3(-124,81,-89)));
        assertEquals(SafariSpecies.Biome.FOREST,SafariAreas.at(new Vec3(1,66,27)));
        assertNull(SafariAreas.at(new Vec3(-52,69,21)));
        assertNull(SafariAreas.at(new Vec3(1000,1000,1000)));
    }
}
