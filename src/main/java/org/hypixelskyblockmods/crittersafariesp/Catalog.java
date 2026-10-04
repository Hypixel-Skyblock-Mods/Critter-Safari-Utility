package org.hypixelskyblockmods.crittersafariesp;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class Catalog {
    public record Data(List<String> entityTypes, List<String> names) {}
    public static final Data DEFAULTS = load();
    private Catalog() {}
    private static Data load() {
        try (var stream = Catalog.class.getResourceAsStream("/assets/crittersafariesp/catalog.json")) {
            if (stream == null) throw new IllegalStateException("Missing critter catalog");
            return new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), Data.class);
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }
}
