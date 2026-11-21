package com.mustangdoc.sgjpatch.client.util;

public final class EditModeTextHelper {
   private EditModeTextHelper() {
   }

   public static String selectedLabel(int var0) {
      if (var0 == 1000) {
         return "Selected: Circle";
      } else if (var0 == 38) {
         return "Selected: Center Blank";
      } else {
         return var0 == 39 ? "Selected: Center Engage" : "Selected: Symbol " + (var0 + 1);
      }
   }

   public static String selectedDetails(String var0, int var1, int var2, boolean var3) {
      return var0 + " | dx=" + var1 + ", dy=" + var2 + ", flip=" + var3;
   }
}
