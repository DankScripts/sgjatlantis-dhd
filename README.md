# Atlantis DHD 2.0.5-hotfix1

The NeoForge hotfix restores the 14 crafting recipes missing from 2.0.5,
including the Atlantis DHD, transporter components, door kits and controls,
and jumper-bay table, panels and redstone port. They match the corresponding
Forge recipes; no changes to the Forge release are required.

## Stargate Atlantis registry migration

Atlantis blocks, items and block entities now use `stargate_atlantis`. Aliases
preserve lookups under `sgjatlantis_dhd` and `sgjadditions_capacity_patch`.
The legacy mod container and saved-data keys remain compatible. Use the matching
City Shield 1.0.4 release and back up existing worlds before upgrading.
The six decorative DHD control crystals stay visible, dimmed when unpowered.

## Source Branches

- [Forge 1.20.1](https://github.com/DankScripts/sgjatlantis-dhd/tree/1.20.1-forge)
- [NeoForge 1.21.1](https://github.com/DankScripts/sgjatlantis-dhd/tree/1.21.1-neoforge)

Atlantis DHD is a standalone Atlantis/Pegasus-style Dial Home Device for
Stargate Journey. This branch contains the NeoForge port for Minecraft 1.21.1.
This branch builds entirely from source; no preserved Atlantis binary is needed.

## Source and compatibility hooks

The NeoForge release JAR is built from this project's source, not a binary
overlay. Its Atlantis DHD uses an SGJourney block-entity DHD adapter with a
`DHDControlledStargate`. Recent-address recording and synchronization belong
to the Atlantis entity; active compatibility mixins are listed in
`src/main/resources/sgjatlantis_dhd.mixins.json`. These target Minecraft,
SGJourney, Lantea, or SG Mobs, not this mod's own classes.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.252
- Java 21
- Stargate Journey 0.6.55-alpha (build and compatibility target)
- Optional: Lantea 0.7.7.12 for Minecraft 1.21.1
- Optional: SGJourney Mobs (triple ZPM hub API target: 1.0.10)

The Atlantis energy iris uses a continuous shield mesh on all SGJourney gate
variants, including the SGJourney gates extended by Stargate Mobs. Lantea gates
use their own iris state: use the Atlantis energy iris item on the controller or
ring to install it; use the item again or sneak-use with an empty hand to toggle
it. Sneak-use with the iris item removes it. A closed destination iris destroys
incoming Lantea travelers before transit. In-game visual and transit testing
is still required for this new Lantea integration.

## Version 2.0.5

Ports the accepted Forge keypad layout, continuous bronze plate, equilateral
button spacing, larger constellation glyphs, centred lower controls, and mini-gate
position. The physical table uses the matching resized keypad and backing plate.

An Atlantis table can operate a nearby native Lantea gate when no SGJourney gate
is linked nearby. Both `mw:` and `pg:` addresses are accepted. Pasting prepares a
step-by-step guide; clicking each symbol updates the native gate immediately,
then the centre button connects it. Lantea owns its connections and wormhole
travel. This does not connect SGJourney gates to Lantea gates.

The optional Atlantis jumper screen is controlled by
`config/sgjatlantis-jumper-common.toml`, `useAtlantisDialerForLanteaGates`.
It defaults to false, matching the existing Forge configuration: native Lantea
jumper gates retain Lantea's screen; set true to use the Atlantis screen there.
The Atlantis table support is always available when Lantea is installed.

The installed 2.0.4-hotfix1 shield visuals, sound, 1200 FE/t power requirement,
creative-tab entries, and concealed cable power bridge are retained.

The NeoForge JAR now bundles the complete 2,009-piece Atlantis city in
SGJourney's Lantea dimension. Its Stargate address is restored to the SGJourney
network when the city loads. The custom jumper bay hatch table and panels,
transporters, and Atlantis door kits and controls are included. Blocks used by
Forge-only dependencies are substituted with available materials.

The `farlands-final` build uses the final Farlands layout, including the
moved jumper bay table and shield controller and the separate decorative
console. Use the matching `farlands-final` City Shield build for the shield
preset at the new location. Replacing the JAR does not rebuild an already
installed city.

The city uses the server backup for its main city and lower rooms, with the
later Farlands control-room layout. Genetics Lab, Lower Spire, and the
underwater bay appear on the transporter LOWER page; outer towers remain on
CITY. Both client and server need the updated JAR for the screen change.
For an existing city, back up the Lantea dimension before running
`/sgjatlantis city install`; the command rebuilds the bundled city in place.

## Features

- Current Atlantis control-table model and physical button rendering.
- Atlantis/Pegasus dialing interface and live constellation display.
- Normal right-click opens the dialer.
- Sneak-right-click opens Crystal Controls when permitted.
- Native SGJourney support for compatible Pegasus crystals.
- Large Naquadah Cable power support through the restored decorative floor.

SGJourney's `sgjourney-common.toml` controls ZPM power on native Naquadah
Wire and Small, Medium, and Large Naquadah Cable. To allow it on all four,
enable each tier's `*_transfers_zero_point_energy` setting. This does not
change transfer limits or make different native tiers connect directly.
- Stargate shield controls and migration compatibility.
- Bundled Atlantis city, hatch controls, transporter rooms, and door controls.

The minimum operating set is one Large Control Crystal, two Advanced Energy
Crystals, and one Advanced Transfer Crystal. Advanced Communication Crystals
remain optional.

## Building

Use Java 21. JEI 19.27.0.336 and Lantea 0.7.7.12 are downloaded from Maven.
The SGJourney 0.6.55-alpha was privately supplied by its author; obtain the
NeoForge 1.21.1 JAR and set `SGJOURNEY_JAR` to its path, or place it in
`libs/Stargate.Journey-1.21.1-0.6.55-alpha.jar`. Do not substitute public 0.6.54.
When the public beta is released, update and recheck the matching dependency.

Run in PowerShell:

```powershell
.\gradlew.bat clean release
```

The release JAR and source ZIP are written to `dist\`. Release validation
rejects packaged mod classes without corresponding Java source.

See `PORT_STATUS.md` and `CHANGELOG.md` for release-specific details.

## Downloads

Use the official CurseForge project for supported release downloads. Files
built directly from this development branch may be unfinished or untested.

## License

Copyright (c) 2026 MustangDoc / DankScripts. All Rights Reserved. See
`LICENSE.txt` for details.
