# Atlantis DHD Upgrade 2.0.5

Source overlay for the released Minecraft 1.20.1 Forge build. This repository
does not include the NeoForge port, Atlantis City Shield, or Destiny Technology.

## Build

Use Java 17 and the checked-in Gradle wrapper. Obtain these inputs separately:

- Stargate Journey 1.20.1 0.6.55-alpha (`SGJOURNEY_JAR`).
- The matching Forge Lantea and JEI JARs (`LANTEA_JAR` and `JEI_JAR`).
- The original frozen Atlantis DHD overlay build input:
  `baseline/2.0.5-after-moregates-removal/sgjatlantis-dhd-1.20.1-2.0.5.jar`.
  This is not the final 2.0.5 release JAR.

Run `.\gradlew.bat verifyReleaseJar` in PowerShell. The finished mod JAR
is written to `dist\`. This is an overlay build, so the checked-in Java
source alone cannot produce the complete release without the frozen input.
