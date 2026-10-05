package org.hypixelskyblockmods.crittersafariesp;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.hypixelskyblockmods.crittersafariesp.platform.ClientCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class CritterSafariClient implements ClientModInitializer {
    public static final String MOD_ID = "crittersafariesp";
    public static final Logger LOGGER = LoggerFactory.getLogger("CritterSafariUtility");
    public static Settings settings;
    public static final SafariRun run = new SafariRun();
    public static final CritterTracker tracker = new CritterTracker();
    private static ConfigStore store;
    private static KeyMapping toggleBinding, settingsBinding;
    private boolean openSettings;

    @Override public void onInitializeClient() {
        store = new ConfigStore(FabricLoader.getInstance().getConfigDir().resolve("crittersafariesp.json"));
        reload();
        var category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        KeyMapping toggle = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.crittersafariesp.toggle",
            ClientCompat.keyboardType(), InputConstants.UNKNOWN.getValue(), category));
        KeyMapping configure = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.crittersafariesp.settings",
            ClientCompat.keyboardType(), InputConstants.UNKNOWN.getValue(), category));
        toggleBinding = toggle; settingsBinding = configure;
        var capturePhase=Identifier.fromNamespaceAndPath(MOD_ID,"capture_observer");
        ClientReceiveMessageEvents.ALLOW_GAME.addPhaseOrdering(capturePhase,Event.DEFAULT_PHASE);
        ClientReceiveMessageEvents.ALLOW_GAME.register(capturePhase,(message,overlay) -> {
            if(!overlay) receiveSafariMessage(Minecraft.getInstance(),message.getString());
            return true;
        });
        ScreenEvents.AFTER_INIT.register((mc,screen,width,height) -> {
            if(!BiomePanels.interactive(screen)) return;
            ScreenMouseEvents.allowMouseClick(screen).register((s,event) -> !BiomePanels.click(event,s.width,s.height,false));
            ScreenMouseEvents.allowMouseDrag(screen).register((s,event,dx,dy) -> !BiomePanels.drag(event,s.width,s.height));
            ScreenMouseEvents.allowMouseRelease(screen).register((s,event) -> !BiomePanels.release(event));
            ScreenEvents.remove(screen).register(s -> BiomePanels.finishDrag());
            ScreenEvents.afterExtract(screen).register((s,graphics,x,y,delta) -> {
                if(BiomePanels.visible()) BiomePanels.draw(graphics,x,y,true);
            });
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
            var command = dispatcher.register(
            literal("csu").executes(context -> { openSettings = true; return 1; })
                .then(literal("toggle").executes(context -> { toggle(); return 1; }))
                .then(literal("reload").executes(context -> { reload(); context.getSource().sendFeedback(Component.literal("CSU settings reloaded.")); return 1; }))
                .then(literal("dump").executes(context -> {
                    try {
                        dumpEntities();
                        context.getSource().sendFeedback(Component.literal("Saved config/crittersafariesp/entity-dump.json"));
                        return 1;
                    } catch (IOException e) {
                        LOGGER.error("Could not dump entities", e);
                        context.getSource().sendError(Component.literal("Could not save dump; see latest.log."));
                        return 0;
                    }
                })));
            dispatcher.register(literal("critteresp").executes(context -> { openSettings = true; return 1; }).redirect(command));
        });
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (toggle.consumeClick()) toggle();
            while (configure.consumeClick()) openSettings = true;
            if (openSettings) {
                openSettings = false;
                ClientCompat.setScreen(mc, new SettingsScreen(ClientCompat.screen(mc)));
            }
            syncRun(mc);
            tracker.tick(mc, settings);
        });
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath(MOD_ID, "tracers"), TracerRenderer::extract);
        HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath(MOD_ID,"biomes"),(graphics,delta) -> {
                var mc=Minecraft.getInstance();
                if(ClientCompat.screen(mc)==null && BiomePanels.visible()) BiomePanels.draw(graphics,-1,-1,false);
            });
    }
    private static void syncRun(Minecraft mc) { run.sync(mc.level,CritterTracker.inArea(mc,settings),settings.oneCritterMode); }
    public static void receiveSafariMessage(Minecraft mc,String text) {
        syncRun(mc);
        if(mc.player==null || mc.level==null) return;
        boolean hypixel=mc.getCurrentServer()!=null && DetectionRules.hypixelAddress(mc.getCurrentServer().ip);
        if((hypixel || !settings.safariOnly) && SafariRun.ownEntry(text,mc.player.getGameProfile().name())) run.reset();
        else if(CritterTracker.inArea(mc,settings)) run.receive(text);
    }
    public static void apply(Settings updated) { settings = updated; save(); syncRun(Minecraft.getInstance()); tracker.refresh(); }
    public static int toggleKeyCode() { return ClientCompat.configCode(InputConstants.getKey(toggleBinding.saveString())); }
    public static int settingsKeyCode() { return ClientCompat.configCode(InputConstants.getKey(settingsBinding.saveString())); }
    public static void setConfigKeys(int toggle, int settings) {
        boolean changed = updateKey(toggleBinding, toggle) | updateKey(settingsBinding, settings);
        if (changed) { KeyMapping.resetMapping(); Minecraft.getInstance().options.save(); }
    }
    private static boolean updateKey(KeyMapping binding, int code) {
        InputConstants.Key key = code == -1 ? InputConstants.UNKNOWN
            : (code >= 0 && code <= 9 ? InputConstants.Type.MOUSE : ClientCompat.keyboardType()).getOrCreate(code);
        if (binding.saveString().equals(key.getName())) return false;
        binding.setKey(key); return true;
    }
    public static void save() {
        try { store.save(settings); }
        catch (IOException e) { LOGGER.error("Could not save CSU settings", e); }
    }
    private static void reload() {
        try { settings = store.load(); }
        catch (IOException e) { LOGGER.error("Could not load CSU settings", e); settings = new Settings(); }
        tracker.refresh();
    }
    private static void toggle() {
        settings.enabled = !settings.enabled;
        save(); tracker.refresh();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.sendOverlayMessage(Component.literal("CSU " + (settings.enabled ? "enabled" : "disabled")));
    }
    private static void dumpEntities() throws IOException {
        Minecraft mc = Minecraft.getInstance();
        var rows = new ArrayList<Map<String, Object>>();
        if (mc.level != null && mc.player != null) {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity == mc.player || !DetectionRules.inRange(entity.distanceToSqr(mc.player), settings.radius)) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", entity.getId()); row.put("type", CritterTracker.typeId(entity));
                row.put("name", DetectionRules.plain(CritterTracker.label(entity)));
                row.put("x", entity.getX()); row.put("y", entity.getY()); row.put("z", entity.getZ());
                rows.add(row);
            }
        }
        var path = FabricLoader.getInstance().getConfigDir().resolve("crittersafariesp/entity-dump.json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(rows) + "\n");
    }
}
