# Atlantis DHD shield-style frame — 2.0.2-beta.2-hotfix3

Based on the user's supplied Forge 1.20.1 hotfix2 JAR and source archive.

Changes are limited to the physical model and version metadata:

- Replaces both original legs and feet with the accepted shield table's smooth
  legs, feet, upper sockets and side covers. The same transform is applied to
  every connected piece, preserving the accepted foot joints.
- Replaces the side rails with the shield controller's stepped side profile.
- Replaces the old rear rail with the complete shield-table backlight and frame.
  The front of the light is behind the rear crystal row, not on top of it.
- Preserves all 283 other DHD elements exactly, including button geometry,
  control plates, crystal positions, tabletop and front trim.
- Supports both the standalone DHD and the bundled SGJ Additions override.
- Bundles all copied textures within the DHD mod; the city-shield mod is not
  required to render this table.

The new light strip is an emissive visual surface. This model-only update does
not introduce power-linked switching, power detection, or shield behavior.
Gameplay logic, GUI, packet handling and recipes are unchanged from the supplied
hotfix2 JAR. The only class-file change is the network protocol version string,
keeping exact-release enforcement aligned with the new version metadata.
Install the same release on client and server.

## Model sources

Runtime resources in `src/main/resources` are authoritative:

- `models/custom/atlantisdhd.json`: Forge composite model.
- `models/custom/atlantisdhd_controls.json`: untouched retained DHD elements.
- `models/custom/atlantis_shield_frame.obj` and `.mtl`: fitted frame and legs.
- `textures/block/shield_frame`: self-contained donor textures.

There are corresponding standalone, legacy and built-in override model roots.
The new `blockbench/atlantis_dhd_shield_frame_hotfix3.bbmodel` is an editable
free-format mesh reference of the assembled model with embedded textures.
Do not export it as a vanilla block model: the smooth legs require mesh support.
Older Blockbench files and restyling scripts are retained as historical inputs;
they do not include this frame transfer and must not overwrite the new resources.

`shield-frame-transfer-report.json` records the selected donor elements, removed
elements, transform, clearance and modified resources. The transfer tool is
included for audit; its documented input layout requires the two original JARs
and extracted source. No source-wide clean Gradle rebuild was performed for this
model-only release. The test JAR uses the supplied working class files, with only
the network version constant updated.

Validation: model references and material resources resolve locally; all retained
elements are unchanged; OBJ normals, quads and UVs are checked; JAR class hashes,
GUI assets, recipes and blockstates match the input. Software mesh previews were
reviewed from front, rear and side. Minecraft runtime testing remains necessary.
