---
layout: default
title: Lantea and Puddle Jumpers
parent: Player Guides
nav_order: 9
---

# Lantea and Puddle Jumpers

Lantea is optional. Its native Stargates, addresses and puddle jumper remain Lantea systems. Atlantis supplies compatibility controls; it does **not** merge native Lantea and Stargate Journey gate networks.

## Stationary Atlantis table

If the table has a valid SGJourney gate link, that link takes priority. Otherwise, when compatible Lantea is present, it can find a nearby, assembled native Lantea gate in the same dimension. The table still checks its required crystals. Native addresses use `mw:` or `pg:` prefixes and must be valid in Lantea's registry.

In the native table screen, Ctrl+V checks a Lantea address and highlights the buttons for **manual** entry. Enter the highlighted sequence, including origin when prompted, then use the center. The saved-address row can start a guide; its separate **Redial** button initiates redial. Clear an existing sequence before choosing another address.

## Jumper dialer

The integration respects Lantea's own authorization checks, including jumper occupancy, the Ancient gene and the installed DHD crystal. The native Lantea gate is checked first. By default, its original Lantea jumper dialer remains in use.

If no native gate takes priority, the integration can select a loaded SGJourney gate in the current dimension within **64 blocks** of the jumper and display the Atlantis-style dialer. Enter symbols and use the center control; gate feedback governs success.

To use the Atlantis-style jumper screen for native Lantea gates too, set `useAtlantisDialerForLanteaGates = true` in `config/sgjatlantis-jumper-common.toml`. The default is `false`; on multiplayer the server chooses. This changes presentation and controls, **not** the gate's address system or network. With the Atlantis native screen, Ctrl+V accepts a Lantea-prefixed address for guided manual entry; right-click clears the native jumper's current sequence.

Gate selection, authorization and available buttons can differ with dependency versions. If compatibility fails, check the game or server log and verify the installed Lantea version.
