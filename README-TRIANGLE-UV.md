# Triangle button UV correction

This accepted baseline fix addresses neighboring texture-atlas colors appearing
on the narrow sides of the physical Atlantis DHD triangle buttons.

The triangle texture is a uniformly opaque 16x16 `#04A2A1` teal sprite. All
1,458 triangle faces in each of the three runtime model copies previously used
UV ranges beginning at `(0,0)`, including side-face spans smaller than half a
texel. Those ranges could sample across the sprite boundary under filtering and
mipmapping.

All triangle faces now use the interior UV rectangle `[4,4,12,12]`. Because the
sprite is a solid color, this does not alter intended artwork. Geometry, texture
assignments, top constellation overlays, encoded gold overlays, and every
non-triangle face remain unchanged. See `triangle-uv-validation.json` for the
structured scope audit.

The correction was confirmed in game and promoted into the accepted baseline on
2026-09-03.