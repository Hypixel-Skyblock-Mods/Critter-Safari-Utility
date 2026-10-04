package org.hypixelskyblockmods.crittersafariesp;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.Set;

public final class DetectionRules {
    private static final Pattern FORMATTING = Pattern.compile("(?i)§[0-9A-FK-ORX]");
    private static final Pattern AREA = Pattern.compile("(?i)\\bArea:\\s*(?:Critter\\s+)?Safari(?:\\s+Zone)?\\s*$");
    private static final Set<String> GENERIC_MODELS = Set.of("minecraft:player", "minecraft:item_display", "minecraft:interaction");
    private static final Pattern PROP_LABEL = Pattern.compile("(?i)\\b(?:shards?|click|trade|trading|capsules?|npc)\\b");
    private DetectionRules() {}
    public static String plain(String raw) { return FORMATTING.matcher(raw).replaceAll("").strip(); }
    public static boolean safariArea(String line) { return AREA.matcher(plain(line)).find(); }
    public static boolean hypixelAddress(String raw) {
        String host = raw.toLowerCase(Locale.ROOT).replaceFirst(":\\d+$", "");
        return host.equals("hypixel.net") || host.endsWith(".hypixel.net");
    }
    public static String matchingName(String raw, List<String> names) {
        String normalized = plain(raw).toLowerCase(Locale.ROOT);
        for (String name : names) {
            String needle = name.toLowerCase(Locale.ROOT);
            int start = normalized.indexOf(needle);
            while (start >= 0) {
                int end = start + needle.length();
                if ((start == 0 || !Character.isLetterOrDigit(normalized.charAt(start - 1)))
                    && (end == normalized.length() || !Character.isLetterOrDigit(normalized.charAt(end)))) return name;
                start = normalized.indexOf(needle, start + 1);
            }
        }
        return null;
    }
    public static boolean inRange(double squaredDistance, double radius) {
        return Double.isFinite(squaredDistance) && squaredDistance >= 0 && squaredDistance <= radius * radius;
    }
    public static boolean typeFallback(String type, String mode) {
        return mode.equals("TYPE") || (mode.equals("BOTH") && !GENERIC_MODELS.contains(type));
    }
    public static boolean critterLabel(String text) { return !PROP_LABEL.matcher(plain(text)).find(); }
}
