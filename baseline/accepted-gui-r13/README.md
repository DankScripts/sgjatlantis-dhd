# Permanent accepted Atlantis DHD GUI baseline (R13)

This directory freezes the user-approved Forge 1.20.1 GUI and table-control
layout from `sgjatlantis-dhd-1.20.1-2.0.3-beta.2-tabletop-r13.jar`.

The permanent baseline includes:

- every dialer button position, orientation, and size;
- the mini Pegasus gate position and scale;
- the six flat left-side control crystals and two lower control bars;
- the accepted constellation offsets;
- the crystal border-box alignment;
- the shortened right border ending before the checker-color swatches; and
- removal of the unwanted dialer/mini-gate boxes and the vertical divider.

Future work must preserve this complete GUI state unless the user explicitly
requests a change to one of those elements. Physical block-renderer changes
must be applied on top of the R13 JAR and must not replace its GUI classes,
textures, or layout defaults.

The Java defaults matching this capture live in
`AtlantisLayoutDefaults.java` and `AtlantisTableControlEditor.java`. The JSON
files here are the exact accepted F8 reference captures supplied by the user.

## SHA-256

```text
50abb382d43266590280a36185572b2c1fa7d39837e58d6a4ecb7bf14215d672  sgjatlantis-dhd-1.20.1-2.0.3-beta.2-tabletop-r13.jar
41580ed509327e2bb0dc5651ac9542cc47ac3758ef8b8d0a5fca48da1f8d5384  sgjpatch-atlantis-constellations.json
a77dd57dc2388c68000800f4cd256b7433fcf6b3839b56d8037fcbad80fbd7ef  sgjpatch-atlantis-dialer-offsets.json
64b73e8bf48706ea8ba84b02f4abb071e403e72529bbaab5fc5be41c2c275304  sgjpatch-atlantis-dialer-scaled2(8).json
4dcb15b45ea0d0aada297cf54cfe973c638dba853a5b3a12f5d4c4611be7eb84  sgjpatch-atlantis-table-control-offsets(1).json
```
