---
layout: default
title: Building and Lighting
parent: Player Guides
nav_order: 8
---

# Building and Lighting

The `SGJ Atlantis` creative tab contains themed building blocks and finishes, stairs and slabs, lighting pieces, floor conduits, doors, and consoles. Availability and recipes vary by loader and installed datapacks; see [recipes and obtaining items]({{ '/reference/recipes.html' | relative_url }}).

## Building palette

The palette includes colored Ancient blocks and walls, pillars, wall panels, clamps and posts, metal and stone blocks, stairs and slabs. Alloy, recessed, ribbed, inset, service, illuminated-seam and seamless panels come in silver, bronze, copper, blue-grey, teal and charcoal finishes. Floors and matching floor conduits also include a stair-grey variant.

Door kits and transporter rooms have matching colored versions. Lights include wall and ceiling lights, illuminated stairs, inscribed stairs and slabs, and powered variants of several pieces. Check the actual creative tab for the exact pieces available in your build.

## Inscribed lights

The proximity behavior applies to **non-powered inscribed stairs and slabs**, not to every Atlantis light. It checks for nearby players once per second, within **8 blocks horizontally and 4 vertically**. After detection, the light remains active for the configured hold time; `inscribedLightHoldSeconds` defaults to **90 seconds**. Powered lights are a separate case.

Powered fixtures form groups through adjacent, loaded powered fixtures. Each group checks power once per second and uses **1 FE per second for each started group of 256 fixtures**; the group has a short, approximately two-second buffer. Supply compatible power to the group. When there is not enough power for its upkeep, the group goes dark.

## Consoles

The Database Console is a decorative multi-block piece, **not a functional information database**. Its placement item checks a **5 x 3** area and three blocks of headroom, along with permissions, entities and liquids. There is also a separate decorative console. If placement fails, clear the whole required space and check build permissions.

Floor conduits look like building pieces but forward energy when connected; they do not store it. See [ZPMs and power]({{ '/guides/power.html' | relative_url }}).
