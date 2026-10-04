package org.hypixelskyblockmods.crittersafariesp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;
    public ConfigStore(Path path) { this.path = path; }
    public Settings load() throws IOException {
        if (!Files.exists(path)) {
            Settings settings = new Settings();
            save(settings);
            return settings;
        }
        try {
            String raw = Files.readString(path);
            var json = JsonParser.parseString(raw).getAsJsonObject();
            Settings settings = GSON.fromJson(json, Settings.class);
            if (settings == null) throw new IllegalArgumentException("Empty config");
            settings.normalize();
            // Upgrade only unchanged presets. Mixed mode now excludes unnamed generic NPC/prop models.
            if ((!json.has("detectionRevision") || json.get("detectionRevision").getAsInt()<2)
                && !settings.detection.equals("TYPE") && settings.entityTypes.equals(Catalog.DEFAULTS.entityTypes()) && settings.names.equals(Catalog.DEFAULTS.names())) {
                Path backup=path.resolveSibling(path.getFileName()+".before-model-fix");
                if(!Files.exists(backup)) Files.copy(path,backup);
                settings.detection="BOTH"; settings.detectionRevision=2;
                save(settings);
            }
            return settings;
        } catch (RuntimeException e) {
            Path backup = path.resolveSibling(path.getFileName() + ".invalid-" + System.currentTimeMillis());
            Files.move(path, backup);
            Settings settings = new Settings();
            save(settings);
            return settings;
        }
    }
    public void save(Settings settings) throws IOException {
        settings.normalize();
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(temporary, GSON.toJson(settings) + "\n");
        try {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
