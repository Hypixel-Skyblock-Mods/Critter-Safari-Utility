# Critter detection research

Researched on 2026-10-03. These are source-backed presets, not live packet
observations from the user's Hypixel session. No species-to-entity mapping is
invented: the protected mixed preset combines selected creature types with
names and narrowly recognized display models.

## Entity types

[noname-mods/ESP SafariEsp.java](https://github.com/noname-mods/ESP/blob/54d7092e0c3c3e6fe29cc4a441204a61cdd99501/src/main/java/com/esp/core/SafariEsp.java)
publishes a Safari preset containing these 26 registry IDs:

```text
minecraft:armadillo       minecraft:bat
minecraft:bee             minecraft:cave_spider
minecraft:creaking        minecraft:dolphin
minecraft:endermite       minecraft:fox
minecraft:frog            minecraft:glow_squid
minecraft:goat            minecraft:interaction
minecraft:item_display    minecraft:panda
minecraft:parrot          minecraft:phantom
minecraft:player          minecraft:polar_bear
minecraft:ravager         minecraft:shulker
minecraft:silverfish      minecraft:slime
minecraft:sniffer         minecraft:snow_golem
minecraft:tropical_fish   minecraft:warden
```

That source also uses the tab-list `Area:` entry for Safari detection. The
sidebar can show `Safari Zone Entrance` while the player remains in Torrhus
Canyon, so this mod requires the complete Safari area value instead of a broad
substring match against the sidebar.

The user's 2026-10-04 screenshot showed the broad preset selecting Safari
Manager and hundreds of objects. The generic `player`, `item_display`, and
`interaction` IDs do not establish that an entity is a capturable critter.
Version 1.0.1 therefore defaults to known names, including associated nameplates,
and requires a name for these generic models even in mixed mode. Raw type
matching remains an explicit advanced option. Local client tests reproduce
unnamed floating displays, an NPC player, and interaction props alongside named
display/player critters, without connecting to Hypixel.

Version 1.0.2 restores the creature-type fallback by default while retaining
generic-model safeguards. Name-only detection could miss distant creature
models whose supporting labels were not available. Floating nameplates without
a body, trading/shard labels, empty displays, and unrelated NPC players beneath
labels are rejected. Display model transforms determine the visible box;
the entity anchor is not a reliable visible bounding box.

Version 1.0.5 removes unrestricted creature-type fallback from final mixed-mode
markers after the user reported caught critters flashing tracers until their
nameplates appeared, plus ambient tropical fish receiving highlights. Named
observations teach a session-only type/species association keyed by the species'
biome. New unnamed bodies can use an unambiguous learned association, followed
by the ordinary session species filter. Names are not inferred for generic
models, tropical fish or parrots; conflicting same-biome observations disable
inference. These are learned heuristics, not hardcoded vanilla/species mappings.

Biome positions derive from
[SkyHanni-REPO's Safari navigation graph](https://github.com/hannibal002/SkyHanni-REPO/blob/master/constants/island_graphs/SAFARI.json).
The bundled compact samples assign each graph node its nearest area boundary
by weighted graph distance, then resolve an entity's nearest sample. Entrance,
ambiguous boundaries and points over 32 blocks from the graph remain unknown.
The repository's MIT notice is included beside the derived samples in the JAR.
Local synthetic tests verify all four biomes, underground coordinates and
entrance rejection. They do not establish actual species/model associations.

## Floor drops and block-shaped models

[Skyblocker's FloorDrops detector](https://github.com/SkyblockerMod/Skyblocker/blob/main/src/main/java/de/hysky/skyblocker/skyblock/hunting/FloorDrops.java)
identifies loot spots from server `HAPPY_VILLAGER` particles at one block above
exactly three `minecraft:string` item displays. The user's orange floor overlay
and green particles match that signature. This mod draws a block box for the
confirmed spot, keeping floor drops separate from capturable critters. Continued
display presence keeps the box; collection and unloading remove it.

[Skyblocker's SafariGlowAdder](https://github.com/SkyblockerMod/Skyblocker/blob/main/src/main/java/de/hysky/skyblocker/skyblock/entity/glow/adder/SafariGlowAdder.java)
identifies green shulker-box item displays as moving Hideonfloor models and
purple ones as moving Hideonwall models. Those item IDs provide a narrow display
fallback, instead of highlighting every item display. No rendering or glow
implementation was imported; this mod retains its own lightly filled boxes.

## Names

The user reported a named Gazer head receiving no marker. Floating nameplates
now bind against the rendered model's transformed position instead of an item
display's anchor. Head-equipped armor stands can be bodies, rather than always
being discarded as nametag carriers. Names remain mandatory: no Gazer texture
or unnamed head-to-species mapping is assumed. A known different name, a nearer
competing nameplate, or equally plausible head models prevents a wrong binding.
Local client fixtures cover delayed nametags, translated text/head displays,
adjacent Gazer/Gimmiegold heads, head-equipped stands and caught filtering.

[QCloudy-Addition HuntingTextParser.java](https://github.com/northwestcloudy/QCloudy-Addition/blob/dea4507f4ad792b750a605f21a5b072ba537a169/src/main/java/cloudy/autume/addition/hunting/HuntingTextParser.java)
publishes 37 Safari critter names grouped by biome. Its own renderer uses
critter names on real entities for highlights, excluding supporting armor
stands. This mod uses those name facts independently; it does not import that
mod's parsing or rendering implementation.

The additional Wriggleworm name is mentioned in
[the Safari spawn-range discussion](https://hypixel.net/threads/add-the-exact-spawn-ranges-for-all-critter-safari-critters.6140536/).
The older Yubat alias appears in
[the Safari guide's discussion of Flitter](https://hypixel.net/threads/how-to-catch-all-safari-zone-critters.6128398/).
These aliases help match older/custom labels; they are not claims about separate
currently spawning species. The raw list is in `assets/crittersafariesp/catalog.json`.

## Version APIs

Rendering follows the Fabric HUD extraction API and Minecraft camera projection,
with no raw OpenGL calls:

- [Fabric GUI drawing](https://docs.fabricmc.net/develop/rendering/gui-graphics)
- [Fabric rendering concepts](https://docs.fabricmc.net/develop/rendering/basic-concepts)
- [Fabric 26.2 porting changes](https://www.fabricmc.net/2026/06/15/262.html)

The menu uses MoulConfig 4.7.2 with the same `MoulConfigScreenComponent`,
annotation processor, and target-specific dependencies used by the local
SkyHUD/ChatTweaks projects. Filled box faces are perspective-projected GUI
quads submitted through Minecraft's `GuiElementRenderState` pipeline; lines
use floating-point geometry with feathered edges and physical-pixel widths.

Minecraft 26.1.2 and 26.2 have distinct screen/camera accessors. The two
compile-time adapters expose the same contract to the shared code.

## Capsule prediction evidence

[SkyHanni's CritterCapsuleHider](https://github.com/hannibal002/SkyHanni/blob/0913ae5/src/main/java/at/hannibal2/skyhanni/features/hunting/safari/CritterCapsuleHider.kt)
identifies the internal item IDs `CRITTER_CAPSULE` and
`MASTERFUL_CRITTER_CAPSULE`. Flying capsules are `Display.ItemDisplay` entities;
grounded capsules are `ItemEntity` objects. The mod excludes these from critter
detection and activates its aiming preview only for matching held capsules.

This source does **not** define capsule flight speed, gravity, or capture
collision rules. The adjustable initial values use a vanilla throwable model,
verified against the local Minecraft classes. Swept collisions stop at the
first terrain/entity impact, with optional linear target motion. This is a
client estimate requiring live calibration, not server-confirmed capture logic.
Version 1.0.2 increases the default gravity from 0.03 to 0.06 in response to the
user's lower-flight observation, adds a separate critter hit margin, and solves
a moving-target lead circle using the same drag/gravity integration. This
adjustment is not a measured reconstruction of the server's capsule physics.
Version 1.0.3 preserves the early gravity estimate, sets default horizontal drag
to 1, and adds a maximum fall speed of 0.35 blocks/tick to flatten the later
path. These are estimates based on the user's flight observations, not verified
server physics. Version 1.0.4 removes the cap at the user's request: forward
prediction and aim solving now both use continuous gravity and preserve the
configured drag/speed. With drag 1 this is a discrete parabola. Movement lead is
checked against forward integration for stationary, left/right, faster/slower,
approaching/receding and vertically moving synthetic targets.
Version 1.0.5 increases estimated default gravity from 0.060 to 0.063 after the
user reported the real capsule dropping slightly below the later preview. This
is a tuning estimate, not a measured server constant. Both lead arcs and the
forward preview use the same discrete physics; local tests verify high-arc
intercepts over an obstacle that blocks the low arc. Nametags use angular
selection independently of capsule reachability or wall visibility.
The default coral red/lime green colors approximate the tracer cores sampled
from the user's reference image; alpha fading and the halo change displayed pixels.

## Validate new or changed models

The user identified the multipart Cavern critter as **Shyworm**. Its Safari name
and biome are corroborated by
[SkyHanni's SafariShard catalog](https://github.com/hannibal002/SkyHanni/blob/beta/src/main/java/at/hannibal2/skyhanni/features/hunting/safari/SafariShard.kt).
That source does not specify body entity types or ownership metadata. Version
1.0.3 groups adjacent selected models matching the named head's type, with
separate nameplate heads protected. Local synthetic silverfish chains exercise
the grouping algorithm; they do not establish the live Shyworm entity mapping.
Ordinary overlapping helper/model pairs likewise use one larger box, without
merging different named species or separate nameplate heads.

1. In Safari, run `/csu dump` and inspect nearby entity types and names.
2. Compare markers with capturable critters and note any false positives.
3. Add missing names/types in the settings or JSON. Put false-positive names in
   `excludedNames`; disable broad type fallbacks when they mostly identify props.
4. Update the bundled catalog only after evidence identifies a new model.

Neither this research nor a successful build proves complete live Safari
coverage. Client-side ESP cannot show entities not sent by the server.

## Species panels and confirmed catches

The 37 species and four biome assignments come from
[SkyHanni's SafariShard enum](https://github.com/hannibal002/SkyHanni/blob/beta/src/main/java/at/hannibal2/skyhanni/features/hunting/safari/SafariShard.kt).
The UI sorts names by descending shard rarity, alphabetically within each tier,
and keeps three columns reading left to right. Rarities derive from the
`ATTRIBUTE_SHARD_*;1.json` shard lore in
[NotEnoughUpdates-REPO](https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO/tree/master/items),
cross-checked for all 37 species. For example,
[Honeybug's shard](https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO/blob/master/items/ATTRIBUTE_SHARD_VISITOR_HONEY%3B1.json)
is Uncommon, and
[Doomspiral's shard](https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO/blob/master/items/ATTRIBUTE_SHARD_ECHO_OF_TRACKING%3B1.json)
is Legendary. Standard Minecraft/SkyBlock rarity colors are white, green, blue,
purple and gold; disabled text halves each RGB channel. Named/recognized grouped models are filtered
as a single logical target; identified species are remembered for that same
live entity if its nameplate temporarily disappears. Removed references and
new worlds clear that cache. Unambiguous observed type/biome associations can
identify anonymous bodies; unidentified bodies receive no default-mode marker.

## Special Safari interactions

[SkyHanni's HideyhoFinder](https://github.com/hannibal002/SkyHanni/blob/beta/src/main/java/at/hannibal2/skyhanni/features/hunting/safari/HideyhoFinder.kt)
identifies a RemotePlayer by the HIDEYHO skin in
[SkyHanni-REPO's Skulls.json](https://github.com/hannibal002/SkyHanni-REPO/blob/master/constants/Skulls.json).
The exact published Minecraft texture URL identifies unnamed Hideyho models
locally; teleporting follows the current loaded model. Scrappy supports a strict
species nameplate attached to an otherwise unnamed player profile. Other named
merchants and shard/trade/click labels remain excluded. Hideyho and Scrappy get
purple interaction markers and no capsule aim circles.

[Skyblocker's SafariGlowAdder](https://github.com/SkyblockerMod/Skyblocker/blob/main/src/main/java/de/hysky/skyblocker/skyblock/entity/glow/adder/SafariGlowAdder.java)
and its
[Rockmite mound texture constant](https://github.com/SkyblockerMod/Skyblocker/blob/main/src/main/java/de/hysky/skyblocker/skyblock/item/HeadTextures.java)
provide the mound's exact head texture, Cavern gate and zero movement duration.
The duration check prevents capture-animation models from qualifying. This mod
uses independent amber boxes/tracers, not the source mod's glow. Mounds disappear
after Rockmite is caught/loot-shared, reset on a new run and respect manual
Rockmite disabling. The same published detector documents green Forest shulkers,
purple Haunted shulkers, Haunted bats and Duplico's three-tick item display
interpolation excluding player heads and purple shulker boxes. These are model
heuristics, not server-confirmed critter identity.

The remaining special interactions are catalogued from
[the community Safari guide](https://hypixel.net/threads/how-to-catch-all-safari-zone-critters.6128398/).
Quest objects are not automatically assumed to be capturable targets. No
unverified wall, feeder, gem podium or ritual object signatures are fabricated.

Personal captures and Safari loot-share formats are corroborated by the examples
in [SkyHanni's AttributeShardsData](https://github.com/hannibal002/SkyHanni/blob/beta/src/main/java/at/hannibal2/skyhanni/features/inventory/attribute/AttributeShardsData.kt),
including multi-shard catches, Hideyho's found/reward variant and loot shares
from another player catching a named Safari critter. SkyHanni accepts a general
`from ...` ending for loot shares and publishes a shard-gain event; its
[Safari checklist](https://github.com/hannibal002/SkyHanni/blob/beta/src/main/java/at/hannibal2/skyhanni/features/hunting/safari/SafariShardChecklist.kt)
consumes those hunt/capture events. The user's Prism chat log additionally
confirms Hideyho's teammate variant: `LOOT SHARE! You received 2x Hideyho Shard
from <teammate> finding the Hideyho!`. CSU accepts that finding variant alongside
ordinary catching messages. The logged personal capture and own Safari entry
formats are also supported.
Our independent parser requires a known critter and matching shard name. String
counts do not multiply catches; One Critter Mode records unique species.
Fabric's system-message allow hook observes these messages before its default
phase, so a normal chat-hiding listener cannot prevent capture tracking. The
observer returns true without suppressing messages. Ordinary player chat,
action-bar messages, floor drops, failed catches,
non-Safari assisting rewards and mismatched shard names are ignored.

Run state is in memory and resets with a new client world, entering the Safari
area, or the player's own entry announcement. Global ESP toggling, radius edits,
chat opening, panel resizing and other players entering do not reset it. Catches
are recorded with the mode off, but only hide species automatically when the
mode is enabled. Manual re-enables override the caught filter until another
confirmed catch of that species or the next run. Layout preferences alone persist.
