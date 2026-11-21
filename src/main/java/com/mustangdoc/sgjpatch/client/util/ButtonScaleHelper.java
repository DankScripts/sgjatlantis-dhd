package com.mustangdoc.sgjpatch.client.util;

public final class ButtonScaleHelper {
   private ButtonScaleHelper() {
   }

   public static int remapSize(int var0) {
      if (var0 >= 20) {
         return 42;
      } else {
         return var0 >= 16 ? 21 : var0;
      }
   }
}
