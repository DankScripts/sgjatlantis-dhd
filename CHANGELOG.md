## 2.0.5-hotfix1

- Restore the 14 NeoForge crafting recipes missing from 2.0.5, matching the
  corresponding Forge recipes for the Atlantis DHD, transporter components,
  door kits and controls, and jumper-bay table, panels and redstone port.

## Unreleased

- Download JEI and Lantea build dependencies from Maven instead of a local
  Minecraft installation. Keep the privately supplied SGJourney alpha explicit.
- Exclude the historical Atlantis binary from the source archive and reject
  packaged mod classes without corresponding Java source.

- Keep all six decorative DHD control crystals visible when unpowered, with
  dimmed green lighting, and draw them above the table background.

- Register Atlantis blocks, items and block entities under `stargate_atlantis`,
  retaining aliases for both previous Atlantis namespaces.

- Repair two maintenance-area floor conduits and their adjacent cable connection
  in the bundled city.
- Start client door animations when the update arrives, even if the door is
  offscreen, rather than delaying the animation until it is next rendered.
- Recheck loaded floor-conduit neighbors and safely rebuild missing SGJourney
  cable networks. Save changed cable network IDs and defer incomplete or unsafe
  native traversals instead of forcing chunks to load.
- Allow a mixed power network to accept a later, larger offer after a smaller
  offer failed within the same transfer.
- Keep mixed-network simulation results specific to the active route and
  discard them after committed transfers change available capacity. Reuse
  compatible results across converging routes to avoid repeated traversal.
- Show the Genetics Lab and Lower Spire under the transporter's LOWER level,
  alongside the underwater bay, without changing unrelated destinations.
- Bundle the server-backup city and lower rooms with the later Farlands
  control-room layout, without copying its temporary open hatch and flooding.

## 2.0.5-dev-138-gate-parity

- Synchronize the Atlantis table's glyphs and point of origin with its linked
  Stargate Journey gate, including Milky Way and Universe.
- Draw the linked model and its current variant, including variants supplied
  by MoreGates. Keep the live ring during brief link updates and draw engaged
  chevrons after the ring so the lights aren't covered.
- Animate the Milky Way mechanical iris when toggled from the table.
- Route unlinked tables to assembled native Lantea gates. Render the selected
  gate's live glyphs, ring motion, and energy iris in the native dialer.
  The native dialer packet changed; use matching builds on client and server.
- Refresh the hatch table's native cable feed through the connected floor
  branch on load or when opening the table with insufficient power.

## 2.0.5-dev-137-transporter-crash-fix

- Fix a Lantea server tick and city-install crash when scanning the expanded
  transporter directory. Each station now carries its own facing.

## 2.0.5-dev-136-native-dhd-link

- Retry native SGJourney DHD linking when opening the table. Restore in-range
  saved links and recover gates that still point to the same table, without
  taking over gates already linked to another DHD.

## 2.0.5-dev-134-restore-original-hatch

- Restore the original powered hatch table and installer location, remove the
  redundant working table, and keep the decorative table separate.

## 2.0.5-dev-133-hatch-power-feed

- Reconnect the relocated hatch console's floor conduits to the bundled ZPM
  network without changing the table's power cost or the decorative console.

## 2.0.5-dev-132-console-legs-hatch-charge

- Render the decorative console's legs and restore the active bundled hatch
  table's starter charge at its new control-room location.

## 2.0.5-dev-131-decorative-console

- Replace the extra spire roof table with a placeable decorative console.
  Leave the working control-room hatch table in place.

## 2.0.5-dev-130-farlands-spire

- Refresh the bundled gate and control rooms from Farlands, including the
  recessed database console and the relocated DHD, shield and hatch controls.

## 2.0.5-dev-129-recessed-database-console

- Angle the front of the Database Console base inward to clear a player's
  approach to both stations. Keep the rear contour and aligned tabletop controls.

## 2.0.5-dev-128-aligned-database-stations

- Rotate each Database Console station as a unit so its tabletop, keypads,
  and display panels stay square. Keep the approved base and worktop height.

## 2.0.5-dev-127-contoured-database-console

- Reshape the Database Console around a clipped front and pointed rear, with
  matching base collision and mirrored stations at the existing worktop height.

## 2.0.5-dev-126-database-console

- Add a decorative Atlantis Database Console with a 5 x 3 footprint, a
  one-block plinth, and a translucent placeholder display. Place it by
  right-clicking the floor with the console item from the Atlantis creative tab.

## 2.0.5-dev-124-native-dhd-clear

- Press the centre diamond on an Atlantis table linked to a native Lantea
  gate to clear a partial address. A complete address still dials.

## 2.0.5-dev-123-mixed-power

- Probe naquadah cable outputs using the same energy type as the transfer, so
  zero-point power can cross alternating floor conduits and cables.

## 2.0.5 release - SGJourney 0.6.55-alpha

- Restore the pre-rebuild Pegasus address when available and unclaimed by another
  gate, revalidate registration, and report the two-way DHD link separately.
- Retry the live Pegasus gate's cached registry lookup after city registration.

- Restore missing or one-sided bundled Pegasus links without moving the gate
  or changing its address. Initialize controller callbacks and sync table glyphs.
- Keep the keypad open when dialing, clear partial addresses, and wait for the
  gate to lock the buffered address before opening a connection.
- Preview the live linked SGJourney gate model and variants, retaining native
  Lantea and SG Mobs display paths.
- Refresh 82 city chunks and their placement maps, include all four additional
  transporter destinations, and match the Forge starter ZPM entropy.

- Forward the Atlantis DHD auto-close policy through its API adapter.
- Return error feedback for rejected dialing and preserve an occupied city DHD
  control slot when restocking.
- Report SG Mobs gate control and permission failures instead of treating them
  as successful actions.

## 2.0.5-dev-96 — SGJourney 0.6.55-alpha

- Implement `BlockEntityDHD` with SGJourney's block-entity adapter helper and
  register the NeoForge DHD block capability.
- Resolve linked Pegasus gate state through `DHDControlledStargate`, retaining
  the backing block entity only for existing renderer and shield operations.

# 2.0.5 loading correction

- Support SGJourney Mobs 1.0.10 as an optional NeoForge add-on. Its triple ZPM
  hub can power the Atlantis roof-control table directly from above, using the
  same SGJourney energy path already accepted by the Atlantis DHD.

- Remove erroneous event subscriber registration from LanteaJumperConfig; settings remain registered by the mod constructor.
- Check compiled subscriber annotations for missing handlers before release.

# 2.0.5 — NeoForge 1.21.1

- Port the approved GUI bronze plate, equal-sided triangles, spacing, positions, and larger glyphs.
- Port the centred physical keypad, resized backing plate, and larger physical glyphs.
- Add optional native Lantea DHD/jumper support, mw/pg address guidance, live symbol entry, and table lights.
- Preserve installed shield power, sound, rendering, creative entries, and cable fixes.
- Remove the superseded GUI offset table and centre-layout editor from the fixed layout.

# Changelog

## 2.0.4

- Promotes the SGJourney 0.6.50 compatibility hotfix to a release build.
- Preserves the current GUI, table assets, crystal requirements, and native
  dialing callbacks; the missing-crystal report was resolved by the user.
- Adds release JAR and source ZIP packaging through `./gradlew release`.

## SGJourney 0.6.50 Compatibility Hotfix

- Rebuilt NeoForge 2.0.3-beta.3 against SGJourney 0.6.50 and require that version.
- Updated DHD/crystal-menu packages, synced energy capacity, block interactions,
  native symbol/engage callbacks, Pegasus button types, and mini-gate accessors.
- Preserved the 720x360 dialer background and existing model/texture assets.
- Center engage closes the full container only after six regular symbols;
  Escape closes normally without the address requirement.
- Namespaced the model-angle mixin helper to coexist with City Shield 1.0.2.
- Combined client startup/resource reload passed with SGJourney 0.6.50, City
  Shield 1.0.2, JEI, and JEI++. In-game dialing/power checks remain unconfirmed.

## 2.0.3-beta.3 — Minecraft 1.21.1 NeoForge

- Updated the physical Atlantis DHD table to the current accepted model.
- Changed interaction routing so normal right-click always opens the dialer
  and sneak-right-click opens Crystal Controls.
- Added the minimum dialing crystal set: one Large Control Crystal, two
  Advanced Energy Crystals, and one Advanced Transfer Crystal.
- Kept Advanced Communication Crystals optional.
- Preserved native SGJourney support for all compatible Pegasus crystal types.
- Added a conditional NeoForge capability proxy on the decorative floor tile
  directly above a Large Naquadah Cable. It exposes only the nearby Atlantis
  DHD to SGJourney's native cable output path, including zero-point energy.
- Preserved the established NeoForge 1.21.1 GUI, menus, shield controls,
  capabilities, symbol display, and migration compatibility.
# 2.0.5 Atlantis city bundle — NeoForge 1.21.1

- Include the full Atlantis city structure templates and restore its SGJourney
  Pegasus gate network address.
- Port the jumper bay hatch table, iris and weather seal, transporter rooms,
  and Atlantis door kits and controls.
- Map blocks from Forge-only dependencies to materials available in NeoForge.

# 2.0.5 NeoForge city model correction

- Use NeoForge's composite and OBJ model loaders for the copied city assets.
  This restores the jumper bay roof-control table model and its materials.

# 2.0.5 NeoForge roof-door culling correction

- Give NeoForge's iris renderer the full roof-door bounds so its blades stay
  visible across camera angles. Apply the same renderer-side bounds to the
  Atlantis door and transporter animations.

## Unreleased - source review

- Move Atlantis recent-address recording and synchronization into the Atlantis
  DHD entity rather than injecting into every SGJourney DHD.
- Preserve the existing history keys and eight-address limit.

