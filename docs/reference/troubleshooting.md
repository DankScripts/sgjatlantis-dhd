---
layout: default
title: Troubleshooting
parent: Reference
nav_order: 3
---

# Troubleshooting

## The mod will not load

Check the Minecraft version, loader, Java version and Stargate Journey dependency against [installation]({{ '/getting-started/installation.html' | relative_url }}). Remove duplicate or wrong-loader jars, and read the first relevant dependency error in the log. Optional integrations also need compatible versions of their respective mods.

## The DHD will not dial or find a gate

Check that the gate is assembled, nearby, loaded and in the same dimension, and that the table is linked to the intended gate. Check the Crystal Controls for the required crystals and a real power source. Follow the gate's feedback; an invalid address or busy gate is not fixed by adding more crystals.

If Lantea is installed, an existing valid SGJourney link has priority over the native Lantea fallback. Use the address format belonging to the selected gate system. Pasting an address starts guided entry; it does not automatically press the symbols.

## A shield will not stay closed

Confirm that the gate iris is installed and use the table's shield control. For an SGJourney Pegasus iris, verify that the gate/table has enough sustained power for 1,200 FE/t. The gate iris and the separate City Shield are different devices.

## A transporter is missing or refuses travel

Make sure both stations are complete and in the same dimension. Enter the cabin to use the panel or map. Clear both doorways and passenger spaces, and dismount riders. Ask an operator to check `/sgjatlantis transporter list` in that dimension.

## A jumper-bay hatch will not move

Confirm that you are operating the correct fixed city opening. For the table, check its power and 50 FE operation cost. For redstone, check the port's exact height, distance from the center and signal state. Clear entities, vehicles and obstructing blocks before closing.

## A city rebuild looks stuck

Use `/sgjatlantis city status` and check the server log. Installation is batched and its rate is configurable. Do not repeatedly start a destructive rebuild as a troubleshooting shortcut; restore or keep a verified backup before intervening.

## A recipe or item is missing

Check the `SGJ Atlantis` creative tab and the loaded recipes in a recipe viewer. Recipes differ between the documented loader archives and can be affected by datapacks. See [recipes and items]({{ '/reference/recipes.html' | relative_url }}).
