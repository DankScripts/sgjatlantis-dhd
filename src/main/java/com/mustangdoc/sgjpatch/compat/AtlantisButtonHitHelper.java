package com.mustangdoc.sgjpatch.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.client.gui.components.AbstractButton;

public final class AtlantisButtonHitHelper {
   private AtlantisButtonHitHelper() {
   }

   public static boolean contains(AbstractButton var0, double var1, double var3) {
      int var5 = var0.getX();
      int var6 = var0.getY();
      int var7 = getInt(var0, "size");
      int var8 = getInt(var0, "actionSymbol");
      boolean var9 = getBoolean(var0, "up");
      return !containsRaw(var5, var6, var7, var8, var9, var1, var3) ? false : var7 < 40 || !overlapsSmallCenterButton(var0, var1, var3, var7);
   }

   private static boolean overlapsSmallCenterButton(Object var0, double var1, double var3, int var5) {
      try {
         Field var6 = var0.getClass().getDeclaredField("screen");
         var6.setAccessible(true);
         Object var7 = var6.get(var0);
         if (var7 == null) {
            return false;
         }

         Field var8 = var7.getClass().getDeclaredField("dialerButtons");
         var8.setAccessible(true);
         Object var9 = var8.get(var7);
         if (!(var9 instanceof List)) {
            return false;
         }

         for (Object var12 : (List)var9) {
            if (var12 != null && var12 != var0) {
               int var13 = getInt(var12, "size");
               if (var13 > 0 && var13 <= 24 && var13 < var5) {
                  int var14 = invokeInt(var12, "m_252754_");
                  int var15 = invokeInt(var12, "m_252907_");
                  int var16 = getInt(var12, "actionSymbol");
                  boolean var17 = getBoolean(var12, "up");
                  if (containsRaw(var14, var15, var13, var16, var17, var1, var3)) {
                     return true;
                  }
               }
            }
         }
      } catch (ReflectiveOperationException var18) {
      }

      return false;
   }

   private static int invokeInt(Object var0, String var1) throws ReflectiveOperationException {
      Method var2 = var0.getClass().getMethod(var1);
      var2.setAccessible(true);
      return ((Number)var2.invoke(var0)).intValue();
   }

   private static boolean containsRaw(int var0, int var1, int var2, int var3, boolean var4, double var5, double var7) {
      double var9 = var5 - (double)var0;
      double var11 = var7 - (double)var1;
      int var13 = var3 != 28 && var3 != 32 ? 0 : 5;
      return var4 ? pointInUpTriangleInset(var9, var11, var2, var13) : pointInDownTriangleInset(var9, var11, var2, var13);
   }

   private static int getInt(Object var0, String var1) {
      try {
         Field var2 = var0.getClass().getDeclaredField(var1);
         var2.setAccessible(true);
         return var2.getInt(var0);
      } catch (ReflectiveOperationException var3) {
         return 0;
      }
   }

   private static boolean getBoolean(Object var0, String var1) {
      try {
         Field var2 = var0.getClass().getDeclaredField(var1);
         var2.setAccessible(true);
         return var2.getBoolean(var0);
      } catch (ReflectiveOperationException var3) {
         return false;
      }
   }

   private static boolean pointInUpTriangleInset(double var0, double var2, int var4, int var5) {
      var0 -= 1.0 + (double)var5;
      var2 -= 1.0 + (double)var5;
      var4 -= 2 + var5 * 2;
      if (var4 <= 2) {
         return false;
      } else {
         double var6 = (double)var4 - 1.0;
         if (!(var2 < 0.0) && !(var2 > var6)) {
            double var8 = ((double)var4 - 1.0) / 2.0;
            double var10 = var2 / var6 * var8;
            return Math.abs(var0 - var8) <= var10;
         } else {
            return false;
         }
      }
   }

   private static boolean pointInDownTriangleInset(double var0, double var2, int var4, int var5) {
      var0 -= 1.0 + (double)var5;
      var2 -= 1.0 + (double)var5;
      var4 -= 2 + var5 * 2;
      if (var4 <= 2) {
         return false;
      } else {
         double var6 = (double)var4 - 1.0;
         if (!(var2 < 0.0) && !(var2 > var6)) {
            double var8 = ((double)var4 - 1.0) / 2.0;
            double var10 = (1.0 - var2 / var6) * var8;
            return Math.abs(var0 - var8) <= var10;
         } else {
            return false;
         }
      }
   }
}
