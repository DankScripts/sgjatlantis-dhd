# Locked 2.0.3-beta.3 baseline

This source tree corresponds to the user-tested and accepted Forge 1.20.1
`2.0.3-beta.3` release baseline locked on 2026-09-03. The exact accepted JAR,
checksum, and preservation contract are stored under
`baseline/accepted-balanced-bars/`.

The baseline includes:

- the confirmed concealed Large Naquadah Cable power bridge;
- synchronized Crystal Controls proxy positioning;
- normal-click dialer and sneak-click Crystal Controls behavior;
- complete table delumination when the DHD has no stored energy;
- a powered rear-lightbar glow alpha of `0.50`;
- triangle-button face UVs inset to `[4,4,12,12]` on the uniform teal sprite,
  preventing neighboring atlas colors from bleeding onto narrow button sides;
- all accepted models, textures, GUI positions, mappings, and visual assets;
- 30 MoreGates 4.3.2 crystallizer recipe overrides generated in the
  top-priority compatibility pack, exposing the previously implicit 100 mB
  SGJourney Liquid Naquadah input for optional Almost Fluidified 0.1.8
  unification.

The MoreGates integration does not add a hard dependency on MoreGates,
Almost Fluidified, or GregTech. If Naquadah is not explicitly enabled in
Almost Fluidified's configuration, the recipes retain SGJourney's original
Liquid Naquadah input.

Locked release JAR SHA-256:

`E2ECCB4E0917995FFC51B86E69D922B117CA514BE561A7A5AAEE28D899D337CF`

Future releases must use the accepted balanced-bars JAR as their direct binary
foundation. A successful build is not a new accepted baseline until it passes
in-game testing and receives explicit user acceptance.
