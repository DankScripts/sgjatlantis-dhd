# NeoForge 1.21.1 / 2.0.5 validation

- Java 21 compilation target: NeoForge 21.1.252 and SGJourney 0.6.55-alpha.
- Shared production geometry: forty equilateral keys, no overlapping interiors,
  four correctly subdivided centre buttons, and bounds fitting the panel.
- Rendered and inspected the layout preview with the real plate coordinates and assets.
- Lantea 0.7.7.12 native services, jumper entry points, address parser, DHD buffer,
  SymbolEncoded and Engaged event constructors checked against the installed JAR.
- Real NeoForge command, guide, and table-light packet codec round trips.
- Existing 2.0.4-hotfix1 shield and cable behavior recovered/preserved.
- All 2,009 Atlantis city templates bundled under the 1.21.1 structure path.
- City placement, SGJourney Stargate registration, hatch controls, transporters,
  and Atlantis doors compiled on NeoForge. A temporary NeoForge server reached
  the ready state with the new registrations and city resources.

Not yet verified in a live NeoForge game: city placement in the Lantea dimension,
opening the hatch and transporter screens, actual dialing and travel, multiplayer
synchronization, shield effects, and the physical model under the game's rendering
pipeline. The startup test included SGJourney but did not load optional Lantea.

Suggested in-game check: place the table beside a Lantea gate, paste a known
mw address, press each highlighted symbol, then origin and engage. Verify gate
motion and table lights. Repeat with SGJourney and check shield toggling.
