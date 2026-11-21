package com.mustangdoc.sgjpatch.client.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConstellationOffsetStore {
   private static final Path PATH = Paths.get("config", "sgjpatch-atlantis-constellations.json");
   private static final Map<Integer, int[]> OFFSETS = new HashMap<>();
   private static boolean loaded;
   private static final Pattern ENTRY = Pattern.compile(
      "\\\"(-?\\d+)\\\"\\s*:\\s*\\{\\s*\\\"dx\\\"\\s*:\\s*(-?\\d+)\\s*,\\s*\\\"dy\\\"\\s*:\\s*(-?\\d+)\\s*\\}", 32
   );

   private ConstellationOffsetStore() {
   }

   private static synchronized void load() {
      if (!loaded) {
         loaded = true;
         if (Files.exists(PATH)) {
            try {
               String var0 = Files.readString(PATH, StandardCharsets.UTF_8);
               Matcher var1 = ENTRY.matcher(var0);

               while (var1.find()) {
                  int var2 = Integer.parseInt(var1.group(1));
                  int var3 = Integer.parseInt(var1.group(2));
                  int var4 = Integer.parseInt(var1.group(3));
                  OFFSETS.put(var2, new int[]{var3, var4});
               }
            } catch (Exception var5) {
            }
         }
      }
   }

   public static synchronized int dx(int var0) {
      load();
      int[] var1 = OFFSETS.get(var0);
      return var1 == null ? 0 : var1[0];
   }

   public static synchronized int dy(int var0) {
      load();
      int[] var1 = OFFSETS.get(var0);
      return var1 == null ? 0 : var1[1];
   }

   public static synchronized void move(int var0, int var1, int var2) {
      load();
      int[] var3 = OFFSETS.computeIfAbsent(var0, var0x -> new int[2]);
      var3[0] += var1;
      var3[1] += var2;
      save();
   }

   public static synchronized void reset(int var0) {
      load();
      OFFSETS.remove(var0);
      save();
   }

   private static void save() {
      try {
         Files.createDirectories(PATH.getParent());
         StringBuilder var0 = new StringBuilder("{\n");
         boolean var1 = true;

         for (Entry var3 : OFFSETS.entrySet().stream().sorted(Entry.comparingByKey()).toList()) {
            if (!var1) {
               var0.append(",\n");
            }

            var1 = false;
            var0.append("  \"")
               .append(var3.getKey())
               .append("\": {\n")
               .append("    \"dx\": ")
               .append(((int[])var3.getValue())[0])
               .append(",\n")
               .append("    \"dy\": ")
               .append(((int[])var3.getValue())[1])
               .append("\n")
               .append("  }");
         }

         var0.append("\n}\n");
         Files.writeString(PATH, var0.toString(), StandardCharsets.UTF_8);
      } catch (Exception var4) {
      }
   }
}
