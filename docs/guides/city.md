---
layout: default
title: Atlantis City
parent: Player Guides
nav_order: 1
---

# Atlantis City

The bundled city belongs to Stargate Journey's `sgjourney:lantea` dimension. Its packaged city index covers **2,009 chunks**. The mod processes indexed chunks as they load; the whole city is not necessarily installed at once. Automatic processing checks for recognized existing or player-built occupancy before replacing a chunk.

The city includes an Atlantis table and SGJourney gate, transporters, and the two fixed jumper-bay openings. The city table is supplied with its initial dialing crystals when suitable slots are empty; that behavior does not apply to ordinary player-placed tables. Gate registration and table linking are also repaired by city-specific code when applicable.

## Manual rebuild

{: .warning }
`/sgjatlantis city install` clears and rebuilds the bundled city area. Player construction or other changes in that area can be lost. Back up the **whole world**, including the Lantea dimension, before starting. Automatic occupancy checks are not a guarantee of preservation during a manual rebuild.

An operator with permission level 2 or higher can run:

* `/sgjatlantis city install` - begin the manual clear-and-rebuild job.
* `/sgjatlantis city status` - inspect its progress.

The job works in batches rather than rebuilding the entire city in one tick. The `cityInstallSpeed` setting controls the rate; see [configuration]({{ '/reference/configuration.html' | relative_url }}). Let the job finish and check its reported status before relying on the rebuilt area. Do not assume simply updating the jar performs this rebuild.
