---
layout: default
title: Transporters
parent: Player Guides
nav_order: 5
---

# Transporters

Atlantis transporters move passengers between complete registered cabins **in the same dimension**. Bundled city destinations are grouped into `CITY` and `LOWER` pages; the lower group includes Genetics Lab, Lower Spire and Underwater Jumper Bay. Custom stations can also appear in the directory.

## Using a cabin

1. Use a door control or switch to open the cabin and step inside.
2. Use the interior panel or map and select an available destination from the list or map. The destination selector cannot be opened from outside.
3. Both cabins must be complete, their doorways and passenger spaces clear, and passengers must be dismounted. The destination is loaded and checked before transport.

When safe, passengers in the two cabins are **swapped**; this is not limited to an empty destination. An obstructed or incomplete room prevents the trip rather than making it safe to force one.

## Placing a custom room

Use an Atlantis Transporter Room item on the top of a floor block at the center of the entrance. The prefab extends **away from you** and occupies a **5 x 5 x 4** footprint with a **3 x 3 x 2** interior. Clear the placement volume and stand back so the walls and doorway do not overlap an entity. Existing machines, inventories, unbreakable blocks, protected positions, or unsuitable obstructions can prevent placement.

Name the room item in an anvil before placement. An operator can also rename a nearby station with `/sgjatlantis transporter rename <name>`, list stations in the current dimension with `/sgjatlantis transporter list`, or obtain a room item with `/sgjatlantis transporter give`.

Removing part of a cabin can unregister it. Rebuild the room completely and check the directory if a station disappears.
