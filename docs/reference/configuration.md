---
layout: default
title: Configuration and Commands
parent: Reference
nav_order: 2
---

# Configuration and Commands

Settings are generated in the instance or server's `config` directory. Back up the world and configuration before significant changes. Multiplayer server settings should be changed on the server.

## Atlantis city and lights

In `sgjatlantis-config.toml`:

| Setting | Default | Meaning |
|:--|:--|:--|
| `inscribedLightHoldSeconds` | `90` | Hold time for proximity-triggered, non-powered inscribed stairs and slabs; range 0-86400 seconds. |
| `cityInstallSpeed` | `MEDIUM` | Batch rate for the manual city installation job: `HIGH`, `MEDIUM` or `LOW`. |

For the manual city job, HIGH handles one clearing chunk or two rebuilding chunks per tick, MEDIUM one per tick, and LOW one every four ticks. Rate is not a safety guarantee. See the [city rebuild warning]({{ '/guides/city.html' | relative_url }}).

## Jumper screen

In `sgjatlantis-jumper-common.toml`, `useAtlantisDialerForLanteaGates` defaults to `false`. Setting it to `true` changes the screen used for native Lantea gates in compatible jumpers. The server chooses in multiplayer. It does not merge gate networks.

## Operator commands

These `/sgjatlantis` commands require operator permission level 2:

| Command | Purpose |
|:--|:--|
| `/sgjatlantis city install` | Destructively clear and rebuild bundled city chunks. **Back up the whole world first.** |
| `/sgjatlantis city status` | Show manual city job progress. |
| `/sgjatlantis transporter list` | List stations in the current dimension. |
| `/sgjatlantis transporter rename <name>` | Rename a nearby transporter station. |
| `/sgjatlantis transporter give` | Give the operator a transporter room item. |

The DHD also has development-only screen-editor settings; leave them disabled for ordinary play. Forge and NeoForge use different client configuration mechanisms. Do not change them as a fix for a missing gate connection.
