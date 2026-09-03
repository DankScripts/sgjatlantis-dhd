# 2.0.2-beta.2-hotfix4

Based on the user-accepted hotfix3 JAR and source. Only the six flat-crystal
Y positions and release identifiers change. The rear row moves down 0.30
model pixels and the front row moves down 0.225; both keep half of their
previous visible clearance above their respective deck surfaces.

The shield-style sides, legs, feet, backlight, triangular dialer, GUI,
recipes, and gameplay methods remain unchanged. The network version string
is updated to enforce the same version on client and server.

Runtime model JSONs and the new hotfix4 Blockbench reference are synchronized.
Older model files/reports are retained as history, not current export inputs.
No full Gradle rebuild or Minecraft runtime test was performed; this resource
patch preserves the working class files except for the protocol version string.
