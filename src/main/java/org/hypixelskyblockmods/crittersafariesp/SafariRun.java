package org.hypixelskyblockmods.crittersafariesp;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.regex.Pattern;

/** Session-only catches and manual overrides. Positions and mode preferences live in Settings. */
public final class SafariRun {
    public record Catch(SafariSpecies species,boolean shared) {}
    private static final Pattern PERSONAL=Pattern.compile("^CAPTURE! You caught an? (.+?) and gained (?:an?|\\d+x?) (.+?) Shards?!$");
    private static final Pattern FOUND=Pattern.compile("^CAPTURE! You found (?:the )?(.+?),? and as a reward (?:he|she|they|it) gave you (?:an?|\\d+x?) (.+?) Shards?!$");
    private static final Pattern SHARED=Pattern.compile("^LOOT SHARE!? You received (?:an?|\\d+x?) (.+?) Shards? from \\S+ catching an? (.+?)!$");
    private final EnumSet<SafariSpecies> caught=EnumSet.noneOf(SafariSpecies.class);
    private final EnumSet<SafariSpecies> shared=EnumSet.noneOf(SafariSpecies.class);
    private final EnumMap<SafariSpecies,Boolean> overrides=new EnumMap<>(SafariSpecies.class);
    private Object world;
    private boolean area,oneMode;
    private long revision;
    public long revision() { return revision; }

    public void sync(Object currentWorld,boolean inArea,boolean mode) {
        if(world!=currentWorld || (!area && inArea)) reset();
        world=currentWorld; area=inArea;
        if(mode!=oneMode) { if(mode) caught.forEach(overrides::remove); revision++; }
        oneMode=mode;
    }
    public void reset() { caught.clear(); shared.clear(); overrides.clear(); revision++; }
    public boolean caught(SafariSpecies species) { return caught.contains(species); }
    public boolean shared(SafariSpecies species) { return shared.contains(species); }
    public boolean enabled(SafariSpecies species) {
        return species==null || overrides.getOrDefault(species,!(oneMode && caught.contains(species)));
    }
    public void toggle(SafariSpecies species) { overrides.put(species,!enabled(species)); revision++; }
    public void setAll(SafariSpecies.Biome biome,boolean enabled) { biome.species().forEach(s -> overrides.put(s,enabled)); revision++; }
    public void receive(String text) {
        Catch event=parse(text);
        if(event==null) return;
        caught.add(event.species());
        if(event.shared()) shared.add(event.species());
        // A fresh confirmed catch wins over a previous manual re-enable in one-critter mode.
        if(oneMode) overrides.remove(event.species());
        revision++;
    }
    public static Catch parse(String raw) {
        String text=DetectionRules.plain(raw).replaceAll("\\s+"," ").replaceFirst(" \\(\\d+\\)$","");
        var personal=PERSONAL.matcher(text); var found=FOUND.matcher(text); var share=SHARED.matcher(text);
        String critter,shard; boolean shared;
        if(personal.matches()) { critter=personal.group(1); shard=personal.group(2); shared=false; }
        else if(found.matches()) { critter=found.group(1); shard=found.group(2); shared=false; }
        else if(share.matches()) { shard=share.group(1); critter=share.group(2); shared=true; }
        else return null;
        var species=SafariSpecies.named(critter);
        return species!=null && species==SafariSpecies.named(shard)?new Catch(species,shared):null;
    }
    public static boolean ownEntry(String raw,String username) {
        String text=DetectionRules.plain(raw).replaceAll("\\s+"," ");
        return Pattern.compile("^(?:(?:\\[[^]]+]) )?"+Pattern.quote(username)+" entered Critter Safari!$")
            .matcher(text.replaceAll("-{3,}"," ").strip()).matches();
    }
}
