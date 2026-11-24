---
layout: default
title: Atlantis DHD
parent: Player Guides
nav_order: 2
---

# Atlantis DHD

The Atlantis table is a Stargate dialer with a keypad, a gate display and crystal controls. Right-click the table for the dialer; **sneak-right-click** it for Crystal Controls. Make sure a suitable nearby gate is assembled and linked and that the table has power.

## Crystals

For the documented SGJourney setup, install at least:

* One **Large Control Crystal**
* Two **Advanced Energy Crystals**
* One **Advanced Transfer Crystal**

Advanced Communication Crystals are optional. The city table is stocked in available slots by the city installer; a table you place yourself is not automatically stocked. The crystal screen also exposes energy-item slots and power information. A correct crystal set alone does not supply power or guarantee that a gate is connected.

## Dialing an SGJourney gate

Open the table, select the destination symbols in order, and use the center control to engage. Follow the screen's feedback if the gate is busy, the address is invalid, or the connection cannot be made.

The address guide accepts a numerical, hyphen-separated SGJourney address from the clipboard with **Ctrl+V**. It selects a **guided manual sequence**: press the highlighted symbols in order and then the center crystal. The guide adds origin `0` when it is absent. It accepts six to eight destination symbols; use an address valid for the destination and network.

The recent-address panel stores up to eight entries. Selecting an SGJourney entry starts the guide rather than instantly dialing it. On the NeoForge build, the guide can fall back to a recently detected chat address when no suitable clipboard address is found; do not rely on that fallback on Forge.

## Native Lantea gate

With compatible Lantea installed, a table without a valid SGJourney link can use a nearby assembled native Lantea gate. SGJourney-linked gates take priority. Lantea addresses use their own `mw:` or `pg:` format and registry; paste opens a guided sequence there too. Its recent-address panel also offers a **Redial** action, distinct from selecting a saved address for guided entry.

These are separate gate systems, not a combined network. See [Lantea and puddle jumpers]({{ '/guides/lantea.html' | relative_url }}) for details.

## Gate iris

The table's gate display provides the shield control for a compatible installed iris. This is a gate-mounted shield, not the separate city-wide shield addon. See [Pegasus gate iris]({{ '/guides/gate-iris.html' | relative_url }}).
