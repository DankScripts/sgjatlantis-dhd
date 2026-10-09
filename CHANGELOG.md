# Changelog

## 2.0.4

- Promotes the SGJourney 0.6.50 compatibility hotfix to a release build.
- Preserves the current GUI, table assets, crystal requirements, and native
  dialing callbacks; the missing-crystal report was resolved by the user.
- Adds release JAR and source ZIP packaging through `./gradlew release`.

## SGJourney 0.6.50 Compatibility Hotfix

- Rebuilt NeoForge 2.0.3-beta.3 against SGJourney 0.6.50 and require that version.
- Updated DHD/crystal-menu packages, synced energy capacity, block interactions,
  native symbol/engage callbacks, Pegasus button types, and mini-gate accessors.
- Preserved the 720x360 dialer background and existing model/texture assets.
- Center engage closes the full container only after six regular symbols;
  Escape closes normally without the address requirement.
- Namespaced the model-angle mixin helper to coexist with City Shield 1.0.2.
- Combined client startup/resource reload passed with SGJourney 0.6.50, City
  Shield 1.0.2, JEI, and JEI++. In-game dialing/power checks remain unconfirmed.

## 2.0.3-beta.3 — Minecraft 1.21.1 NeoForge

- Updated the physical Atlantis DHD table to the current accepted model.
- Changed interaction routing so normal right-click always opens the dialer
  and sneak-right-click opens Crystal Controls.
- Added the minimum dialing crystal set: one Large Control Crystal, two
  Advanced Energy Crystals, and one Advanced Transfer Crystal.
- Kept Advanced Communication Crystals optional.
- Preserved native SGJourney support for all compatible Pegasus crystal types.
- Added a conditional NeoForge capability proxy on the decorative floor tile
  directly above a Large Naquadah Cable. It exposes only the nearby Atlantis
  DHD to SGJourney's native cable output path, including zero-point energy.
- Preserved the established NeoForge 1.21.1 GUI, menus, shield controls,
  capabilities, symbol display, and migration compatibility.
