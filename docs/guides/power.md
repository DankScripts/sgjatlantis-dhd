---
layout: default
title: ZPMs and Power
parent: Player Guides
nav_order: 4
---

# ZPMs and Power

## Triple ZPM Hub

The Triple ZPM Hub accepts up to **three SGJourney ZPMs**. Its energy output is on the **bottom**. It draws from installed modules; it is not a charger. SGJourney's ZPM transfer and permission settings affect output and inventory access.

Place a compatible receiver or route an appropriate power connection from the output. If using SGJourney cables, check their zero-point-energy transfer settings rather than assuming every cable tier carries ZPM power. Transfer to non-SGJourney energy consumers also depends on SGJourney's `other_mods_use_zero_point_energy` setting and the receiving device.

## Floor conduits

Atlantis floor conduits forward energy through connected, loaded conduit blocks to compatible endpoints. They do **not** generate or store energy. Their network routing is reconstructed from loaded blocks; an unloaded section can interrupt a route.

If a table or hatch control is empty, check the source, bottom output, connections, loaded chunks and the destination's accepted energy before replacing blocks. A visible cable or conduit by itself does not prove that the system is powered.

The SGJourney gate iris has a continuing power cost while closed; see [Pegasus gate iris]({{ '/guides/gate-iris.html' | relative_url }}). The city-shield addon has its own separate power rules.
