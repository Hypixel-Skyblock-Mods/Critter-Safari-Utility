package org.hypixelskyblockmods.crittersafariesp;

import com.google.gson.Gson;
import io.github.notenoughupdates.moulconfig.ChromaColour;
import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.annotations.*;
import io.github.notenoughupdates.moulconfig.common.text.StructuredText;
import io.github.notenoughupdates.moulconfig.gui.GuiContext;
import io.github.notenoughupdates.moulconfig.gui.GuiElementComponent;
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor;
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent;
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver;
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.hypixelskyblockmods.crittersafariesp.platform.ClientCompat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Standard MoulConfig editor, with the same screen integration as the neighboring SkyBlock mods. */
public final class SettingsScreen extends MoulConfigScreenComponent {
    final EditorConfig config;
    final MoulConfigEditor<EditorConfig> editor;
    private record Bundle(EditorConfig config, MoulConfigEditor<EditorConfig> editor) {}
    public SettingsScreen(Screen parent) { this(bundle(), parent); }
    private SettingsScreen(Bundle bundle, Screen parent) {
        super(Component.literal("Critter Safari Utility"), new GuiContext(new GuiElementComponent(bundle.editor())), parent);
        this.config = bundle.config(); this.editor = bundle.editor();
        config.general.moveHud = () -> {
            config.saveNow();
            ClientCompat.setScreen(net.minecraft.client.Minecraft.getInstance(), new HudPositionScreen(parent));
        };
    }
    private static Bundle bundle() { var config = new EditorConfig(); return new Bundle(config, editor(config)); }
    private static MoulConfigEditor<EditorConfig> editor(EditorConfig config) {
        var processor = MoulConfigProcessor.withDefaults(config);
        var driver = new ConfigProcessorDriver(processor);
        driver.checkExpose = false;
        driver.processConfig(config);
        return new MoulConfigEditor<>(processor);
    }
    @Override public void removed() { config.saveNow(); super.removed(); }
    @Override public void tick() {
        super.tick();
        // Apply range edits while the editor is open, rather than waiting for its close/save lifecycle.
        Integer entered=config.general.parsedRadius();
        if (entered!=null) {
            double radius = entered;
            if (CritterSafariClient.settings.radius != radius) {
                CritterSafariClient.settings.radius = radius;
                CritterSafariClient.save();
                CritterSafariClient.tracker.refresh();
            }
        }
    }

    public static final class EditorConfig extends Config {
        @Category(name = "General", desc = "Enable ESP, detection radius and Safari area control.")
        public final General general = new General();
        @Category(name = "Appearance", desc = "Smooth tracers, soft glow, translucent boxes and color.")
        public final Appearance appearance = new Appearance();
        @Category(name = "Detection", desc = "Filter critter names and the vanilla entity-type fallback.")
        public final Detection detection = new Detection();
        @Category(name = "Trajectory", desc = "Estimated Critter Capsule path and red/green hit feedback.")
        public final Trajectory trajectory = new Trajectory();
        private final transient Settings draft;

        public EditorConfig() {
            draft = new Gson().fromJson(new Gson().toJson(CritterSafariClient.settings), Settings.class);
            general.enabled = draft.enabled; general.radius = Integer.toString((int)draft.radius);
            general.safariOnly = draft.safariOnly; general.hud = draft.hud;
            general.floorDrops = draft.floorDrops;
            general.oneCritterMode=draft.oneCritterMode; general.biomePanels=draft.biomePanels;
            general.toggleKey = CritterSafariClient.toggleKeyCode();
            general.settingsKey = CritterSafariClient.settingsKeyCode();
            appearance.tracers = draft.tracers; appearance.boxes = draft.boxes;
            appearance.halo = draft.halo; appearance.haloStrength = draft.haloStrength;
            appearance.lineWidth = draft.lineWidth; appearance.opacity = draft.opacity;
            appearance.filledBoxes = draft.filledBoxes; appearance.fillOpacity = draft.fillOpacity;
            appearance.smoothing = draft.smoothingMillis;
            appearance.boxStyle = List.of("BOX_3D", "CORNERS", "BOX").indexOf(draft.boxStyle);
            appearance.origin = draft.origin.equals("CROSSHAIR") ? 0 : 1;
            appearance.aimNames = draft.aimNames; appearance.edges = draft.offscreenIndicators;
            int rgb = Integer.parseInt(draft.color.substring(1), 16);
            appearance.color = draft.chromaColor != null ? ChromaColour.forLegacyString(draft.chromaColor)
                : ChromaColour.fromStaticRGB(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, 255);
            int hitRgb = Integer.parseInt(draft.hitColor.substring(1), 16);
            appearance.hitColor = draft.chromaHitColor != null ? ChromaColour.forLegacyString(draft.chromaHitColor)
                : ChromaColour.fromStaticRGB(hitRgb >> 16 & 255, hitRgb >> 8 & 255, hitRgb & 255, 255);
            trajectory.enabled = draft.trajectory; trajectory.showPath = draft.showTrajectory;
            trajectory.movingTargets = draft.predictMotion;
            trajectory.speed = draft.ballSpeed; trajectory.gravity = draft.ballGravity;
            trajectory.drag = draft.ballDrag; trajectory.radius = draft.ballRadius;
            trajectory.hitTolerance=draft.hitTolerance; trajectory.aimCircle=draft.aimCircle;
            trajectory.ticks = draft.predictionTicks;
            detection.mode = List.of("BOTH", "NAME", "TYPE").indexOf(draft.detection);
            detection.names = String.join(", ", draft.names);
            detection.exclusions = String.join(", ", draft.excludedNames);
            detection.types = String.join(", ", draft.entityTypes);
            saveRunnables.add(this::persist);
        }
        @Override public StructuredText getTitle() { return StructuredText.of("Critter Safari Utility"); }
        private void persist() {
            draft.enabled = general.enabled;
            Integer radius=general.parsedRadius();
            draft.radius=radius==null?CritterSafariClient.settings.radius:radius;
            draft.safariOnly = general.safariOnly; draft.hud = general.hud;
            draft.floorDrops=general.floorDrops;
            draft.oneCritterMode=general.oneCritterMode; draft.biomePanels=general.biomePanels;
            // Placement can be edited separately while this editor's draft is still alive.
            draft.biomeX=CritterSafariClient.settings.biomeX.clone();
            draft.biomeY=CritterSafariClient.settings.biomeY.clone();
            draft.biomeScale=CritterSafariClient.settings.biomeScale.clone(); draft.hudScale=CritterSafariClient.settings.hudScale;
            draft.tracers = appearance.tracers; draft.boxes = appearance.boxes;
            draft.halo = appearance.halo; draft.haloStrength = appearance.haloStrength;
            draft.lineWidth = appearance.lineWidth; draft.opacity = appearance.opacity;
            draft.filledBoxes = appearance.filledBoxes; draft.fillOpacity = appearance.fillOpacity;
            draft.smoothingMillis = appearance.smoothing;
            draft.boxStyle = List.of("BOX_3D", "CORNERS", "BOX").get(Math.clamp(appearance.boxStyle, 0, 2));
            draft.origin = appearance.origin == 0 ? "CROSSHAIR" : "BOTTOM";
            draft.aimNames = appearance.aimNames; draft.offscreenIndicators = appearance.edges;
            draft.chromaColor = appearance.color.toLegacyString();
            draft.color = String.format("#%06X", appearance.color.getEffectiveColourRGB() & 0xFFFFFF);
            draft.chromaHitColor = appearance.hitColor.toLegacyString();
            draft.hitColor = String.format("#%06X", appearance.hitColor.getEffectiveColourRGB() & 0xFFFFFF);
            draft.trajectory = trajectory.enabled; draft.showTrajectory = trajectory.showPath;
            draft.predictMotion = trajectory.movingTargets;
            draft.ballSpeed = trajectory.speed; draft.ballGravity = trajectory.gravity;
            draft.ballDrag = trajectory.drag; draft.ballRadius = trajectory.radius;
            draft.hitTolerance=trajectory.hitTolerance; draft.aimCircle=trajectory.aimCircle;
            draft.predictionTicks = (int)trajectory.ticks;
            draft.detection = List.of("BOTH", "NAME", "TYPE").get(Math.clamp(detection.mode, 0, 2));
            draft.names = csv(detection.names); draft.excludedNames = csv(detection.exclusions);
            draft.entityTypes = csv(detection.types);
            CritterSafariClient.apply(draft);
            CritterSafariClient.setConfigKeys(general.toggleKey, general.settingsKey);
        }
        private static List<String> csv(String text) {
            return new ArrayList<>(Arrays.stream(text.split(",")).map(String::strip).filter(s -> !s.isEmpty()).distinct().toList());
        }
    }
    public static final class General {
        @ConfigOption(name = "Enabled", desc = "Highlight loaded nearby critters.") @ConfigEditorBoolean
        public boolean enabled = true;
        @ConfigOption(name="One Critter Mode",desc="Hide a species after your first confirmed capture or loot-share catch this run. Open chat to click a species and override it. New runs reset all species to enabled.") @ConfigEditorBoolean
        public boolean oneCritterMode=false;
        @ConfigOption(name="Biome Panels",desc="Four colored Safari panels with three alphabetical names per row. With a cursor screen open, click white/gray names to toggle species or use Enable all / Disable all. Otherwise only enabled names show. Filters reset each run.") @ConfigEditorBoolean
        public boolean biomePanels=true;
        @ConfigOption(name = "Radius", desc = "Enter a whole number from 1 to 100 blocks. 12 means exactly 12. Invalid input keeps the previous radius.")
        @ConfigEditorText
        public String radius = "100";
        Integer parsedRadius() {
            try { int value=Integer.parseInt(radius.strip()); return value>=1 && value<=100?value:null; }
            catch(NumberFormatException e) { return null; }
        }
        @ConfigOption(name = "Safari Only", desc = "Require Hypixel and the Safari tab Area. Disable for a local test world.") @ConfigEditorBoolean
        public boolean safariOnly = true;
        @ConfigOption(name = "Status HUD", desc = "Show the count and radius when critters are nearby. Hidden when the count is zero.") @ConfigEditorBoolean
        public boolean hud = true;
        @ConfigOption(name="Floor Drop Boxes",desc="Box floor-drop spots confirmed by green particles and three string displays. Separate from critters.") @ConfigEditorBoolean
        public boolean floorDrops=true;
        @ConfigOption(name = "Edit HUD", desc = "Move and resize all five HUDs on one canvas. Drag the badge or a biome header; scroll over any HUD to resize it. Layout persists across runs.")
        @ConfigEditorButton(buttonText = "Edit")
        public transient Runnable moveHud = () -> {};
        @ConfigOption(name = "Toggle Key", desc = "The same toggle binding as Minecraft's Controls menu.") @ConfigEditorKeybind(defaultKey = -1)
        public int toggleKey = -1;
        @ConfigOption(name = "Settings Key", desc = "Open this MoulConfig menu.") @ConfigEditorKeybind(defaultKey = -1)
        public int settingsKey = -1;
    }
    public static final class Appearance {
        @ConfigOption(name = "Tracers", desc = "Draw smooth lines from your crosshair or the bottom of the screen.") @ConfigEditorBoolean
        public boolean tracers = true;
        @ConfigOption(name = "Outline Markers", desc = "Box edges or compact brackets around targets, including invisible helper entities.") @ConfigEditorBoolean
        public boolean boxes = true;
        @ConfigOption(name = "Outline Style", desc = "A 3D box matches the reference. Corners and full frames offer a simpler overlay.")
        @ConfigEditorDropdown(values = {"3D box", "Corners", "Full frame"})
        public int boxStyle;
        @ConfigOption(name = "Filled Boxes", desc = "A light translucent tint inside the 3D tracer box.") @ConfigEditorBoolean
        public boolean filledBoxes = true;
        @ConfigOption(name = "Fill Opacity", desc = "Transparency of the box fill; the critter remains visible inside it.")
        @ConfigEditorSlider(minValue = 0, maxValue = 0.6f, minStep = 0.01f)
        public float fillOpacity = 0.10f;
        @ConfigOption(name = "Normal Color", desc = "Red normally; used for the tracer, box edges and fill. Optional chroma is supported.") @ConfigEditorColour
        public ChromaColour color = ChromaColour.fromStaticRGB(255, 100, 100, 255);
        @ConfigOption(name = "Predicted Hit Color", desc = "Green when the estimated capsule path intersects that critter first.") @ConfigEditorColour
        public ChromaColour hitColor = ChromaColour.fromStaticRGB(100, 255, 100, 255);
        @ConfigOption(name = "Line Width", desc = "Width in physical screen pixels, independent of Minecraft GUI scale.")
        @ConfigEditorSlider(minValue = 0.5f, maxValue = 5, minStep = 0.25f)
        public float lineWidth = 1.25f;
        @ConfigOption(name = "Opacity", desc = "Opacity of tracer lines and outline markers.")
        @ConfigEditorSlider(minValue = 0.1f, maxValue = 1, minStep = 0.05f)
        public float opacity = 0.9f;
        @ConfigOption(name = "Soft Halo", desc = "Add a gentle feathered glow around the crisp line core.") @ConfigEditorBoolean
        public boolean halo = true;
        @ConfigOption(name = "Halo Strength", desc = "Glow intensity. Lower values keep the overlay subtle.")
        @ConfigEditorSlider(minValue = 0, maxValue = 1, minStep = 0.05f)
        public float haloStrength = 0.55f;
        @ConfigOption(name = "Motion Smoothing", desc = "Additional world-position smoothing in milliseconds. Zero uses Minecraft interpolation only.")
        @ConfigEditorSlider(minValue = 0, maxValue = 200, minStep = 5)
        public float smoothing = 70;
        @ConfigOption(name = "Tracer Origin", desc = "Choose where tracers begin.")
        @ConfigEditorDropdown(values = {"Crosshair", "Bottom center"})
        public int origin;
        @ConfigOption(name = "Looked-at Names", desc = "Show species names through walls near your crosshair. The viewing angle widens nearby and narrows at long range, even without a capsule.") @ConfigEditorBoolean
        public boolean aimNames=true;
        @ConfigOption(name = "Edge Indicators", desc = "Small directional chevrons for targets outside your view.") @ConfigEditorBoolean
        public boolean edges = true;
    }
    public static final class Detection {
        @ConfigOption(name = "Match Mode", desc = "Mixed mode learns creature types from named critters and matches unnamed bodies within their biome when unambiguous. Generic models, fish and parrots need per-entity names/models. Raw Types can highlight ambient mobs, NPCs and props.")
        @ConfigEditorDropdown(values = {"Names + creature types", "Names Only", "Raw Types (may show props)"})
        public int mode;
        @ConfigOption(name = "Critter Names", desc = "Comma-separated names. Sparkling prefixes and color codes do not affect matching.") @ConfigEditorText
        public String names = "";
        @ConfigOption(name = "Excluded Names", desc = "Comma-separated species or named props to skip in every mode.") @ConfigEditorText
        public String exclusions = "";
        @ConfigOption(name = "Entity Types", desc = "Comma-separated registry IDs. Remove broad helpers such as minecraft:interaction if they show props.") @ConfigEditorText
        public String types = "";
    }
    public static final class Trajectory {
        @ConfigOption(name = "Hit Prediction", desc = "While holding a Critter Capsule, turn the first predicted critter hit green. This is an estimate, not a capture guarantee.") @ConfigEditorBoolean
        public boolean enabled = true;
        @ConfigOption(name = "Show Trajectory", desc = "Show the estimated curved path and its impact marker. Collision prediction still works with the path hidden.") @ConfigEditorBoolean
        public boolean showPath = true;
        @ConfigOption(name = "Moving Targets", desc = "Extrapolate the critter's current velocity. Sudden turns cannot be predicted.") @ConfigEditorBoolean
        public boolean movingTargets = true;
        @ConfigOption(name="Aim Circle",desc="While holding a capsule, show where to aim for the nearest-to-crosshair reachable critter in front of you. Checks low and high arcs against terrain and entities, with estimated movement and drop.") @ConfigEditorBoolean
        public boolean aimCircle=true;
        @ConfigOption(name="Hit Margin",desc="Extra collision allowance around critters, in blocks. Makes green hit feedback less strict without enlarging terrain or NPC hitboxes.")
        @ConfigEditorSlider(minValue=0,maxValue=2,minStep=0.05f)
        public float hitTolerance=0.5f;
        @ConfigOption(name = "Launch Speed", desc = "Blocks per tick. Adjust against observed throws; default 1.5 is a vanilla throwable estimate.")
        @ConfigEditorSlider(minValue = 0.1f, maxValue = 5, minStep = 0.05f)
        public float speed = 1.5f;
        @ConfigOption(name = "Gravity", desc = "Downward velocity change per tick. Default 0.063 lowers the later arc slightly; tune in steps of 0.001 against observed throws. Server physics remain estimated.")
        @ConfigEditorSlider(minValue = 0, maxValue = 0.2f, minStep = 0.001f)
        public float gravity = 0.063f;
        @ConfigOption(name = "Air Drag", desc = "Velocity retained each tick. Default 1 preserves horizontal speed for a longer, straighter tail.")
        @ConfigEditorSlider(minValue = 0.8f, maxValue = 1, minStep = 0.001f)
        public float drag = 1f;
        @ConfigOption(name = "Ball Radius", desc = "Collision radius in blocks. Increase only if observed capsule collisions have a wider reach.")
        @ConfigEditorSlider(minValue = 0, maxValue = 0.5f, minStep = 0.025f)
        public float radius = 0.125f;
        @ConfigOption(name = "Prediction Length", desc = "Maximum simulated ticks. The path stops at the first collision or unloaded chunk.")
        @ConfigEditorSlider(minValue = 10, maxValue = 160, minStep = 5)
        public float ticks = 80;
    }
}
