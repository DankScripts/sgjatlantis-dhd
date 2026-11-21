package com.mustangdoc.sgjpatch.client.render;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class NativePegasusTextureHelper {
   private static final String[] PEGASUS = new String[]{
      null,
      "bydo",
      "robandus",
      "sibbron",
      "once_el",
      "laylox",
      "earth",
      "tahnan",
      "setas",
      "amiwill",
      "acjesis",
      "ca_po",
      "arami",
      "aaxel",
      "illume",
      "ramnon",
      "avoniv",
      "lenchan",
      "alura",
      "ecrumig",
      "symbol20_fix",
      "zamilloz",
      "danami",
      "salma",
      "hacemill",
      "hamlinto",
      "olavii",
      "poco_re",
      "abrin",
      "roehi",
      "sandovi",
      "gilltin",
      "dawnre",
      "elenami",
      "recktic",
      "aldeni",
      "zeo",
      "unknown_1",
      "unknown_2"
   };
   private static volatile Method shaderColor;
   private static volatile boolean searchedColor;

   private NativePegasusTextureHelper() {
   }

   public static String texturePath(int var0, boolean var1) {
      return var0 > 0 && var0 < PEGASUS.length ? "textures/gui/pegasus_clean/" + (var1 ? "outer/" : "center/") + PEGASUS[var0] + ".png" : null;
   }

   public static void applyTint() {
      setColor(0.03F, 0.08F, 0.1F, 1.0F);
   }

   public static void resetTint() {
      setColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private static void setColor(float var0, float var1, float var2, float var3) {
      try {
         Method var4 = findShaderColor();
         if (var4 != null) {
            var4.invoke(null, var0, var1, var2, var3);
         }
      } catch (Throwable var5) {
      }
   }

   private static Method findShaderColor() {
      if (searchedColor) {
         return shaderColor;
      } else {
         synchronized (NativePegasusTextureHelper.class) {
            if (searchedColor) {
               return shaderColor;
            } else {
               try {
                  Class var1 = Class.forName("com.mojang.blaze3d.systems.RenderSystem");
                  Method var2 = null;

                  for (Method var6 : var1.getDeclaredMethods()) {
                     if (Modifier.isStatic(var6.getModifiers())) {
                        Class[] var7 = var6.getParameterTypes();
                        if (var7.length == 4 && var7[0] == float.class && var7[1] == float.class && var7[2] == float.class && var7[3] == float.class) {
                           String var8 = var6.getName().toLowerCase();
                           if (var6.getName().equals("setShaderColor")) {
                              var6.setAccessible(true);
                              shaderColor = var6;
                              break;
                           }

                           if (var2 == null && var8.contains("color")) {
                              var2 = var6;
                           }
                        }
                     }
                  }

                  if (shaderColor == null && var2 != null) {
                     var2.setAccessible(true);
                     shaderColor = var2;
                  }
               } catch (Throwable var10) {
               }

               searchedColor = true;
               return shaderColor;
            }
         }
      }
   }
}
