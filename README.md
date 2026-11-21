# Atlantis DHD Upgrade 2.0.5

## Stargate Atlantis registry migration

Atlantis blocks, items and block entities now use `stargate_atlantis`, owned by
the Stargate Atlantis mod container. The legacy container remains for existing
integration. Saved-ID remapping and structure-NBT conversion preserve the old
Atlantis IDs. Use the matching City Shield 1.0.4 release. Back up existing worlds
before upgrading; third-party datapacks with direct old tag entries may need updates.

Source overlay for Atlantis DHD Upgrade on Minecraft 1.20.1 and Forge 47.4.16.
This branch contains only the Forge edition of Atlantis DHD.

The DHD entity and iris packet were reconstructed from the accepted binary
and compatibility mixins; they are not recovered original source. DHD guards
and self-targeting behavior have been moved into their corresponding classes
or event subscribers. Third-party mixins and a compatibility coremod remain.
The frozen binary input is still required for classes without recovered source.

## Bundled Atlantis city

The 2,009 city chunks use the server backup for the main city and lower rooms,
with the later Farlands control-room layout, database console, moved tables,
and decorative console. Genetics Lab, Lower Spire, and the underwater bay
appear on the transporter LOWER page; outer towers remain on CITY. The client
and server both need the updated JAR for the screen change.

Use the matching Forge City Shield release for the city preset at the new
controller position. Replacing the JAR does not rebuild an installed city.
Back up the Lantea dimension before using `/sgjatlantis city install`.

The jumper bay table refreshes its large naquadah cable feeds when it loads
or is opened with less than 50 FE, including feeds connected through its
floor conduit. It still requires a powered feed.

## Build

Use Java 17 and the checked-in Gradle wrapper. Obtain these inputs separately:

- Stargate Journey 1.20.1 0.6.55-alpha (`SGJOURNEY_JAR`).
- Forge Lantea for Minecraft 1.20.1 (`LANTEA_JAR`) and JEI
  15.20.0.116 (`JEI_JAR`).
- The frozen Atlantis DHD build input from the original Forge 2.0.5 source
  archive. If you don't have that archive, request the input from the
  maintainer. Place it at
  `baseline/2.0.5-after-moregates-removal/sgjatlantis-dhd-1.20.1-2.0.5.jar`.
  This is not the final 2.0.5 release JAR, despite the identical filename.
  Verify its SHA-256 before building:
  `DCA6FA2CAA8B2CCDA3037AE2BD6C4F6BFAC6BBED1C8E400C638F1BE328B5D760`.

Run `.\gradlew.bat verifyReleaseJar` in PowerShell. The finished mod JAR
is written to `dist\`. This is an overlay build, so the checked-in Java
source alone cannot produce the complete release without the frozen input.

## Repository layout

- [`src/main`](src/main): current Java source and game resources.
- [`tools/tests`](tools/tests): focused build-time tests.
- [`blockbench`](blockbench): editable model assets.

Older build archives and migration experiments are available in Git history,
not in the current branch checkout.
