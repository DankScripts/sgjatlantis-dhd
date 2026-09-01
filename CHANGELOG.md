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
