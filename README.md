# Critter Safari Utility (CSU)

Client-only Fabric mod for Minecraft **26.1.2**, **26.2** and **26.3**. Draws thin red/green
tracers with a soft halo and lightly filled 3D bounding boxes.
Uses **MoulConfig** for settings. Default radius: **100 blocks**, measured
as a sphere around the player.

Use `/csu` to open CSU settings. The previous `/critteresp` command remains an
alias. The existing Fabric ID, resource namespace and config filename are kept
for compatibility, so replacing the old JAR preserves settings and keybinds.

## Install and use

1. Use Fabric Loader **0.19.3 or newer**, Java **25**, Fabric API for your
   Minecraft version, and **Fabric Language Kotlin 1.13.12+kotlin.2.4.0 or newer**.
   MoulConfig 4.7.2 is bundled with each JAR.
2. Put the matching `CritterSafariUtility-1.0.8+mc<version>.jar` in the instance's
   `mods` folder. Install **one** version of this mod per instance.
3. Enter the Critter Safari. The default area gate requires a Hypixel server
   address and the tab list's `Area: Safari` entry.
4. Run `/csu` for settings. Keybinds for opening settings and toggling ESP
   are available in **Options → Controls → Key Binds → Critter Safari Utility**;
   both start unbound to avoid colliding with your existing mods.

The standard MoulConfig screen has searchable **General**, **Appearance**,
**Detection**, and **Trajectory** categories, with native toggles, sliders, keybind editors and a
color picker. Changes are saved by the editor and when you close it. Radius
edits also apply and save immediately while the menu is open. Radius uses an
exact integer field from **1 to 100**; select its text and enter `12` for 12
blocks. Empty, non-integer, or out-of-range input keeps the previous radius.
Box outlines, translucent fill, tracers, halo, aim-target nametags, edge indicators,
and the status HUD can be switched independently. The tracer origin can be
the crosshair or the bottom center of the screen. Radius, line width, opacity,
fill opacity, halo strength, motion smoothing, color, critter names, entity
registry IDs, and exclusions are editable. The color picker supports chroma.

Under **General → Edit HUD**, click **Edit** to position all five HUDs together.
Drag the status badge or a biome header, and scroll over any HUD to resize it
independently. Press **Done** or **Esc** to save. **Reset** restores the complete
default layout and sizes. Positions adapt to window size and GUI scale. During play the badge is
completely hidden when no critters or floor drops are detected, with no waiting message.

**Biome Panels** adds separate Cavern, Forest, Haunted and Icy panels with amber,
green, purple and cyan side accents. Species appear by descending shard rarity in three
columns reading left to right, with alphabetical order inside a rarity tier.
Names use SkyBlock rarity colors; disabled names use a darker shade of that color.
With chat closed, only enabled species appear. Open chat or another
cursor screen to show all species: click names to enable/disable them,
or use each panel's **Enable all** / **Disable all** controls. Headers can also be
dragged with a cursor screen open. The MoulConfig screen has a separate panel
visibility toggle; editing uses the same **Edit HUD** canvas as the status badge.

**One Critter Mode** is an optional General toggle, off by default. While enabled,
a confirmed personal capture or Safari loot-share catch hides that species from
ESP, the count and aim selection for the rest of the run. Floor drops, failed
throws and ordinary chat do not count. The biome header counts unique caught
species. Manual clicks or Enable all can restore a caught species; its next
confirmed catch hides it again while the mode is on. With the mode off, catches
never automatically disable species. New worlds, entering the Safari area and
your own server entry message reset run filters/catches to all enabled; another
player's entry does not. Panel positions/sizes and the mode preference persist.
Species filters require an identified critter name or recognized species model;
an anonymous vanilla-type fallback cannot reliably be assigned to a species.

**Floor Drop Boxes** marks drop locations: green happy-villager particles must
confirm exactly three string
item displays in one block. These get a lightly filled amber (`#FFBF40`) block box and a separate
`Drops` count, without extra tracers. Boxes disappear when the displays are
collected or unloaded. The same radius and Safari gate apply.

Critter boxes, fill, and tracers are normally coral red (`#FF6464`). While holding a
**Critter Capsule** or **Masterful Critter Capsule**, the estimated throw path
is displayed. The first critter it intersects turns lime green (`#64FF64`),
along with its tracer, box fill and edges. Both colors have native MoulConfig
pickers. A small diamond marks the estimated impact. The path stops at the
first solid block, entity, or unloaded chunk. Hiding the path keeps hit feedback
active; disabling **Hit Prediction** returns all targets to the normal color.

**Aim Circle** shows a small circle for the reachable critter nearest your
crosshair in front of you, including stationary critters. There is no fixed
30-degree cutoff. Aim at the circle; its position accounts for estimated travel
time, gravity, drag, and current movement speed and direction. The collision
check tries the low arc first, then a high arc if the low arc is blocked. A circle
appears only when the estimated path hits that critter before terrain or another
entity; the projected aim point must be on screen. Turning off **Moving Targets**
still shows the circle using a stationary estimate.

**Looked-at Names** independently displays species names above boxes near your
crosshair, through walls and even without holding a capsule. Its viewing
half-angle decreases smoothly with distance: up to 40 degrees nearby, about 22
degrees at 10 blocks and 8 degrees at 50 blocks. Wall obstruction can hide an aim
circle while its nametag remains visible. Neither feature changes player aim or
throws a capsule.
**Hit Margin** adds a configurable allowance around critters only (default
0.5 blocks), making hit feedback more forgiving while keeping NPC and terrain
collision separate.

The prediction uses swept collision checks, current target velocity, and
adjustable speed, gravity, drag, radius and simulation length. Defaults are
a configurable estimate: 1.5 blocks/tick, gravity 0.063, drag 1, radius
0.125. Gravity continues throughout flight: with drag 1, the path follows a
discrete parabola, with no straight terminal tail or fall-speed cap. Both the
forward preview and lead circle use this same model. It has not been calibrated
against recorded live throws. Hypixel sends flying capsules as custom item displays, and no published
source verified their exact server physics. Tune these values against observed
throws. Sudden target turns, latency, underwater behavior, and server-specific
collision/capture rules can differ. **Green means a predicted collision, not
a guaranteed capture.** The preview does not aim, throw, or move the player.

To calibrate, raise **Gravity** in steps of 0.001 if the real ball falls below the
preview, or lower it if it stays above. Keep **Air Drag** at 1 for a parabola and
adjust **Launch Speed** for horizontal travel. Matching several throws at
different angles/distances is stronger evidence than matching one throw.

Default styling uses a thin **1.25 physical-pixel** core, feathered edges, a
restrained halo, **10%** box fill, and **70 ms** of world-position smoothing.
Camera movement remains immediate. A 3D box matches the reference; corner
brackets and full screen-space frames are optional alternatives. Line width
does not grow when you increase Minecraft's GUI scale.

Commands:

| Command | Action |
| --- | --- |
| `/csu` | Open settings |
| `/csu toggle` | Enable/disable and save |
| `/csu reload` | Reload the JSON config from disk |
| `/csu dump` | Save nearby loaded entity types, names and positions for detection diagnostics |

## Detection and limitations

The bundled catalog contains **26 entity types and 39 name/alias patterns**,
researched from published mod source. **Names + creature types (`BOTH`)** is
the default. It accepts known critter names and recognized shulker-box display
models, then learns creature-type/species associations from named bodies during
the current world session. A fresh unnamed body can match a learned type only
inside that species' biome, determined from the bundled Safari area graph.
If two species share that type in the same biome, automatic inference stops.
Generic models, tropical fish and Forest parrots always need a per-entity name
or recognized model: broad type/biome matching cannot reliably distinguish them.
Unidentified models get no standalone marker while their names are unavailable.
Anonymous parts can still join a named Shyworm or overlapping model/helper pair.
**Names Only (`NAME`)** uses known names or recognized models. In mixed mode,
generic `player`, `item_display`, and `interaction` models still require
a critter name or a recognized model. **Raw Types (`TYPE`)** is an advanced opt-in fallback that
can highlight NPCs and decorative props. Type selection controls the fallback;
named matches still apply in `BOTH`. Inferred names go through the same caught
and manual species filters immediately, so a hidden species cannot flash a
tracer while waiting for its fresh body's nameplate. Learned associations clear
on a world change; they are observational heuristics, not verified catalog
assignments. Do not select Raw Types when avoiding ambient mobs is a priority.
To hide a named species entirely, put its name in the exclusions list.

Named armor stands/text displays are associated with the visible model beneath
them, including transformed head displays and armor stands carrying head-slot
equipment. A stand with its own critter name and head model is itself a target.
Head models require a known name first; neighboring heads cannot inherit a
different critter's name, and ambiguous pairs wait for a clear association.
Each identified body receives one marker, with ordinary caught/manual filters.
Orphan nameplates get no marker; shard/trade/click
labels are ignored. Generic player NPCs still require a named body except for
strict Scrappy/Hideyho nameplate matches on otherwise unnamed profiles. Hideyho's
published skin also identifies it without a nameplate, including after a
teleport. Display boxes use the model's
translation, rotation, and scale instead of its zero-sized entity anchor.
Empty display models are skipped. Your player and players listed in the tab list are excluded.
Unidentified player models require Raw Types to be selected.

Hideyho and Scrappy use purple interaction markers and do not offer capsule aim
circles. Hideyho's tracer follows its current loaded NPC model. Rockmite mounds
use an amber box/tracer, identified by their exact head texture, Cavern location
and zero movement-interpolation duration. Animated capture displays do not
qualify. Mounds hide after a confirmed Rockmite catch or loot share even when
One Critter Mode is off, and reset on the next run; actual Rockmites follow the
ordinary One Critter Mode/manual filters. Manually disabling Rockmite also hides
its mounds. Known shulker color/biome signatures cover hidden Hideonfloor and
Hideonwall bodies; the published Haunted bat and Duplico animation signatures
provide narrow additional model heuristics.

Overlapping critter/helper pairs get one marker using the larger model's box.
Separate nameplate heads and different named species are kept distinct. A named
**Shyworm** groups adjacent matching body segments into one enclosing box, one
tracer, and one count entry. Its individual parts still participate in capsule
collision checks. Grouping uses local model positions and labels; it cannot
prove server ownership when unrelated anonymous models overlap.

Version 1.0.2 upgrades unchanged legacy presets to the protected mixed mode,
backing up the original JSON as `crittersafariesp.json.before-model-fix`.
Custom legacy filters are preserved. Generic NPC, floating item display, and
interaction types do not qualify on type alone. The catalog is a practical
preset, **not a verified one-to-one mapping of every Safari species**. See
[the source notes](docs/detection-sources.md).

Only entities loaded by the client can be detected. Increasing radius cannot
reveal server-hidden, unspawned, or unloaded critters. The fill tints the box
around the critter; the soft halo follows tracer lines and box edges. Invisible
interaction entities can be located using the box. The mod adds local visuals
and has no movement/capture automation.

To test in a local world, disable **Safari Only** in General. This also allows
name-based detection to run outside Hypixel. Name an entity after a catalog
critter (for example, `Honeybug`) to exercise the default filter.

## Advanced settings

The instance's `config/crittersafariesp.json` persists all options. The dump is
written to `config/crittersafariesp/entity-dump.json`. If the config is malformed,
its original contents are backed up as `crittersafariesp.json.invalid-<timestamp>`
before default settings are restored.

Optional per-type normal colors can be set in JSON, then loaded with `/csu reload`.
The predicted-hit color takes precedence for the selected hit target:

```json
"typeColors": {
  "minecraft:bee": "#FFFF00",
  "minecraft:warden": "#FF4080"
}
```

`scanIntervalTicks` defaults to 5 (four scans per second at 20 TPS). Radius is
limited to whole numbers from 1–100 blocks, width to 0.5–5 physical screen pixels, line opacity to
0.1–1, fill opacity to 0–0.6, and motion smoothing to 0–200 ms. Zero smoothing
uses Minecraft's own interpolation without an extra filter. HUD tracers/boxes
render through walls and stay visible during chat. Other menus hide the overlay.
Off-screen edge indicators are optional and start disabled. Teleports and world
changes reset the smoothing cache rather than sliding a marker across the map.

Overlay geometry is batched into one GUI submission per frame, preserving the
halo, feathered edges, and position interpolation. Motion sampling covers selected
models; confirmed floor drops retain their three display references instead of
rescanning every loaded entity each frame. The path and aim circle share one
collision scene, and collision checks reuse expanded bounds. Rendering is not
throttled to a lower update rate. A local 81-critter test draws 15,820 quads in
one geometry submission; this verifies reduced submission work, not a live FPS gain.

## Build

On Windows with Java 25:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot'
.\gradlew.bat build releaseManifest
```

Both targets are built from `gradle/targets.properties`. Production JARs:

```text
versions/mc26_1_2/build/libs/CritterSafariUtility-1.0.8+mc26.1.2.jar
versions/mc26_2/build/libs/CritterSafariUtility-1.0.8+mc26.2.jar
```

Source JARs are also generated. Shared behavior lives under `src/main/java`;
only camera/screen API differences live under `src/26.1.2/java` and
`src/26.2/java`. MoulConfig uses the target-specific
`modern-26.1` / `modern-26.2` artifacts.

Unit tests cover area/domain matching, name boundaries, spherical range,
projection, frame-rate-independent smoothing, HUD placement after a resize,
nearest projectile hits, wall occlusion, moving targets, gravity/drag, and
config persistence/recovery. A separate client game test boots a temporary
local world, checks that ESP does not add entity glow, rejects flying props and
NPCs and orphan/trade nameplates, detects named display/player critters,
rejects unnamed ambient tropical fish while detecting a named fish critter,
resolves an unnamed Forest creature by an observed type/biome match, checks its
immediate caught filtering and manual restoration, and types
12 into the native MoulConfig radius field, verifies exact saving/reopening and
increasing it to 100, checks chat visibility, translated display bounds,
particle-confirmed floor drops and their collection cleanup, capsule hit/miss
previews and stationary/moving aim circles (including targets outside 30 degrees,
blocked paths and disabled motion prediction), verifies distant nametags without a
capsule and through a solid wall, exercises MoulConfig categories and HUD dragging/persistence, removes a target,
groups Shyworm segments and a duplicate Flitter/helper pair, renders an 81-critter
scene with one geometry submission,
checks capture and loot-share messages through Fabric's real message callback,
per-species clicks and biome bulk controls through native screen input events,
own/other entry messages, run resets, and unified HUD dragging/scroll resizing,
and checks that disabling ESP clears the selection. It captures screenshots
under each target's `build/run/clientGameTest/screenshots` directory. The test
mod is excluded from production JARs.

Run the client tests separately for each target:

```powershell
.\gradlew.bat :versions:mc26_1_2:runClientGameTest
.\gradlew.bat :versions:mc26_2:runClientGameTest
```

Client test screenshots cover tracer/halo/fill rendering, MoulConfig categories,
HUD placement, and the empty scene without a HUD badge. Live Hypixel entity
coverage still needs verification in an actual
Safari instance.

Minecraft 26.3 builds bundle the official MoulConfig source port; see [THIRD_PARTY.md](THIRD_PARTY.md) for the pinned revision and JDK 8/25 build setup.
