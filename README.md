# Atlantis DHD 2.0.4

## Source Branches

- [Forge 1.20.1](https://github.com/DankScripts/sgjatlantis-dhd/tree/1.20.1-forge)
- [NeoForge 1.21.1](https://github.com/DankScripts/sgjatlantis-dhd/tree/1.21.1-neoforge)

Atlantis DHD is a standalone Atlantis/Pegasus-style Dial Home Device for
Stargate Journey. This branch contains the NeoForge port for Minecraft 1.21.1.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.219
- Java 21
- Stargate Journey 0.6.50 or newer

## Features

- Current Atlantis control-table model and physical button rendering.
- Atlantis/Pegasus dialing interface and live constellation display.
- Normal right-click opens the dialer.
- Sneak-right-click opens Crystal Controls when permitted.
- Native SGJourney support for compatible Pegasus crystals.
- Large Naquadah Cable power support through the restored decorative floor.
- Stargate shield controls and migration compatibility.

The minimum operating set is one Large Control Crystal, two Advanced Energy
Crystals, and one Advanced Transfer Crystal. Advanced Communication Crystals
remain optional.

## Building

Run:

```bash
./gradlew release
```

The release JAR and source ZIP are written to `dist/`.

See `PORT_STATUS.md` and `CHANGELOG.md` for release-specific details.

## Downloads

Use the official CurseForge project for supported release downloads. Files
built directly from this development branch may be unfinished or untested.

## License

Copyright (c) 2026 MustangDoc / DankScripts. All Rights Reserved. See
`LICENSE.txt` for details.
