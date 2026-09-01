# Confirmed working R17b baseline

The user confirmed on 2026-08-27 that R17b works completely. This directory
freezes R17b as the permanent full-project baseline for later Atlantis DHD
Forge 1.20.1 work.

The locked release artifact is:

`sgjatlantis-dhd-1.20.1-2.0.3-beta.2-direct-resource-constellations-r17b.jar`

Future work must begin from this exact state and preserve all accepted behavior
unless the user explicitly requests a change. In particular:

- The complete R13 GUI remains immutable: artwork, constellation order, labels,
  text, colors, layout, dimensions, offsets, and control assignments must not
  change.
- Outer triangle buttons remain 42 pixels and center symbol buttons remain 21
  pixels in the accepted GUI. Physical button geometry, placement, sizing, and
  hitboxes must not move or resize.
- The physical DHD must display the actual constellation and Point-of-Origin
  artwork selected by the connected SGJourney gate variant. Never invent,
  redraw, widen, dilate, or substitute constellation shapes.
- The physical crystal overlay retains the accepted exact row-to-symbol map,
  readable cyan inactive artwork, gold entered-address state, synchronized
  center-symbol state, and Engage state.
- Right-clicking the top opens the Atlantis triangle dialer. Shift/right-click
  or side access opens the crystal interface.
- The center controls retain symbols 8 and 33, the live Point of Origin, and
  Engage/close behavior.
- Existing table geometry, shell, DHD plate, crystal panels, energy/crystal
  behavior, standalone behavior, and compatibility behavior remain unchanged.

`../accepted-gui-r13/` remains the narrower permanent GUI-layout reference.
This R17b directory supersedes earlier full-project development baselines.

The included source ZIP is the source package that produced the confirmed R17b
test artifact. Exact file hashes are recorded in `SHA256SUMS`.
