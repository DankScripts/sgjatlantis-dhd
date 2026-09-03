# 2.0.2-beta.2-hotfix5

Based on the accepted hotfix4 JAR and source. The dedicated 16x16 backplate
texture is uniformly opaque brown. Its 21 small cuboids previously used UVs
starting at zero, including very thin border faces. All 126 faces now sample
the interior rectangle [4,4,12,12], leaving a four-pixel texture-edge margin.
This addresses atlas-edge sampling without changing shape, position, color,
face rotation or culling. It does not modify any triangular dialer button.

All three runtime/compatibility model variants and the new hotfix5 Blockbench
reference are synchronized. Runtime resource JSONs are authoritative. Older
model files and reports are retained as history, not current export inputs.
The accepted lowered flat crystals and shield-style frame are unchanged.

Validation: all non-UV model properties unchanged; all other resource bytes
unchanged; matching source/runtime resources; ZIP integrity checks. Existing
working classes are preserved except for the exact-match network version
string. No full Gradle rebuild or Minecraft runtime test was performed.

Install this JAR instead of hotfix4, not alongside it. Update the server too
where applicable, since the existing protocol requires matching versions.
