# Atlantis DHD

Atlantis DHD is a standalone Atlantis/Pegasus-style Dial Home Device for
Stargate Journey. It provides an Atlantis-themed physical DHD, dialing
interface, live Pegasus constellation display, crystal controls, energy
support, Stargate shield integration, and optional compatibility features.

## Default branch: Forge 1.20.1

This is the source package corresponding to the currently public Forge 1.20.1
release supplied by the project author:

- Minecraft 1.20.1
- Forge 47.4.0
- Java 17
- Atlantis DHD 2.0.2-beta.2-hotfix2

The NeoForge 1.21.1 port is maintained separately on the
`1.21.1-neoforge` branch.

## Important build note

This public Forge source release preserves its original patch-overlay build
process. The release task combines the source in this branch with the author's
frozen, known-good Atlantis DHD baseline stored under `baseline/`. Those owned
baseline artifacts are intentionally retained because removing them would make
this historical public source package unable to reproduce its release build.

Future development may replace this legacy assembly process with a fully
source-native build. See [DEVELOPMENT_NOTES.md](DEVELOPMENT_NOTES.md) for the
technical release and preservation notes.

## Building

Run:

```bash
./gradlew release
```

The release JAR and source archive are written to `dist/`.

The build requires the matching Stargate Journey 1.20.1 development JAR. Set
the `SGJOURNEY_JAR` environment variable to its local path when it is not
already available in the expected development workspace.

## Compatibility

This release contains an optional compatibility bridge for exactly More Gates
4.3.2 for Forge 1.20.1. It does not bundle More Gates. See
[DEVELOPMENT_NOTES.md](DEVELOPMENT_NOTES.md) for its exact version guard and
configuration setting.

Compatibility reports and focused pull requests are welcome. Include the
other mod's exact Minecraft version, loader, mod version, and public API or
source link when available.

## Downloads

Use the official CurseForge project for supported release downloads. Files
built directly from this development repository may be unfinished or untested.

## License

Copyright (c) 2026 MustangDoc / DankScripts. All Rights Reserved.

This repository is source-visible for inspection, compatibility work, and
contributions. It is not open-source software and may not be redistributed,
repackaged, or published without prior written permission. See
[LICENSE.txt](LICENSE.txt) for the complete terms.

