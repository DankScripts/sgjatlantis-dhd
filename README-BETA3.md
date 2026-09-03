# 2.0.3-beta.3

This is the locked Forge 1.20.1 baseline accepted after in-game testing.

This release starts from the exact user-confirmed 2.0.2-beta.2-hotfix5
JAR and source. Normal right-click now opens the dialer regardless of which
table face or height was clicked. Crystal Controls opens only while the
player is sneaking (Shift + right-click). This is enforced for both the
standalone Atlantis DHD and the SGJ Additions compatibility table.

No model, texture, geometry, crystal position, button mapping, GUI layout,
power behavior, or other gameplay behavior changed. The MoreGates 4.3.2
crystallizer recipes are schema-modernized to expose their previously implicit
100 mB Liquid Naquadah input. This preserves vanilla SGJourney behavior while
allowing Almost Fluidified to replace that fluid when Naquadah unification is
explicitly enabled. Historical hotfix scripts and validation reports remain
unchanged. The supplied promotion tool reproducibly applies the narrowly
validated class-code changes to the confirmed binary baseline.

Install this JAR instead of hotfix5, not alongside it. Client and server
must use the same version because the existing handshake enforces it.
