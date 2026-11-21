# Atlantis DHD Upgrade 2.0.5

## Stargate Atlantis registry migration

Atlantis blocks, items and block entities now use `stargate_atlantis`, owned by
the Stargate Atlantis mod container. The legacy container remains for existing
integration. Saved-ID remapping and structure-NBT conversion preserve the old
Atlantis IDs. Use the matching City Shield 1.0.4 release. Back up existing worlds
before upgrading; third-party datapacks with direct old tag entries may need updates.

Source project for Atlantis DHD Upgrade on Minecraft 1.20.1 and Forge 47.4.16.
This branch contains only the Forge edition of Atlantis DHD.

Previously missing required classes and resources have been reconstructed from
the accepted release. Reconstructed Java is not recovered original source.
All packaged mod classes are now compiled from this source tree; no frozen
Atlantis binary is required or merged into the release. Unregistered legacy
mixins from the old binary are not packaged. Third-party compatibility mixins
and the explicitly registered compatibility coremod remain.

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

Use Java 17 and the checked-in Gradle wrapper. Gradle downloads Forge,
JEI 15.20.0.116 and Lantea 0.7.7.12 from their configured repositories.
`LANTEA_JAR` remains an explicit override for validating an exact supplied JAR.
No dependency defaults to a Minecraft installation or a home-directory path.

SGJourney 0.6.55-alpha was privately supplied by its author and is not a public
Maven release. Obtain the Forge 1.20.1 alpha from the author, then set
`SGJOURNEY_JAR` to that JAR, or place it at
`libs/Stargate.Journey-1.20.1-0.6.55-alpha.jar`. Do not substitute the published
0.6.54 release. Update the dependency and verify the API when the public beta
becomes available; the alpha and beta are not assumed interchangeable.

Run `.\gradlew.bat clean release` in PowerShell. The finished mod JAR and
source ZIP are written to `dist\`. The release check also rejects packaged
mod classes without corresponding Java source.

The floor conduit's `Network` is a loaded-block routing cache, not saved energy
or a persistent network identifier. Chunk/world load rebuilds it from floor
blocks; connected hubs and native SGJourney cable networks own persistence.

## Repository layout

- [`src/main`](src/main): current Java source and game resources.
- [`tools/tests`](tools/tests): focused build-time tests.
- [`blockbench`](blockbench): editable model assets.

Older build archives and migration experiments are available in Git history,
not in the current branch checkout.
