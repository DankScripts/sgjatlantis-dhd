package com.mustangdoc.sgjpatch.client.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public final class AtlantisHoverPriorityHelper {
   private static final ThreadLocal<double[]> MOUSE = ThreadLocal.withInitial(() -> new double[]{Double.NaN, Double.NaN});

   private AtlantisHoverPriorityHelper() {
   }

   public static void setMouse(int var0, int var1) {
      double[] var2 = MOUSE.get();
      var2[0] = (double)var0;
      var2[1] = (double)var1;
   }

   public static boolean isExclusiveCurrent(Object var0) {
      double[] var1 = MOUSE.get();
      return !Double.isNaN(var1[0]) && !Double.isNaN(var1[1]) ? isExclusive(var0, var1[0], var1[1]) : fallbackHovered(var0);
   }

   public static boolean isExclusive(Object var0, double var1, double var3) {
      if (var0 != null && contains(var0, var1, var3)) {
         try {
            Object var5 = getField(var0, "screen");
            if (var5 == null) {
               return true;
            } else if (!(getField(var5, "dialerButtons") instanceof List var7)) {
               return true;
            } else {
               Object var8 = null;
               int var9 = Integer.MAX_VALUE;
               double var10 = Double.POSITIVE_INFINITY;
               int var12 = Integer.MAX_VALUE;

               for (Object var14 : var7) {
                  if (var14 != null && contains(var14, var1, var3)) {
                     int var15 = getInt(var14, "size", 2147483646);
                     int var16 = getInt(var14, "index", 2147483646);
                     double var17 = centerDistanceSquared(var14, var15, var1, var3);
                     if (var15 < var9 || var15 == var9 && var17 < var10 - 1.0E-4 || var15 == var9 && Math.abs(var17 - var10) <= 1.0E-4 && var16 < var12) {
                        var8 = var14;
                        var9 = var15;
                        var10 = var17;
                        var12 = var16;
                     }
                  }
               }

               return var8 == var0;
            }
         } catch (Throwable var19) {
            return contains(var0, var1, var3);
         }
      } else {
         return false;
      }
   }

   private static boolean contains(Object var0, double var1, double var3) {
      try {
         Method var5 = var0.getClass().getMethod("m_5953_", double.class, double.class);
         var5.setAccessible(true);
         Object var6 = var5.invoke(var0, var1, var3);
         return var6 instanceof Boolean && (Boolean)var6;
      } catch (Throwable var7) {
         return false;
      }
   }

   private static double centerDistanceSquared(Object var0, int var1, double var2, double var4) {
      try {
         int var6 = invokeInt(var0, "m_252754_");
         int var7 = invokeInt(var0, "m_252907_");
         double var8 = (double)var6 + (double)var1 / 2.0;
         double var10 = (double)var7 + (double)var1 / 2.0;
         double var12 = var2 - var8;
         double var14 = var4 - var10;
         return var12 * var12 + var14 * var14;
      } catch (Throwable var16) {
         return Double.POSITIVE_INFINITY;
      }
   }

   private static boolean fallbackHovered(Object var0) {
      try {
         Method var1 = var0.getClass().getMethod("m_198029_");
         var1.setAccessible(true);
         Object var2 = var1.invoke(var0);
         return var2 instanceof Boolean && (Boolean)var2;
      } catch (Throwable var3) {
         return false;
      }
   }

   private static Object getField(Object var0, String var1) throws ReflectiveOperationException {
      for (Class var2 = var0.getClass(); var2 != null; var2 = var2.getSuperclass()) {
         try {
            Field var3 = var2.getDeclaredField(var1);
            var3.setAccessible(true);
            return var3.get(var0);
         } catch (NoSuchFieldException var4) {
         }
      }

      throw new NoSuchFieldException(var1);
   }

   private static int getInt(Object var0, String var1, int var2) {
      try {
         Object var3 = getField(var0, var1);
         return var3 instanceof Number ? ((Number)var3).intValue() : var2;
      } catch (Throwable var4) {
         return var2;
      }
   }

   private static int invokeInt(Object var0, String var1) throws ReflectiveOperationException {
      Method var2 = var0.getClass().getMethod(var1);
      var2.setAccessible(true);
      return ((Number)var2.invoke(var0)).intValue();
   }
}
