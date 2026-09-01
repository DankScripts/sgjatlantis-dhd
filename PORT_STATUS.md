# Atlantis DHD 2.0.2-beta.2 — Minecraft 1.21.1 NeoForge

Targets:

- Minecraft 1.21.1
- NeoForge 21.1.219
- Java 21
- Stargate Journey 0.6.48-hotfix1

This release preserves the established 1.21.1 NeoForge registration,
inventory, menu, shield, energy, crystal, and standalone behavior. Its scoped
changes are the locked 720×360 Atlantis GUI and the completed Atlantis table
model/texture presentation, including live SGJourney symbols on the physical
buttons.

Validation completed for the packaged release:

- JAR ZIP integrity
- NeoForge metadata and dependency/version ranges
- Clean Java 21 compilation of every source file against the exact Minecraft
  1.21.1, NeoForge 21.1.219, and SGJourney 0.6.48-hotfix1 APIs
- Structural, stack-map-frame, and exact member-descriptor verification of every
  emitted class
- Exact locked GUI asset dimensions and R13 button offsets
- Dedicated 720×360 background rendering so the Atlantis GUI texture does not
  wrap through SGJourney's inherited 256×256 texture assumptions
- R17b's four-button center cluster restored at its locked 16×16 sizes, with
  the final accepted offsets from `sgjpatch-atlantis-dialer-scaled2(9).json`
- Pressed dialer buttons restored to the 1.20.1 GUI's translucent gold
  triangle fill, with dark constellation symbols drawn above the effect
- Center-only F8 editor gated behind
  `debugger.enableF8Editor = true` in
  `config/sgjatlantis_dhd-client.toml`; it is disabled by default for normal
  players
- Table model and texture resources in both active compatibility namespaces
- Production-name mixin support for the table model's accepted custom angles
- Correct Java 21 interface call sites for both DHD menu validity checks
- Explicit 1.21.1 menu-owned DHD references, independent of renamed SGJourney
  superclass fields
- Native NeoForge screen-constructor method references for the main and crystal
  menus, with no hand-built adapter bytecode
- Client-only screen and block-entity-renderer registration isolated through
  NeoForge's client event subscriber
- Unchanged gameplay logic for block, menu, inventory, capability, energy,
  crystal, and shield behavior

Runtime smoke-test checklist:

1. Confirm the block registers as `sgjatlantis_dhd:atlantis_dhd`.
2. Confirm the new table model renders in-world and in the inventory.
3. Open the 720×360 GUI and verify its live connected-gate symbol set.
4. Dial symbols and confirm entered symbols turn gold on the physical table.
5. Confirm shield, crystal inventory, FE, and item capability behavior remains
   identical to the prior 1.21.1 build.
