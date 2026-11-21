## Unreleased - Forge source recovery

- Register Atlantis blocks, items and block entities under `stargate_atlantis`,
  retaining legacy saved-ID remapping and bundled structure compatibility.
- Fix a structure-migration mixin visibility error that crashed the world list.
- Repair two maintenance-area floor conduits and their adjacent cable connection
  in the bundled city.
- Recheck loaded floor-conduit neighbors and safely rebuild missing SGJourney
  cable networks, saving changed IDs without forcing chunks to load.
- Keep mixed-network simulation results specific to the active route and
  discard them after committed transfers. Preserve the energy type when
  probing SGJourney cable outputs.
- Start client door animations when the update arrives, even if the door is
  offscreen, rather than delaying the animation until it is next rendered.
- Show the Genetics Lab and Lower Spire under the transporter's LOWER level,
  alongside the underwater bay, without changing unrelated destinations.
- Bundle the server-backup city and lower rooms with the later Farlands
  control-room layout, without copying its temporary open hatch and flooding.
- Reconstruct the Atlantis DHD entity from the accepted binary and integrate
  its DHD-specific compatibility mixins into the readable entity source.
- Move the DHD crystal guard into entity overrides. Recover the sneak-use event
  source and move its position fix into that event.
- Integrate screen behavior and the iris compatibility handler into their
  target classes, and register jumper configuration through a Forge event
  subscriber instead of modifying the mod's bootstrap class.
- Keep the legacy inventory format while compiling all packaged mod classes
  from source; remove the frozen Atlantis binary dependency and release merge.
- Reconstruct missing bootstrap, block, menu and client-helper source and
  restore missing release resources. These files are reconstructed source,
  not the original author's source.
- Replace home-directory JEI and Lantea defaults with Maven dependencies.
  Keep the author's private SGJourney alpha as an explicitly supplied input.
- Check that every packaged owned class has corresponding Java source.
- Third-party mixins and a compatibility coremod remain.

## 2.0.5-dev-144-restore-original-hatch

- Restore the original powered hatch table and installer location, remove the
  redundant working table, and keep the decorative table separate.

## 2.0.5-dev-143-hatch-power-feed

- Reconnect the relocated hatch console's floor conduits to the bundled ZPM
  network without changing the table's power cost or the decorative console.

## 2.0.5-dev-142-console-legs-hatch-charge

- Render the decorative console's legs and restore the active bundled hatch
  table's starter charge at its new control-room location.

## 2.0.5-dev-141-decorative-console

- Replace the extra spire roof table with a placeable decorative console.
  Leave the working control-room hatch table in place.

## 2.0.5-dev-140-farlands-spire

- Refresh the bundled gate and control rooms from Farlands, including the
  recessed database console and the relocated DHD, shield and hatch controls.

## 2.0.5-dev-139-recessed-database-console

- Angle the front of the Database Console base inward to clear a player's
  approach to both stations. Keep the rear contour and aligned tabletop controls.

## 2.0.5-dev-138-aligned-database-stations

- Rotate each Database Console station as a unit so its tabletop, keypads,
  and display panels stay square. Keep the approved base and worktop height.

## 2.0.5-dev-137-contoured-database-console

- Reshape the Database Console around a clipped front and pointed rear, with
  matching base collision and mirrored stations at the existing worktop height.

## 2.0.5-dev-136-database-console

- Add a decorative Atlantis Database Console with a 5 x 3 footprint, a
  one-block plinth, and a translucent placeholder display. Place it by
  right-clicking the floor with the console item from the Atlantis creative tab.

## 2.0.5-dev-134-native-iris-transition

- Animate the native Lantea energy iris in the world and on the Atlantis
  dialer. Both use the same opening and closing timing without changing
  when the iris blocks transit.

## 2.0.5-dev-133-native-preview-lighting

- Restore the GUI cutout pass for native mini-gate lights so the brightness
  change does not replace the visible animation with an invisible shader pass.
- Use flat GUI lighting for the miniature ring to reduce directional shading.

## 2.0.5-dev-132-native-preview-glow

- Brighten the native Lantea mini-gate's glyphs and chevrons. The travelling
  glyph and leading edge of the incoming sweep glow white against cyan lock lights.

## 2.0.5-dev-131-native-ring-animation

- Animate the Forge native Lantea preview with Pegasus travelling glyphs and
  lock lights, and Milky Way rotor and latch motion.
- Size the iris control and its circular click area to the native ring opening.

## 2.0.5-dev-130-native-preview-alphabet

- Disable physical keypad positions outside the selected native gate's alphabet.
- Draw the complete native gate models and live chevron lights in the preview
  instead of using the chunk-dependent world renderer.

## 2.0.5-dev-129-native-mini-gate

- Render the native Lantea gate selected by the Forge Atlantis table instead
  of a fixed gate image. The iris button refers to the same controller.
- The preview protocol changed; use matching client and server builds.

## 2.0.5-dev-128-native-dhd-clear

- On the native Lantea Atlantis table, press the centre diamond to clear a
  partial address without closing the dialer. A complete address still dials.

## 2.0.5-dev-127-native-table-glyphs

- Sync the table's crystal-adjusted range to the client and use it to find
  assembled native Lantea gates for physical DHD glyphs, even while idle.
- Restore the origin symbol for native Lantea tables.

## 2.0.5-dev-126-native-dhd-routing

- Keep the Stargate Journey dialer for a table actually linked to a live
  Stargate Journey gate, not merely one with a nearby gate or stale controller.
  Log why a table without a valid link falls back instead of opening the
  native Lantea dialer.

## 2.0.5-dev-125-city-cover-cleanup

- Remove invisible air cover entries from the bundled Pegasus gate so newly
  installed gates do not require breaking nonexistent covers before removal.

## 2.0.5 - SGJourney 0.6.55-alpha

- Revalidate the replacement Pegasus gate in SGJourney's registries after a city rebuild instead of trusting the old gate's registration flag. Retain the pre-rebuild address when available and refuse to claim an address owned by another gate.
- Report gate registration and the two-way city table link separately at the end of the rebuild.
- Remove the pre-saved Destiny address from the bundled city table's recent history without changing the other entries or Destiny dialing.
- Retry a failed live gate lookup once the bundled Pegasus gate has a matching
  registry entry. The existing city DHD recovery can then restore a one-sided link.
- Initialize the bundled Pegasus gate's DHD callbacks after loading its saved
  registry entry. Repair missing or one-sided links when the pair is within
  range and neither side belongs to another device; synchronize symbols after linking.
- Log why the city table link recovery cannot run, without changing ownership.

- Refresh the bundled Forge Atlantis city with Arg's new rooms and fixtures.
  Keep the existing bundled gate, DHD, shield, and ZPM hub settings.
- Start the third bundled ZPM at 93% entropy instead of 99.9%; the other
  two remain depleted.
- Register the additional bundled transporter stations, including Genetics Lab
  and Lower Spire, in the city transporter directory.
- Refresh a stale cached city gate controller after reinstall and during normal
  city ticks, only when its saved registry identity matches the physical gate.
- Let the Forge Atlantis paste guide accept six to eight destination symbols
  without an explicit point of origin. It adds `0` to the guide, not to the gate.
- Draw the Forge mini-gate's active chevron lights after the base chevrons,
  even with Oculus installed. The world rendering order is unchanged.
- Keep the Forge mini-gate on the linked gate's live animation during brief
  link updates and stop substituting the stock Pegasus ring when no SGJourney
  gate is available.
- Render the linked gate's live model, chevrons, and glyphs on the Forge
  Atlantis dialer, including MoreGates variants loaded by Stargate Journey.
- Animate the Milky Way mechanical iris when toggled from the Forge Atlantis
  table, with a mechanical opening or closing cue.
- Clear a partial Atlantis table address on center press without interrupting
  a complete address that is still locking.
- Fix the Forge table iris-button crash by invoking its registered action
  directly instead of looking up an obsolete synthetic screen method.
- Fix the Forge Atlantis connect button crash caused by an obsolete screen
  transformer calling a removed SGJourney method.
- Keep the Atlantis dialer open and defer connection until the Stargate has
  locked the entered address, rather than resetting an in-progress dial.
- Migrate the Forge Atlantis dialer screen to the current DHD menu and packet
  APIs while preserving the existing table controls.
- Recalculate crystal effects using the 0.6.55 energy, network, and range
  fields instead of removed legacy fields.
- Restore memory-crystal dial-attempt recording and forward the DHD's
  auto-close policy through its API adapter.
- Apply crystal effects and memory recording in every Atlantis inventory slot,
  accept call-forwarding devices, and avoid replacing occupied slots when
  stocking the bundled city DHD.
- Refreshed city completeness still needs in-game verification.

## 2.0.5-dev-102 - SGJourney 0.6.55-alpha

- Fix the DHD crystal guard's mixin callbacks for SGJourney's return-valued
  dialing methods, which prevented client and server startup.

## 2.0.5-dev-101 — SGJourney 0.6.55-alpha

- Migrate the Forge DHD binary baseline to SGJourney's new API through a
  focused `BlockEntityDHD` adapter mixin.
- Resolve linked gate state through `DHDControlledStargate`, preserving the
  existing rendering, shield, and dial-cancel behavior.

# 2.0.5

- Removes the legacy More Gates 4.3.2 compatibility bridge, its config option,
  and recipe overrides now that More Gates has been updated by its maintainer.
- Prevents repeat city repairs from replacing the existing Atlantis Stargate.
  On load, removes stale SGJourney addresses left by earlier repairs while
  restoring the original address captured with the bundled city gate.
- Adds `/sgjatlantis city install` for operators to install or repair the bundled
  city in two-chunk batches from any dimension or the server console, plus
  `/sgjatlantis city status` for progress.
- Registers the bundled Pegasus Stargate with SGJourney after structure placement
  and repairs its network entry when an existing city's gate chunks load.
- Repairs partially generated Atlantis chunks by reapplying every captured city
  block during `/sgjatlantis city install`; unrelated positions are left alone and
  existing puddle jumpers are not duplicated.
- Fixes jumper-to-SGJourney dialing by passing the required Point of Origin 0
  to SGJourney's instant-dial operation. Typing the final 0 is optional.
- Refreshes the bundled Atlantis city from the current ATM9 Stargates `world`
  save: 2,009 chunks and 2,266,341 exact placed blocks. Ordinary stone, clay
  and dirt used in city walls and floors are retained at city height, while
  environmental snow blocks and thin snow layers are excluded. The bundle
  includes all three placed control consoles, both complete
  roof doors and weather seals, 36 saved block entities and both parked puddle
  jumpers. The installer preserves the door and table states in the bundle.
- Adds optional Lantea 0.7.2.0 jumper DHD compatibility: nearby SGJourney gates can be dialed and disconnected from the cockpit, retaining Lantea's gene/crystal checks and SGJourney's power rules. Autopilot and jumper transit are not changed.
- Replaces the jumper's numeric SGJourney prompt with a compact Atlantis triangle dialer and animated miniature gate, rotated 180 degrees to match the cockpit panel. The selected and encoded symbols light up as the gate dials.
- Mirrors the inverted jumper dialer so its upright miniature gate sits at the upper right, restores upright constellation glyphs by default and numbers while Shift is held, and illuminates selected triangles in gold. The miniature gate's center is an explicit Dial/Close control.
- Starts SGJourney's staged glyph engagement immediately, then lets the gate validate the address at final engagement, so unsuccessful addresses still visibly dial and report their failure.
- Keeps Lantea's original screen for native Lantea gates by default. A server config option enables the Atlantis screen there while native gate validation and dialing remain Lantea's own.
- Changes jumper-to-SGJourney dialing from an instant connection to SGJourney's
  normal staged Pegasus sequence, including visible chevron engagement and the
  normal event-horizon opening effect.
- Illuminates the matching physical jumper glyphs as numeric symbols are entered.
  SGJourney's duplicate Pegasus Robandus slot is mapped to Lantea's Robandus key;
  SGJourney-only extended symbols remain unlit because the jumper has no matching
  physical keys.
- Makes an Atlantis DHD table linked to the same Stargate mirror the jumper's
  actively encoded symbols throughout the dialing sequence.
- Keeps all Lantea integration optional through reflection and pseudo mixins;
  Lantea is neither bundled nor declared as a required dependency.

- Makes both ceiling-hatch rims opaque with solid metal textures and closed inner/outer edges.

- Carries forward the installed 2.0.4 JAR as the release baseline.
- Bundles the constructed Lantea city copied from the `world` save as 2,009
  chunk-sized structures at its original coordinates, including the underwater
  foundations, Pegasus Stargate, block-state patterns, and block entities.
- Preserves both parked puddle jumpers while omitting transient dropped items
  and falling blocks.
- Adds independent circular ceiling hatches at Y 428 and Y 443 and one shared
  Atlantis-style table with a two-door GUI. Each successful table command uses
  50 FE from a 10,000 FE buffer that accepts power on every side.
- Restores the shield table's two continuous angled legs beneath the jumper
  bay tabletop, and connects a Large Naquadah Cable two or three blocks below the table anchor
  through the floor.
- Fixes concealed door-table power by following intermediate cable segments to real network endpoints, with separate outputs for the DHD and door table sharing a cable network.
- Replaces instant roof hatch switching with six-second Milky Way iris blade
  animation, synchronized motion, saved motor state, and aperture collision.
  Closure reverses if a player or vehicle enters the doorway.
- Seals both fully closed roof hatches against rain and snow with an invisible,
  non-colliding weather barrier that clears after opening completes.
- Adds an optional craftable redstone port for each door: power opens the
  adjacent roof door and removing power closes it.
- Adds recipes, inventory models, and loot for the table and replacement hatch
  panels and redstone port. Closing a door refuses to overwrite an obstructing
  block or entity.

# 2.0.3-beta.3

- Insets all triangular-button face UVs into the solid teal texture interior to
  prevent neighboring texture-atlas colors from bleeding onto narrow bevels.
- Preserves button geometry, top symbols, encoded gold state, and all
  non-triangle model faces.
- Deluminates the complete Atlantis DHD table when it has no stored energy.
- Draws the powered rear lightbar dynamically and locks its accepted glow alpha
  to `0.50` after side-by-side comparison with the City Shield table.
- Normal right-click opens the Atlantis dialer from every part of the table.
- Crystal Controls opens only with sneak/Shift + right-click.
- Removes the old side/lower-table shortcut to Crystal Controls.
- Lets a concealed SGJourney Large Naquadah Cable power a nearby Atlantis DHD
  through a separate unused virtual cable side.
- Preserves occupied cable connections, including a Mekanism Teleporter above
  the concealed cable.
- Fixes the proxy Crystal Controls block position so client and server slot
  counts remain synchronized.
- Exposes the implicit 100 mB Liquid Naquadah input in all 30 MoreGates 4.3.2
  crystallizer recipes so Almost Fluidified 0.1.8 can unify it when Naquadah is
  explicitly enabled in that mod's configuration.
- Places those corrected recipes in the generated top-priority MoreGates
  compatibility pack so they reliably override MoreGates' original resources.
- Keeps those overrides as top-level SGJourney crystallizer recipes so fluid
  unifiers can discover them, with a MoreGates-loaded condition preventing
  registration when the addon is absent.
- Preserves every confirmed hotfix5 model and visual unchanged.

# 2.0.2-beta.2-hotfix5

- Insets all 126 dialer-backplate face UVs into the solid-brown texture interior to address white edge bleed.
- Changes no geometry or button positions; preserves hotfix4 half-gap crystal heights.

# 2.0.2-beta.2-hotfix4

- Lowers all six flat crystals by half their previous clearance.
- Rear-row gap: 0.60 to 0.30 model pixels; front-row gap: 0.45 to 0.225.
- Preserves crystal thickness, horizontal positions, textures and all other geometry.

# 2.0.2-beta.2-hotfix3

- Copies the accepted shield table's smooth legs, feet, joints, stepped sides,
  and full illuminated rear panel onto the DHD.
- Preserves DHD tabletop controls, textures, GUI, recipes and gameplay code.
- Includes standalone and SGJ Additions override models and self-contained textures.
- Backlight is visually emissive; this update adds no power-detection logic.

# 2.0.2-beta.2-hotfix2

- Fixes the physical Atlantis DHD rendering as Minecraft's purple-and-black
  missing model in minimal Forge installations.
- Makes the approved extended DHD model self-contained instead of depending on
  another modpack's renderer behavior.
- Preserves the exact accepted table geometry, GUI, constellation mapping,
  gold pressed-button lighting, dialing behavior, and hotfix1 recipes.

# 2.0.2-beta.2-hotfix1

- Removes the obsolete `pegasus_upgraded_shield` recipe entry and replaces it
  with a clean Atlantis Energy Iris recipe using the accepted
  NeoForge 1.21.1 layout: eight Advanced Energy Crystals surrounding one
  Stargate Shielding Ring.
- Adds the missing standalone Atlantis DHD recipe using Trinium Ingots, an
  Advanced Communication Crystal, two Advanced Energy Crystals, a Pegasus DHD,
  and an Advanced Control Crystal.
- Changes the Forge display version and exact network protocol to
  `2.0.2-beta.2-hotfix1`.

# 2.0.2-beta.2

- Adds Forge 1.20.1 crafting recipes matching the accepted NeoForge 1.21.1
  recipes for the standalone Atlantis DHD table and Pegasus Energy Iris.
- Repackages the user-confirmed, fully working R17b baseline under the requested
  public version `2.0.2-beta.2`; GUI, physical rendering, constellation artwork,
  geometry, mappings, and behavior remain unchanged.
- Changes Forge display-version metadata to `2.0.2-beta.2` with
  `displayTest="MATCH_VERSION"` and pins the network channel protocol to the
  same exact string. Clients and servers must both run `2.0.2-beta.2`.

# 2.0.3-beta.2

- R17b was confirmed by the user on 2026-08-27 to work completely and is now
  frozen under `baseline/confirmed-working-r17b/` as the permanent full-project
  baseline. R13 remains the immutable GUI-layout baseline inside that contract.
- R17 direct-resource correction: the screenshots prove the physical renderer
  and gold state are active while SGJourney's `ClientSymbols` lookup is empty.
  The renderer now reads the connected gate's exact `assets/<namespace>/sgjourney`
  symbol and Point-of-Origin JSON, loads the referenced PNG pixels directly,
  and uses the locked GUI's exact Pegasus artwork only if a client pack omits
  that data. The rejected R14 dilation and oversized icon square remain absent.
- R16 visibility correction: resolves `ClientSymbols` and
  `ClientPointOfOrigin` directly from the connected gate before consulting the
  DHD's synchronized copy, then renders the exact live stitched-sprite pixels
  through the proven full-bright physical overlay. This removes R15's invisible
  raw textured BER path while retaining gate-variant artwork with no bundled
  fallback or stroke dilation.
- R15 live-variant correction: preserves the complete accepted R13 GUI JAR
  byte-for-byte outside the physical button renderer and renders SGJourney's
  exact `getExtendedSymbolTexture` / `getExtendedTexture` results on the block.
  There is no packaged-symbol fallback, mask conversion, dilation, or invented
  constellation artwork; each DHD follows its synchronized gate variant and
  point of origin.
- Locks the accepted `2.0.2-beta.1` JAR, source archive, and Blockbench model as
  the immutable visual baseline for this release.
- Adds the active gate variant's correct constellation marking to every
  physical Atlantis DHD triangle, including the three symbol crystals in the
  center cluster.
- Mirrors the live dialed address on the table: accepted symbols glow gold and
  remain illuminated until the DHD address clears.
- Mirrors the center Engage state on its physical crystal and includes the
  gate-provided Point of Origin glyph.
- Uses the DHD's synchronized SGJourney symbol set, so the physical markings
  follow the same Pegasus mapping as the connected gate and GUI.
- Uses the accepted GUI's exact final left-to-right row map on the physical
  five-row crystal plate, so every numbered GUI triangle and tabletop triangle
  reference the same SGJourney symbol rather than an approximate nearby slot.
- Reads every numbered constellation and Point of Origin from SGJourney's live
  extended textures and renders the original transparent artwork directly at
  full brightness.
- Aligns the physical overlay from the table's already-baked triangle surface
  quads, keeping it seated on the accepted oversized model for every standalone
  and legacy block orientation without relying on an assumed facing transform.
- Uses bright cyan inactive constellation linework for clear contrast against
  the accepted teal crystal texture.
- Implements all physical button feedback as a client rendering overlay. No
  table geometry, button positions, dimensions, hitboxes, numbering, or dialing
  logic were changed.
- Enables Forge `MATCH_VERSION` display checking and uses `2.0.3-beta.2` as the
  exact network-channel protocol, rejecting mismatched client/server builds.

# 2.0.2-beta.1

- Restyles only the Atlantis DHD table shell with the shield console's copper,
  dark-inlay, deck, and segmented fascia visual language.
- Adds a recessed front band, five raised fascia blocks, and subtle foot inlays
  without moving the table or altering its two-block footprint.
- Leaves the DHD, DHD plate, triangle buttons, center controls, and six patterned
  left-side crystal panels unchanged.
- Includes the updated Blockbench source and a reproducible model-generation
  script.
- Fixes the SGJ Additions override pack so the restyled table's block-model
  bindings and copper textures load with the geometry instead of displaying
  Minecraft's magenta/black missing-texture fallback.
- Recolors the DHD backplate with the same copper-brown fascia treatment used
  by the restyled table and applies the matching fascia/deck treatment to the
  support dividing the two rows of three patterned control crystals.
- Gives the 21-piece DHD dialer plate a dedicated, fully opaque copper-brown
  texture from the table palette, eliminating bright UV and mipmap bleed along
  its perimeter and internal angled seams.
- Replaces the compressed patterned texture on the narrow front shadow band
  with the same solid dark base color, removing the dotted bleed visible at
  both front corner joints without changing their geometry.
- Replaces the floating square iris control with a low-profile, stepped-round
  button seated on the tabletop and finished in the accepted crystal teal.
- Extends both raised side housings outward by a restrained 0.5 model pixel to
  conceal the end-face texture seam without creating oversized side wings.
- Reverses both Z-shaped leg supports to lean in the same direction as the
  Atlantis city-shield control table without changing their height or floor and
  tabletop contact positions.
- Widens the DHD table's sled feet and diagonal supports to approximately the
  same cross-section as the city-shield control table's legs.
- Preserves the complete 2.0.1-beta.1 More Gates 4.3.2 compatibility bridge and
  its existing configuration toggle.

# 2.0.1-beta.1

- Preserves the complete frozen 2.0.0-beta.1 Atlantis DHD implementation.
- Adds a default-enabled, restart-required More Gates compatibility toggle.
- Restricts the bridge to `moregates-4.3.2-forge-1.20.1.jar`, CurseForge file
  `5783142`, verified by its exact SHA-256 fingerprint.
- Migrates More Gates 4.3.2 symbol and point-of-origin registry files to the
  current SGJourney schema.
- Converts the legacy More Gates symbol atlases at runtime without bundling the
  third-party mod or its assets in this release.
- Adds current SGJourney unstable wormhole, vortex, and disconnect definitions
  to legacy More Gates gate variants while retaining each variant's textures,
  sounds, colors, and model settings.
## 2.0.5-dev-103 - SGJourney 0.6.55-alpha

- Migrate the Forge baseline DHD's crystal recalculation to SGJourney's
  current protocol, energy, transfer, and communication fields.

