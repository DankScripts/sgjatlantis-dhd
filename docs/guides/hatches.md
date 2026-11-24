---
layout: default
title: Jumper-Bay Hatches
parent: Player Guides
nav_order: 6
---

# Jumper-Bay Hatches

The city's jumper-bay system controls two **fixed** openings in `sgjourney:lantea`, centered around X `-3892`, Z `5885`:

| Table label | Opening height |
|:--|:--|
| GATE ROOM CEILING | Y `428` |
| JUMPER ROOM CEILING | Y `443` |

The two openings operate independently. The table and hatch-panel items do not turn an arbitrary custom-built opening elsewhere into another controlled hatch.

## Table control

Use the Jumper Bay Door Table to select an opening and operate it. The table has a **200 FE buffer** and a successful GUI operation costs **50 FE**. It needs a real power supply; reopening the interface can attempt to refresh its cable feed, but does not create power.

Closing can be blocked by entities or vehicles in the opening, or by obstructing blocks. Clear the opening and retry rather than trapping a jumper or player.

## Redstone control

Place a Jumper Bay Hatch Redstone Port beside the intended fixed opening, at its **exact Y level** and no more than **7 blocks away on either the X or Z axis**. When the signal changes, a powered port requests open and an unpowered port requests closed. It calls the hatch service directly and does not spend the table's GUI energy. Safety/obstruction checks still matter. Each opening needs a correctly positioned port for its own redstone control.
