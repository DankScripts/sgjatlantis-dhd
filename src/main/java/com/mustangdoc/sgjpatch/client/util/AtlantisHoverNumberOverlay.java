package com.mustangdoc.sgjpatch.client.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public final class AtlantisHoverNumberOverlay {
   private AtlantisHoverNumberOverlay() {
   }

   public static void renderHoveredNumber(Object var0, Object var1, int var2, int var3) {
      try {
         if (var0 == null || var1 == null) {
            return;
         }

         if (showLegacyNumbers(var0)) {
            return;
         }

         Object var4 = findBestHoveredButton(var0, var2, var3);
         if (var4 == null) {
            return;
         }

         Integer var5 = getIntField(var4, "displaySymbol");
         if (var5 == null || var5 < 0) {
            return;
         }

         String var6 = Integer.toString(var5);
         Object var7 = invokeStatic(findClass("net.minecraft.client.Minecraft"), "m_91087_", new Class[0]);
         if (var7 == null) {
            return;
         }

         Object var8 = getField(var7, "f_91062_");
         if (var8 == null) {
            return;
         }

         int var9 = var2 + 14;
         int var10 = var3 - 6;
         draw(var1, var8, var6, var9, var10, 16777215, true);
      } catch (Throwable var11) {
      }
   }

   private static boolean showLegacyNumbers(Object var0) {
      try {
         Method var1 = var0.getClass().getDeclaredMethod("sgjpatch$showLegacyNumbers");
         var1.setAccessible(true);
         Object var2 = var1.invoke(var0);
         return var2 instanceof Boolean && (Boolean)var2;
      } catch (Throwable var3) {
         return false;
      }
   }

   private static Object findBestHoveredButton(Object var0, int var1, int var2) throws Exception {
      Field var3 = var0.getClass().getDeclaredField("dialerButtons");
      var3.setAccessible(true);
      if (!(var3.get(var0) instanceof List var5)) {
         return null;
      } else {
         Object var6 = null;
         double var7 = Double.MAX_VALUE;

         for (Object var10 : var5) {
            if (var10 != null) {
               Boolean var11 = (Boolean)invoke(var10, "m_5953_", new Class[]{double.class, double.class}, (double)var1, (double)var2);
               if (Boolean.TRUE.equals(var11)) {
                  Integer var12 = (Integer)invoke(var10, "m_252754_", new Class[0]);
                  Integer var13 = (Integer)invoke(var10, "m_252907_", new Class[0]);
                  Integer var14 = getIntField(var10, "size");
                  if (var12 != null && var13 != null && var14 != null) {
                     double var15 = (double)var12.intValue() + (double)var14.intValue() / 2.0;
                     double var17 = (double)var13.intValue() + (double)var14.intValue() / 2.0;
                     double var19 = (double)var1 - var15;
                     double var21 = (double)var2 - var17;
                     double var23 = var19 * var19 + var21 * var21;
                     if (var23 < var7) {
                        var7 = var23;
                        var6 = var10;
                     }
                  }
               }
            }
         }

         return var6;
      }
   }

   private static Integer getIntField(Object var0, String var1) {
      try {
         Object var2 = getField(var0, var1);
         return var2 instanceof Integer ? (Integer)var2 : null;
      } catch (Throwable var3) {
         return null;
      }
   }

   private static Object getField(Object var0, String var1) throws Exception {
      Field var2 = var0.getClass().getDeclaredField(var1);
      var2.setAccessible(true);
      return var2.get(var0);
   }

   private static void draw(Object var0, Object var1, String var2, int var3, int var4, int var5, boolean var6) throws Exception {
      invoke(var0, "m_280056_", new Class[]{var1.getClass(), String.class, int.class, int.class, int.class, boolean.class}, var1, var2, var3, var4, var5, var6);
   }

   private static Object invoke(Object var0, String var1, Class<?>[] var2, Object... var3) throws Exception {
      Method var4 = var0.getClass().getMethod(var1, var2);
      var4.setAccessible(true);
      return var4.invoke(var0, var3);
   }

   private static Object invokeStatic(Class<?> var0, String var1, Class<?>[] var2, Object... var3) throws Exception {
      Method var4 = var0.getMethod(var1, var2);
      var4.setAccessible(true);
      return var4.invoke(null, var3);
   }

   private static Class<?> findClass(String var0) throws ClassNotFoundException {
      return Class.forName(var0);
   }
}
