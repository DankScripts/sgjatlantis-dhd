package com.mustangdoc.sgjpatch.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class SGJCompatibilityConfig {
   private static final String FILE_NAME = "sgjatlantis-dhd.toml";
   private static final String KEY = "useOriginalSGJAdditionsDHD";
   private static final Pattern GAME_DIR_PATTERN = Pattern.compile("--gameDir\\s+(?:\\\"([^\\\"]+)\\\"|(.+?))(?=\\s+--[A-Za-z0-9_.-]+|$)");
   private static final Path GAME_DIR = resolveGameDir();
   private static final Path CONFIG_PATH = GAME_DIR.resolve("config").resolve("sgjatlantis-dhd.toml");
   private static final boolean SGJ_ADDITIONS_PRESENT = detectSGJAdditions();
   private static final boolean USE_ORIGINAL = loadOrCreate();

   private SGJCompatibilityConfig() {
   }

   public static boolean shouldApplyMixin(String var0, String var1) {
      if (!SGJ_ADDITIONS_PRESENT || !USE_ORIGINAL || var1 == null) {
         return true;
      } else {
         return var1.endsWith("AtlantisDHDModelAngleMixin")
            ? true
            : !var1.endsWith("SGJAdditionsLegacyClientSetupMixin")
               && !var1.endsWith("SGJAdditionsClientSetupBypassMixin")
               && !var1.endsWith("ClientPacketListenerOpenScreenMixin")
               && !var1.endsWith("MenuScreensCreateMixin")
               && !var1.endsWith("PlayMessagesOpenContainerMixin")
               && !var1.endsWith("SGJourneyBigButtonIlluminationMixin")
               && !var1.endsWith("AtlantisDHDBlockMixin");
      }
   }

   public static boolean useOriginalSGJAdditionsDHD() {
      return SGJ_ADDITIONS_PRESENT && USE_ORIGINAL;
   }

   public static Path configPath() {
      return CONFIG_PATH;
   }

   private static boolean loadOrCreate() {
      try {
         Files.createDirectories(CONFIG_PATH.getParent());
         if (!Files.exists(CONFIG_PATH)) {
            Files.writeString(CONFIG_PATH, fullDefaults(), StandardCharsets.UTF_8);
            System.out.println("[SGJPATCH] Created config " + CONFIG_PATH);
         } else if (!containsKey(CONFIG_PATH, "useOriginalSGJAdditionsDHD")) {
            String var0 = "\n# SGJ Additions Atlantis DHD compatibility\n# false (default): use this mod's upgraded Atlantis DHD.\n# true: when SGJ Additions is installed, use its original Atlantis DHD.\n# Restart Minecraft after changing this setting.\nuseOriginalSGJAdditionsDHD = false\n";
            Files.writeString(CONFIG_PATH, var0, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            System.out.println("[SGJPATCH] Updated existing config with useOriginalSGJAdditionsDHD | config=" + CONFIG_PATH);
         }

         for (String var2 : Files.readAllLines(CONFIG_PATH, StandardCharsets.UTF_8)) {
            String var3 = var2.trim();
            if (!var3.isEmpty() && !var3.startsWith("#")) {
               int var4 = var3.indexOf(35);
               if (var4 >= 0) {
                  var3 = var3.substring(0, var4).trim();
               }

               int var5 = var3.indexOf(61);
               if (var5 >= 0) {
                  String var6 = var3.substring(0, var5).trim();
                  if ("useOriginalSGJAdditionsDHD".equalsIgnoreCase(var6)) {
                     String var7 = var3.substring(var5 + 1).trim().toLowerCase(Locale.ROOT);
                     if (var7.equals("true")) {
                        announce(true);
                        return true;
                     }

                     if (var7.equals("false")) {
                        announce(false);
                        return false;
                     }
                  }
               }
            }
         }
      } catch (Throwable var8) {
         System.out.println("[SGJPATCH] Could not read/update " + CONFIG_PATH + "; using upgraded Atlantis DHD. " + var8);
         return false;
      }

      announce(false);
      return false;
   }

   private static boolean containsKey(Path var0, String var1) throws IOException {
      for (String var3 : Files.readAllLines(var0, StandardCharsets.UTF_8)) {
         String var4 = var3.trim();
         if (!var4.isEmpty() && !var4.startsWith("#")) {
            int var5 = var4.indexOf(35);
            if (var5 >= 0) {
               var4 = var4.substring(0, var5).trim();
            }

            int var6 = var4.indexOf(61);
            if (var6 >= 0 && var1.equalsIgnoreCase(var4.substring(0, var6).trim())) {
               return true;
            }
         }
      }

      return false;
   }

   private static String fullDefaults() {
      return "# Atlantis DHD settings\n#\n# Leave false for normal locked gameplay. Set true only to enable F8 editing.\nenableDhdEditorDebug = false\n\n# SGJ Additions Atlantis DHD compatibility\n# false (default): use this mod's upgraded Atlantis DHD.\n# true: when SGJ Additions is installed, use its original Atlantis DHD.\n# Restart Minecraft after changing this setting.\nuseOriginalSGJAdditionsDHD = false\n";
   }

   private static void announce(boolean var0) {
      if (SGJ_ADDITIONS_PRESENT) {
         System.out.println("[SGJPATCH] SGJ Additions Atlantis DHD mode: " + (var0 ? "ORIGINAL" : "UPGRADED (default)") + " | config=" + CONFIG_PATH);
      } else {
         System.out.println("[SGJPATCH] SGJ Additions not detected; standalone Atlantis DHD mode active. | config=" + CONFIG_PATH);
      }
   }

   private static boolean detectSGJAdditions() {
      Path var0 = GAME_DIR.resolve("mods");
      if (!Files.isDirectory(var0)) {
         return false;
      } else {
         try {
            boolean var2;
            try (Stream<Path> var1 = Files.list(var0)) {
               var2 = var1.anyMatch(
                  var0x -> {
                     String var1x = var0x.getFileName().toString().toLowerCase(Locale.ROOT);
                     return var1x.startsWith("sgjadditions-")
                        && !var1x.startsWith("sgjadditions_capacity_patch")
                        && (var1x.endsWith(".jar") || Files.isDirectory(var0x));
                  }
               );
            }

            return var2;
         } catch (IOException var6) {
            return false;
         }
      }
   }

   private static Path resolveGameDir() {
      try {
         String var0 = System.getProperty("sun.java.command", "");
         Matcher var1 = GAME_DIR_PATTERN.matcher(var0);
         if (var1.find()) {
            String var2 = var1.group(1) != null ? var1.group(1) : var1.group(2);
            if (var2 != null && !var2.isBlank()) {
               return Paths.get(var2.trim()).toAbsolutePath().normalize();
            }
         }
      } catch (Throwable var3) {
      }

      return Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
   }
}
