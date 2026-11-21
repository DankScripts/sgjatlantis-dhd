# Building NeoForge 1.21.1 / 2.0.5

Use Java 21 and the included Gradle wrapper. The project targets NeoForge 21.1.252.
The exact SGJourney 0.6.55-alpha was privately supplied by its author, not
published on Maven. Place the matching NeoForge 1.21.1 JAR at
`libs/Stargate.Journey-1.21.1-0.6.55-alpha.jar`, or set `SGJOURNEY_JAR` to its
absolute path. Third-party mods are not bundled in the source archive.

Run `.\gradlew.bat clean release` in PowerShell. Both the installable
JAR and complete source ZIP are written to `dist\`. The release check rejects
packaged mod classes without Java source; no preserved Atlantis binary is used.

Gradle downloads JEI 19.27.0.336 and NeoForge Lantea 0.7.7.12 from Maven.
`LANTEA_JAR` is an optional explicit override for an exact supplied JAR.
No dependency defaults to a Minecraft installation or a home-directory path.
Run `.\gradlew.bat verifyPort` to check without packaging.
This checks production keypad proportions, the actual installed native API
signatures, and command/guide/table-light packet serialization. The rich-text
state packet needs a fully loaded Minecraft registry context and is not covered
by this standalone check.

`python tools/verify_layout.py` checks all forty click shapes and creates
`build/neoforge-layout-preview.png`. It needs Pillow and Java 21; adjust the
JAVA path at the top for another machine. The preview is a geometry/asset
inspection, not an in-game screenshot.

Source recovery: the saved 1.21.1-neoforge branch was 2.0.4. Shield hotfix sources
were recovered from the user's installed 2.0.4-hotfix1 JAR to retain that behavior.
