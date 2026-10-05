# CritterSafariUtility agent guidance

Client-only Java/Fabric mod for Minecraft 26.1.2, 26.2 and 26.3, built with Java 25 and
Fabric Loom. Keep changes scoped and preserve the user's existing work.

- `gradle/targets.properties` defines all supported targets and dependencies.
- Keep shared behavior under `src/main/java`; only compile-time API differences
  belong under `src/<minecraft-version>/java`, with matching contracts.
- Use MoulConfig 4.7.2's target-specific modern artifact for settings, following
  the neighboring SkyHUD/ChatTweaks integrations. Do not substitute a custom UI.
- Preserve thin pixel-based strokes, smooth motion, and separately adjustable
  box fill/halo opacity. Submit geometry through Minecraft's rendering pipeline.
- All targets share `mod_version`. Build every target with `.\gradlew.bat build`
  on Windows or `./gradlew build` elsewhere; fix failures before completion.
- Run `releaseManifest` after target catalog changes and verify its JAR paths.
- Highlight boxes with a light translucent fill. Do not add entity silhouette
  glow or mutate server metadata, player movement or entity interaction.
- Normal targets are red; only the first predicted capsule collision is green.
  Keep trajectory physics configurable and label the result as an estimate.
- Keep the default Safari area gate narrow. Do not match Safari entrance text
  as an active Safari instance.
- Treat detection presets as heuristics. Document evidence for catalog updates
  in `docs/detection-sources.md`; do not invent per-species entity mappings.
- Default to selected creature types and known names. Mixed mode must require
  names or recognized models for generic players/displays/interactions;
  raw type matching is opt-in. Do not mark orphan or trading nameplates.
- Default-mode anonymous creature candidates must resolve a learned type/biome
  association before receiving standalone markers and caught-species filtering.
  Reject ambiguous mappings; never infer generic models, ambient tropical fish
  or Forest parrots by type alone. Keep nameless parts available for grouping.
- Radius edits must apply while settings are open, and range shrinking must
  remove distant targets immediately. Exercise the native input in local tests.
- Radius is an exact integer from 1 to 100. Keep ESP visible during chat.
- Floor-drop boxes require server particles and the three-string-display
  signature; use amber boxes and keep them separate from critter targets and hit prediction.
- Select the reachable stationary/moving critter nearest the crosshair in front
  of the player; no fixed 30-degree gate. Check low/high arcs for obstruction.
- Account for display model transforms. Aim circles are visual estimates;
  keep lead solving and the forward trajectory on identical discrete physics.
- Group Shyworm segments and overlapping helper/model pairs into one marker.
  Preserve distinct nameplate heads; use the larger box for ordinary pairs.
- Batch overlay geometry without reducing render/interpolation cadence. Share
  collision preparation between the forward path and aim circle.
- Capsule previews have no player-motion inheritance or fall-speed-cap option.
  Apply gravity throughout the flight; drag 1 gives a discrete parabola. Lead
  prediction and forward integration must agree for speed/direction changes.
- One Critter Mode hides confirmed caught/loot-shared species only when enabled.
  Reset session filters/catches on a new run, never on global ESP/radius edits.
- Keep four colored biome panels, rarity-descending three-column species grids
  reading left to right, with alphabetical order inside each rarity. Use standard
  SkyBlock rarity colors, dimmed to half RGB when disabled. Closed chat shows enabled species only;
  cursor screens show all species and accept toggles/bulk actions.
- One Edit HUD canvas moves/resizes the status badge and all four biome panels.
  Hover scrolling resizes independently; saved layouts must survive Moul saves.
- Show ESP nametags through walls independently of capsule/aim-circle visibility.
  Use a distance-dependent angular cone: wider nearby, narrower at long range.
- Commit task changes locally at stable verified checkpoints. Do not push,
  publish, or install into the user's Minecraft instance unless requested.
- Do not commit build products, run directories, IDE files or credentials.
