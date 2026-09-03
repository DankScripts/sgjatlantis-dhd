# Atlantis DHD Upgrade 2.0.3-beta.3

The user-accepted Forge 1.20.1 runtime from 2026-09-03 is the current permanent
baseline. Its exact JAR and checksums are stored in
`baseline/accepted-balanced-bars/`; future releases must start from that binary.
It includes zero-power table delumination, the accepted `0.50` powered rear
lightbar glow, and the accepted triangle-side atlas-bleed correction while
preserving the previously approved GUI and table behavior.

R17b was confirmed by the user on 2026-08-27 to work completely and is now the
historical full-project foundation. Its exact tested JAR, producing source
package, checksums, and locked preservation contract remain stored in
`baseline/confirmed-working-r17b/`.

The complete user-approved R13 GUI is now frozen under
`baseline/accepted-gui-r13/` as the permanent layout baseline. Physical block
rendering work must preserve that GUI unless the user explicitly requests a
layout change.

This release preserves the complete accepted `2.0.2-beta.1` Forge 1.20.1 Atlantis DHD,
including its narrowly guarded compatibility bridge for
`moregates-4.3.2-forge-1.20.1.jar` (CurseForge file `5783142`).

The physical DHD crystals now display the exact live constellation textures
selected by SGJourney for the connected gate variant. Symbols already entered
into the current address glow gold directly on the table, remain illuminated
during dialing, and return to their normal teal state when the address clears.
The live Point of Origin, symbols 8 and 33, and the center Engage crystal use
the same synchronized state. This is a renderer overlay only: the accepted
table geometry, triangle placement, hitboxes, and dialing behavior are
unchanged.

The physical overlay resolves its transform from the triangle surfaces in
Minecraft's already-baked table model. This keeps every constellation and gold
state directly on its assigned crystal regardless of the standalone/legacy
block type or table facing. Unselected constellation markings use a brighter
cyan line treatment so they remain readable against the physical teal crystal
surface. The physical plate now uses the accepted GUI's exact final
left-to-right row map, so every GUI triangle and tabletop triangle share the
same SGJourney symbol number. The overlay resolves the connected gate's live
symbol and Point-of-Origin resource keys, reads the corresponding SGJourney
client JSON and PNG resources directly, and renders those exact alpha pixels
through the physical model's reliable full-bright pass. This bypasses an empty
`ClientSymbols` lookup observed in the test runtime. If a third-party pack
provides no matching client JSON, the physical Atlantis plate uses the locked
GUI's exact packaged Pegasus pixels as its final fallback. Nothing is widened,
redrawn, or substituted with invented artwork.

Forge display-version matching is enabled, and the network protocol is pinned
to `2.0.2-beta.2-hotfix2`. Multiplayer therefore rejects a client/server pair running
different Atlantis DHD builds instead of allowing an unsafe mixed-version
connection.

Hotfix2 also permits only the three approved DHD element endpoints that extend
slightly beyond Minecraft's normal model-coordinate limit. This prevents the
physical table from becoming a purple-and-black missing model in minimal Forge
clients while retaining its exact accepted dimensions in larger modpacks.

The physical DHD table shell now uses the same copper, dark-inlay, segmented
fascia, and deck language as the Atlantis City Shield console. The DHD itself,
all triangle buttons, center controls, and the six patterned left-side crystal
panels retain their existing geometry and assignments. The DHD mounting plate
keeps its geometry but uses a UV-safe copper-brown texture from the new table
palette so its small angled pieces do not develop bright texture seams.
The narrow recessed strip behind the five front fascia panels likewise uses a
uniform dark texture so its exposed corner faces remain clean at every mipmap
level.

The bridge detects the exact legacy JAR by filename and SHA-256, then generates
an internal high-priority compatibility pack from the installed More Gates
resources. It migrates old symbol and point-of-origin data, splits the two legacy
symbol atlases for SGJourney's current sprite system, and fills in the modern
wormhole transition fields. More Gates itself is not bundled.

## Configuration

The setting is written to `config/sgjatlantis-dhd.toml`:

```toml
# More Gates 4.3.2 compatibility
# ONLY for moregates-4.3.2-forge-1.20.1.jar (CurseForge file 5783142).
# true (default): enable the legacy More Gates 4.3.2 compatibility bridge.
# false: disable this bridge. Other More Gates versions are never patched.
# Restart Minecraft after changing this setting.
enableMoreGates432CompatibilityPatch = true
```

## Build

Run `./gradlew release`. If the workspace-local SGJourney development JAR is
not present, set `SGJOURNEY_JAR` to the required SGJourney 1.20.1 JAR before
building. The finished JAR and source archive are written to `dist/`.
