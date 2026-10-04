package org.hypixelskyblockmods.crittersafariesp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ConfigStoreTest {
    @Test void radiusIsAnExactIntegerWithTheRequestedHundredBlockLimit() {
        var input=new SettingsScreen.General();
        input.radius="12"; assertEquals(12,input.parsedRadius());
        input.radius="100"; assertEquals(100,input.parsedRadius());
        input.radius="128"; assertNull(input.parsedRadius());
        input.radius="12.8"; assertNull(input.parsedRadius());
        input.radius=""; assertNull(input.parsedRadius());
        var settings=new Settings(); settings.radius=128; settings.normalize(); assertEquals(100,settings.radius);
        settings.radius=12; settings.normalize(); assertEquals(12,settings.radius);
    }
    @TempDir Path directory;
    @Test void savesAndReloadsCustomFiltersAndColors() throws Exception {
        var store = new ConfigStore(directory.resolve("config.json"));
        var settings = store.load();
        settings.radius = 83; settings.entityTypes.clear(); settings.entityTypes.add("minecraft:bee");
        settings.typeColors.put("minecraft:bee", "#FFFF00");
        settings.hudX = 0.75f; settings.hudY = 0.25f;
        settings.fillOpacity = 0.23f; settings.smoothingMillis = 45;
        settings.ballSpeed = 1.8f; settings.hitColor = "#00FFAA";
        settings.filledBoxes = false; store.save(settings);
        var reloaded = store.load();
        assertEquals(83, reloaded.radius); assertFalse(reloaded.filledBoxes);
        assertEquals(java.util.List.of("minecraft:bee"), reloaded.entityTypes);
        assertEquals(0xFFFF00, reloaded.rgb("minecraft:bee"));
        assertEquals(0.75f, reloaded.hudX); assertEquals(0.25f, reloaded.hudY);
        assertEquals(0.23f, reloaded.fillOpacity); assertEquals(45, reloaded.smoothingMillis);
        assertEquals(1.8f, reloaded.ballSpeed); assertEquals(0x00FFAA, reloaded.hitRgb());
        settings.oneCritterMode=true; settings.biomePanels=false; settings.biomeX[1]=.8f; settings.biomeY[2]=.6f;
        settings.hudScale=1.25f; settings.biomeScale[1]=.75f;
        store.save(settings); reloaded=store.load();
        assertTrue(reloaded.oneCritterMode); assertFalse(reloaded.biomePanels);
        assertEquals(.8f,reloaded.biomeX[1]); assertEquals(.6f,reloaded.biomeY[2]);
        assertEquals(1.25f,reloaded.hudScale); assertEquals(.75f,reloaded.biomeScale[1]);
    }
    @Test void preservesBrokenConfigBeforeRestoringDefaults() throws Exception {
        Path path = directory.resolve("config.json");
        Files.writeString(path, "{broken");
        var loaded = new ConfigStore(path).load();
        assertEquals(100, loaded.radius);
        try (var files = Files.list(directory)) {
            assertTrue(files.anyMatch(p -> p.getFileName().toString().startsWith("config.json.invalid-")));
        }
    }
    @Test void invalidValuesAreNormalizedAndEmptyFiltersRemainEmpty() {
        var settings = new Settings();
        settings.radius = Double.NaN; settings.lineWidth = 100; settings.opacity = -10;
        settings.scanIntervalTicks = 0; settings.color = "bad"; settings.origin = null;
        settings.detection = null; settings.entityTypes.clear(); settings.names.clear();
        settings.normalize();
        assertEquals(100, settings.radius); assertEquals(5, settings.lineWidth); assertEquals(0.1f, settings.opacity);
        assertEquals(1, settings.scanIntervalTicks); assertTrue(Settings.validColor(settings.color));
        assertEquals("CROSSHAIR", settings.origin); assertEquals("NAME", settings.detection);
        assertTrue(settings.entityTypes.isEmpty()); assertTrue(settings.names.isEmpty());
    }
    @Test void upgradesLegacyPresetPreservingHudAndClampingToRequestedRadiusLimit() throws Exception {
        Path path = directory.resolve("config.json");
        String original = "{\"detection\":\"BOTH\",\"radius\":201,\"hudX\":1,\"hudY\":0.7}";
        Files.writeString(path, original);
        var store = new ConfigStore(path);
        var settings = store.load();
        assertEquals("BOTH", settings.detection); assertEquals(100, settings.radius);
        assertEquals(1, settings.hudX); assertEquals(0.7f, settings.hudY);
        assertEquals(original, Files.readString(directory.resolve("config.json.before-model-fix")));
        settings.detection = "BOTH"; store.save(settings);
        assertEquals("BOTH", store.load().detection); // A subsequent intentional selection is respected.
    }
    @Test void doesNotReplaceCustomLegacyTypeFilters() throws Exception {
        Path path = directory.resolve("config.json");
        Files.writeString(path, "{\"detection\":\"BOTH\",\"entityTypes\":[\"minecraft:bee\"]}");
        assertEquals("BOTH", new ConfigStore(path).load().detection);
        assertFalse(Files.exists(directory.resolve("config.json.before-model-fix")));
    }
}
