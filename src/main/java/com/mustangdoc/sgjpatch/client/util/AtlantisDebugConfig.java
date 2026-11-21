package com.mustangdoc.sgjpatch.client.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public final class AtlantisDebugConfig {
   private static final Path PATH = Paths.get("config", "sgjatlantis-dhd.toml");

   private AtlantisDebugConfig() {
   }

   public static boolean editorEnabled() {
      ensureFile();

      try {
         for (String var1 : Files.readAllLines(PATH, StandardCharsets.UTF_8)) {
            String var2 = var1.trim();
            if (!var2.isEmpty() && !var2.startsWith("#")) {
               int var3 = var2.indexOf(61);
               if (var3 >= 0) {
                  String var4 = var2.substring(0, var3).trim();
                  if (var4.equals("enableDhdEditorDebug")) {
                     String var5 = var2.substring(var3 + 1).trim().toLowerCase(Locale.ROOT);
                     int var6 = var5.indexOf(35);
                     if (var6 >= 0) {
                        var5 = var5.substring(0, var6).trim();
                     }

                     return var5.equals("true");
                  }
               }
            }
         }
      } catch (Exception var7) {
      }

      return false;
   }

   private static void ensureFile() {
      if (!Files.exists(PATH)) {
         try {
            Files.createDirectories(PATH.getParent());
            String var0 = "# Atlantis DHD debug/editor settings\n# Leave false for normal locked gameplay. Set true to enable F8 editing.\nenableDhdEditorDebug = false\n";
            Files.writeString(PATH, var0, StandardCharsets.UTF_8);
         } catch (Exception var1) {
         }
      }
   }
}
