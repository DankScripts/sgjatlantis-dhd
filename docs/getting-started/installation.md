---
layout: default
title: Installation
parent: Getting Started
nav_order: 2
---

# Installation

| | Forge release | NeoForge release |
|:--|:--|:--|
| Minecraft | 1.20.1 | 1.21.1 to before 1.22 |
| Loader declared by jar | Forge 47.4.0 or newer | NeoForge 21.1.219 or newer |
| Build target | Forge 47.4.16 | NeoForge 21.1.252 |
| Java | 17 | 21 |
| Required mod | Stargate Journey 0.6.55-alpha or newer compatible build | Stargate Journey 0.6.55-alpha or newer compatible build |

1. Install the loader and Java for your Minecraft version.
2. Put the matching Atlantis and Stargate Journey jars in `mods`. Use matching client and server versions for multiplayer.
3. If desired, install a compatible version of Lantea for its optional integration. The NeoForge release metadata declares Lantea 0.7.3.0 or newer, and its release README recommends 0.7.7.12. This is not a guarantee of compatibility with every later version.
4. Start the game or server and check the log for dependency errors.

Stargate Journey alpha builds may be privately supplied; the version requirement is not a promise of public availability or compatibility with untested beta versions. Keep only the intended Atlantis jar for the loader in the mod folder.

## Identifying the documented files

These are SHA-256 hashes of the local 2.0.5 files used for this guide. A different hash means a different file, even if its version label matches.

| File | SHA-256 |
|:--|:--|
| `sgjatlantis-dhd-1.20.1-2.0.5.jar` | `02f06a164d7824ef9d36305b99702a462d080fc1723c74995b77a2a1b8e4e8fe` |
| `sgjatlantis-dhd-1.21.1-2.0.5.jar` | `b929cc15e958137e9ae7f588bbff1ca04a41e3dab05a3a48c097fd5043c424a8` |
| `sgjatlantis-dhd-1.20.1-2.0.5-source.zip` | `f819ec4193aa93769c42577fd9565bc25e4f894df2dbf954af007a0b21e8e1af` |
| `sgjatlantis-dhd-1.21.1-2.0.5-source.zip` | `1a3a25cd09b162e3b9a9c673c3cc0e75774ad1656a9c35c48efec6dc135535f6` |

{: .warning }
Back up the world before changing Minecraft versions, loaders, mods or major mod versions. Moving a world from Forge 1.20.1 to NeoForge 1.21.1 is not a certified world conversion.
