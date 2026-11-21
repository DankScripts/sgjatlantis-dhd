package com.mustangdoc.sgjpatch.client.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class AtlantisMiniGateOverlay {
   private static final int GATE_SIZE = 118;
   private static final int GATE_OFFSET = 32;
   private static final int ORB_SIZE = 74;
   private static final int ORB_OFFSET = 10;
   private static final String ROOT = "textures/gui/mini_gate/early_port/";
   private static final String ORB_OFF = "textures/gui/atlantis_shield_button_off_large.png";
   private static final String ORB_ON = "textures/gui/atlantis_shield_button_on_large.png";

   private AtlantisMiniGateOverlay() {
   }

   public static void render(Object var0, Object var1) {
      try {
         if (var0 == null || var1 == null) {
            return;
         }

         Object var2 = getField(var0, "circleButton");
         if (var2 == null) {
            return;
         }

         Integer var3 = callInt(var2, "m_252754_");
         Integer var4 = callInt(var2, "m_252907_");
         if (var3 == null || var4 == null) {
            return;
         }

         int var5 = var3 - 32;
         int var6 = var4 - 32;
         blit(var1, "textures/gui/mini_gate/early_port/gate_base.png", var5, var6);
         Object var7 = resolveLinkedStargate(var0);
         if (var7 == null || !isPegasus(var7)) {
            blit(var1, "textures/gui/mini_gate/early_port/idle_symbols.png", var5, var6);
            renderOrb(var0, var1, var3, var4);
            return;
         }

         Object var8 = invokeNoArgQuiet(var7, "getAddress");
         Object var9 = getFieldQuiet(var7, "addressBuffer");
         Object var10 = invokeNoArgQuiet(var7, "getEncodedSymbols");
         int var11 = clamp(addressLength(var8), 0, 9);
         int var12 = Math.max(var11, addressLength(var9));
         boolean var13 = callBoolean(var7, "isSymbolSpinning", false);
         boolean var14 = callBoolean(var7, "isConnected", false);
         if (var12 <= 0 && var11 <= 0 && !var13 && !var14) {
            blit(var1, "textures/gui/mini_gate/early_port/idle_symbols.png", var5, var6);
         }

         for (int var15 = 0; var15 < var11; var15++) {
            int var16 = symbolAt(var10, var15);
            if (var16 >= 0 && var16 <= 38) {
               int var17 = chevronPosition(var7, var15 + 1);
               if (var17 >= 0) {
                  var17 = mirrorPosition(floorMod(var17, 36));
                  blit(var1, symbolTexture(var16, var17), var5, var6);
               }
            }
         }

         if (var13) {
            int var21 = fieldInt(var7, "symbolBuffer", var11);
            int var23 = symbolAt(var10, var21);
            if (var23 < 0 || var23 > 38) {
               var23 = symbolAt(var9, var21);
            }

            int var26 = callIntDefault(var7, "getCurrentSymbol", -1);
            if (var23 >= 0 && var23 <= 38 && var26 >= 0) {
               int var18 = floorMod(var26, 36);
               if (!isLockedPosition(var7, var11, var18)) {
                  int var19 = mirrorPosition(var18);
                  blit(var1, symbolTexture(var23, var19), var5, var6);
               }
            }
         }

         for (int var22 = 0; var22 < var11; var22++) {
            int var24 = chevronPosition(var7, var22 + 1);
            if (var24 >= 0) {
               int var27 = mirrorPosition(floorMod(var24, 36));
               int var28 = floorMod(Math.floorDiv(var27, 4), 9);
               blit(var1, "textures/gui/mini_gate/early_port/chevron_slot_" + var28 + ".png", var5, var6);
            }
         }

         renderOrb(var0, var1, var3, var4);
      } catch (Throwable var20) {
      }
   }

   private static int mirrorPosition(int var0) {
      return floorMod(-var0, 36);
   }

   private static void renderOrb(Object var0, Object var1, int var2, int var3) {
      try {
         boolean var4 = shieldActive(var0);
         blit(var1, var4 ? "textures/gui/atlantis_shield_button_on_large.png" : "textures/gui/atlantis_shield_button_off_large.png", var2 - 10, var3 - 10, 74);
      } catch (Throwable var5) {
      }
   }

   private static boolean shieldActive(Object var0) {
      try {
         Class var1 = Class.forName("com.mustangdoc.sgjpatch.compat.AtlantisShieldIndicatorHelper");

         for (Method var5 : var1.getDeclaredMethods()) {
            if (var5.getName().equals("isShieldActive") && var5.getParameterCount() == 1 && var5.getParameterTypes()[0].isAssignableFrom(var0.getClass())) {
               var5.setAccessible(true);
               Object var6 = var5.invoke(null, var0);
               return var6 instanceof Boolean && (Boolean)var6;
            }
         }
      } catch (Throwable var7) {
      }

      return false;
   }

   private static boolean isLockedPosition(Object var0, int var1, int var2) {
      for (int var3 = 0; var3 < var1; var3++) {
         int var4 = chevronPosition(var0, var3 + 1);
         if (var4 >= 0 && floorMod(var4, 36) == var2) {
            return true;
         }
      }

      return false;
   }

   private static String symbolTexture(int var0, int var1) {
      return "textures/gui/mini_gate/early_port/" + String.format("s%02d_p%02d.png", var0, var1);
   }

   private static Object resolveLinkedStargate(Object var0) {
      try {
         Object var1 = invokeNoArg(var0, "m_6262_");
         if (var1 == null) {
            return null;
         }

         Object var2 = invokeNoArg(var1, "sgjpatch$getBlockEntity");
         if (var2 == null) {
            return null;
         }

         Class var3 = Class.forName("com.mustangdoc.sgjpatch.util.DhdLookup");

         for (Method var7 : var3.getDeclaredMethods()) {
            if (var7.getName().equals("resolveStargate") && var7.getParameterCount() == 1) {
               Class var8 = var7.getParameterTypes()[0];
               if (var8.isAssignableFrom(var2.getClass())) {
                  var7.setAccessible(true);
                  return var7.invoke(null, var2);
               }
            }
         }
      } catch (Throwable var9) {
      }

      return null;
   }

   private static boolean isPegasus(Object var0) {
      for (Class var1 = var0.getClass(); var1 != null; var1 = var1.getSuperclass()) {
         if ("net.povstalec.sgjourney.common.block_entities.stargate.PegasusStargateEntity".equals(var1.getName())
            || var1.getName().endsWith(".PegasusStargateEntity")) {
            return true;
         }
      }

      return false;
   }

   private static int chevronPosition(Object var0, int var1) {
      try {
         Method var2 = findMethod(var0.getClass(), "getChevronPosition", int.class);
         if (var2 != null) {
            Object var3 = var2.invoke(var0, var1);
            if (var3 instanceof Number) {
               int var4 = ((Number)var3).intValue();
               if (var4 >= 0) {
                  return var4;
               }
            }
         }
      } catch (Throwable var6) {
      }

      try {
         if (invokeNoArg(var0, "getEngagedChevrons") instanceof int[] var8) {
            int var9 = var1 - 1;
            if (var9 >= 0 && var9 < var8.length) {
               return var8[var9] * 4;
            }
         }
      } catch (Throwable var5) {
      }

      return -1;
   }

   private static int symbolAt(Object var0, int var1) {
      if (var0 != null && var1 >= 0) {
         try {
            Method var2 = findMethod(var0.getClass(), "symbolAt", int.class);
            if (var2 == null) {
               return -1;
            } else {
               Object var3 = var2.invoke(var0, var1);
               return var3 instanceof Number ? ((Number)var3).intValue() : -1;
            }
         } catch (Throwable var4) {
            return -1;
         }
      } else {
         return -1;
      }
   }

   private static int addressLength(Object var0) {
      if (var0 == null) {
         return 0;
      } else {
         try {
            Method var1 = findNoArgMethod(var0.getClass(), "getLength");
            if (var1 == null) {
               return 0;
            } else {
               Object var2 = var1.invoke(var0);
               return var2 instanceof Number ? ((Number)var2).intValue() : 0;
            }
         } catch (Throwable var3) {
            return 0;
         }
      }
   }

   private static void blit(Object var0, String var1, int var2, int var3) throws Exception {
      blit(var0, var1, var2, var3, 118);
   }

   private static void blit(Object var0, String var1, int var2, int var3, int var4) throws Exception {
      Class var5 = Class.forName("net.minecraft.resources.ResourceLocation");
      Method var6 = var5.getMethod("fromNamespaceAndPath", String.class, String.class);
      Object var7 = var6.invoke(null, "sgjpatch", var1);
      Method var8 = null;

      for (Method var12 : var0.getClass().getMethods()) {
         if (var12.getName().equals("m_280163_") && var12.getParameterCount() == 9) {
            Class[] var13 = var12.getParameterTypes();
            if (var13.length == 9 && var13[0].isAssignableFrom(var5)) {
               var8 = var12;
               break;
            }
         }
      }

      if (var8 != null) {
         var8.setAccessible(true);
         var8.invoke(var0, var7, var2, var3, 0.0F, 0.0F, var4, var4, var4, var4);
      }
   }

   private static Object getFieldQuiet(Object var0, String var1) {
      try {
         return getField(var0, var1);
      } catch (Throwable var3) {
         return null;
      }
   }

   private static Object getField(Object var0, String var1) throws Exception {
      for (Class var2 = var0.getClass(); var2 != null; var2 = var2.getSuperclass()) {
         try {
            Field var3 = var2.getDeclaredField(var1);
            var3.setAccessible(true);
            return var3.get(var0);
         } catch (NoSuchFieldException var4) {
         }
      }

      return null;
   }

   private static int fieldInt(Object var0, String var1, int var2) {
      try {
         Object var3 = getField(var0, var1);
         return var3 instanceof Number ? ((Number)var3).intValue() : var2;
      } catch (Throwable var4) {
         return var2;
      }
   }

   private static Object invokeNoArgQuiet(Object var0, String var1) {
      try {
         return invokeNoArg(var0, var1);
      } catch (Throwable var3) {
         return null;
      }
   }

   private static Object invokeNoArg(Object var0, String var1) throws Exception {
      Method var2 = findNoArgMethod(var0.getClass(), var1);
      return var2 == null ? null : var2.invoke(var0);
   }

   private static Method findNoArgMethod(Class<?> var0, String var1) {
      for (Class var2 = var0; var2 != null; var2 = var2.getSuperclass()) {
         try {
            Method var3 = var2.getDeclaredMethod(var1);
            var3.setAccessible(true);
            return var3;
         } catch (NoSuchMethodException var5) {
         }
      }

      try {
         Method var6 = var0.getMethod(var1);
         var6.setAccessible(true);
         return var6;
      } catch (Throwable var4) {
         return null;
      }
   }

   private static Method findMethod(Class<?> var0, String var1, Class<?>... var2) {
      for (Class var3 = var0; var3 != null; var3 = var3.getSuperclass()) {
         try {
            Method var4 = var3.getDeclaredMethod(var1, var2);
            var4.setAccessible(true);
            return var4;
         } catch (NoSuchMethodException var6) {
         }
      }

      try {
         Method var7 = var0.getMethod(var1, var2);
         var7.setAccessible(true);
         return var7;
      } catch (Throwable var5) {
         return null;
      }
   }

   private static Integer callInt(Object var0, String var1) {
      int var2 = callIntDefault(var0, var1, Integer.MIN_VALUE);
      return var2 == Integer.MIN_VALUE ? null : var2;
   }

   private static int callIntDefault(Object var0, String var1, int var2) {
      try {
         Method var3 = findNoArgMethod(var0.getClass(), var1);
         if (var3 == null) {
            return var2;
         } else {
            Object var4 = var3.invoke(var0);
            return var4 instanceof Number ? ((Number)var4).intValue() : var2;
         }
      } catch (Throwable var5) {
         return var2;
      }
   }

   private static boolean callBoolean(Object var0, String var1, boolean var2) {
      try {
         Method var3 = findNoArgMethod(var0.getClass(), var1);
         if (var3 == null) {
            return var2;
         } else {
            Object var4 = var3.invoke(var0);
            return var4 instanceof Boolean ? (Boolean)var4 : var2;
         }
      } catch (Throwable var5) {
         return var2;
      }
   }

   private static int clamp(int var0, int var1, int var2) {
      return Math.max(var1, Math.min(var2, var0));
   }

   private static int floorMod(int var0, int var1) {
      int var2 = var0 % var1;
      return var2 < 0 ? var2 + var1 : var2;
   }
}
