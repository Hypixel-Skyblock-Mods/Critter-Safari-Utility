package org.hypixelskyblockmods.crittersafariesp;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** Named Safari species only; vanilla model types are deliberately not species mappings. */
public enum SafariSpecies {
    CAVERNFISH("Cavernfish",Rarity.COMMON,Biome.CAVERN), FLITTER("Flitter",Rarity.COMMON,Biome.CAVERN), SHYWORM("Shyworm",Rarity.COMMON,Biome.CAVERN),
    DRIFTLING("Driftling",Rarity.UNCOMMON,Biome.CAVERN), CHUCKWALLA("Chuckwalla",Rarity.RARE,Biome.CAVERN), ROCKMITE("Rockmite",Rarity.RARE,Biome.CAVERN),
    SCRAPPY("Scrappy",Rarity.RARE,Biome.CAVERN), SNOOZLE("Snoozle",Rarity.RARE,Biome.CAVERN), GEMZIE("Gemzie",Rarity.EPIC,Biome.CAVERN),
    FOXTROT("Foxtrot",Rarity.COMMON,Biome.FOREST), HONEYBUG("Honeybug",Rarity.UNCOMMON,Biome.FOREST), HIDEONFLOOR("Hideonfloor",Rarity.RARE,Biome.FOREST),
    TREEFROG("Treefrog",Rarity.UNCOMMON,Biome.FOREST), WOODCHUCKER("Woodchucker",Rarity.UNCOMMON,Biome.FOREST), FLUFFLING("Fluffling",Rarity.RARE,Biome.FOREST),
    BLUEBIRD("Bluebird",Rarity.UNCOMMON,Biome.FOREST), PARAKEET("Parakeet",Rarity.RARE,Biome.FOREST), MACAW("Macaw",Rarity.LEGENDARY,Biome.FOREST),
    SOLSNATCHER("Solsnatcher",Rarity.UNCOMMON,Biome.HAUNTED), BLOODBAT("Bloodbat",Rarity.UNCOMMON,Biome.HAUNTED), LITTERBUG("Litterbug",Rarity.UNCOMMON,Biome.HAUNTED),
    AREITA("Areita",Rarity.UNCOMMON,Biome.HAUNTED), DUPLICO("Duplico",Rarity.UNCOMMON,Biome.HAUNTED), GAZER("Gazer",Rarity.UNCOMMON,Biome.HAUNTED),
    HIDEONWALL("Hideonwall",Rarity.RARE,Biome.HAUNTED), GIMMIEGOLD("Gimmiegold",Rarity.RARE,Biome.HAUNTED), HIDEYHO("Hideyho",Rarity.RARE,Biome.HAUNTED),
    DOOMSPIRAL("Doomspiral",Rarity.LEGENDARY,Biome.HAUNTED), STRONGARM("Strongarm",Rarity.COMMON,Biome.ICY), POLARIS("Polaris",Rarity.UNCOMMON,Biome.ICY),
    BILLYGOAT("Billygoat",Rarity.RARE,Biome.ICY), TEPID("Tepid",Rarity.COMMON,Biome.ICY), NOZZLENOSE("Nozzlenose",Rarity.RARE,Biome.ICY),
    MANTIS_SHRIMP("Mantis Shrimp",Rarity.RARE,Biome.ICY), SHUDDERSQUID("Shuddersquid",Rarity.UNCOMMON,Biome.ICY), TROODON("Troodon",Rarity.RARE,Biome.ICY), WUMPA("Wumpa",Rarity.LEGENDARY,Biome.ICY);

    public enum Biome {
        CAVERN("Cavern",0xFFBF64), FOREST("Forest",0x72D98A), HAUNTED("Haunted",0xC28AFF), ICY("Icy",0x7BDDED);
        public final String title;
        public final int color;
        Biome(String title,int color) { this.title=title; this.color=color; }
        public List<SafariSpecies> species() { return Lists.BY_BIOME.get(this); }
    }
    private static final class Lists {
        static final EnumMap<Biome,List<SafariSpecies>> BY_BIOME=new EnumMap<>(Biome.class);
        static { for(var biome:Biome.values()) BY_BIOME.put(biome,Arrays.stream(values()).filter(s -> s.biome==biome)
            .sorted(Comparator.comparingInt((SafariSpecies s) -> s.rarity.ordinal()).reversed().thenComparing(s -> s.title)).toList()); }
    }
    public enum Rarity {
        COMMON(0xFFFFFF), UNCOMMON(0x55FF55), RARE(0x5555FF), EPIC(0xAA00AA), LEGENDARY(0xFFAA00);
        public final int rgb;
        Rarity(int rgb) { this.rgb=rgb; }
        public int color(boolean enabled) {
            if(enabled) return 0xFF000000|rgb;
            return 0xFF000000|((rgb>>16 & 255)/2<<16)|((rgb>>8 & 255)/2<<8)|(rgb & 255)/2;
        }
    }
    public final String title;
    public final Rarity rarity;
    public final Biome biome;
    SafariSpecies(String title,Rarity rarity,Biome biome) { this.title=title; this.rarity=rarity; this.biome=biome; }
    private static final Map<String,SafariSpecies> BY_NAME=new HashMap<>();
    static {
        for(var species:values()) BY_NAME.put(species.title.toLowerCase(Locale.ROOT).replace(" ",""),species);
        BY_NAME.put("yubat",FLITTER);
    }
    public static SafariSpecies named(String name) {
        String plain=DetectionRules.plain(name).toLowerCase(Locale.ROOT).replaceFirst("^sparkling\\s+","").replaceAll("\\s+","");
        return BY_NAME.get(plain);
    }
}
