package com.mustangdoc.sgjpatch.client.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class AtlantisEditorHelper {
   private static final Map<Object, Integer> CONST_SELECTION = new WeakHashMap<>();

   private AtlantisEditorHelper() {
   }

   public static synchronized void clear(Object var0) {
      CONST_SELECTION.remove(var0);
   }

   public static synchronized boolean hasConstellationSelection(Object var0) {
      return CONST_SELECTION.containsKey(var0);
   }

   public static boolean selectConstellation(Object var0, double var1, double var3, int var5) {
      if (var5 == 1 && AtlantisDebugConfig.editorEnabled() && getBoolean(var0, "editMode")) {
         try {
            Field var6 = field(var0.getClass(), "dialerButtons");
            if (var6.get(var0) instanceof List var8) {
               Object var9 = null;
               double var10 = Double.MAX_VALUE;
               int var12 = -1;

               for (Object var14 : var8) {
                  int var15 = getInt(var14, "displaySymbol");
                  if (var15 >= 1 && var15 <= 38) {
                     Method var16 = var14.getClass().getDeclaredMethod("m_5953_", double.class, double.class);
                     var16.setAccessible(true);
                     if (Boolean.TRUE.equals(var16.invoke(var14, var1, var3))) {
                        Method var17 = var14.getClass().getMethod("m_252754_");
                        Method var18 = var14.getClass().getMethod("m_252907_");
                        int var19 = ((Number)var17.invoke(var14)).intValue();
                        int var20 = ((Number)var18.invoke(var14)).intValue();
                        int var21 = getInt(var14, "size");
                        double var22 = var1 - ((double)var19 + (double)var21 * 0.5);
                        double var24 = var3 - ((double)var20 + (double)var21 * 0.5);
                        double var26 = var22 * var22 + var24 * var24;
                        if (var26 < var10) {
                           var10 = var26;
                           var9 = var14;
                           var12 = getInt(var14, "index");
                        }
                     }
                  }
               }

               if (var9 == null) {
                  return false;
               } else {
                  synchronized (AtlantisEditorHelper.class) {
                     CONST_SELECTION.put(var0, var12);
                  }

                  setInt(var0, "selectedIndex", -1);
                  return true;
               }
            } else {
               return false;
            }
         } catch (Throwable var30) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean handleConstellationKey(Object var0, int var1, int var2) {
      if (AtlantisDebugConfig.editorEnabled() && getBoolean(var0, "editMode")) {
         int var3;
         synchronized (AtlantisEditorHelper.class) {
            Integer var5 = CONST_SELECTION.get(var0);
            if (var5 == null) {
               return false;
            }

            var3 = var5;
         }

         if (var1 == 82) {
            ConstellationOffsetStore.reset(var3);
            return true;
         } else {
            byte var8 = 1;
            if ((var2 & 1) != 0) {
               var8 = 5;
            } else if ((var2 & 2) != 0) {
               var8 = 10;
            }

            int var9 = 0;
            int var6 = 0;
            switch (var1) {
               case 262:
                  var9 = var8;
                  break;
               case 263:
                  var9 = -var8;
                  break;
               case 264:
                  var6 = var8;
                  break;
               case 265:
                  var6 = -var8;
                  break;
               default:
                  return false;
            }

            ConstellationOffsetStore.move(var3, var9, var6);
            return true;
         }
      } else {
         return false;
      }
   }

   public static int constellationDx(int var0) {
      return ConstellationOffsetStore.dx(var0);
   }

   public static int constellationDy(int var0) {
      return ConstellationOffsetStore.dy(var0);
   }

   public static String selectionStatus(Object var0) {
      Integer var1;
      synchronized (AtlantisEditorHelper.class) {
         var1 = CONST_SELECTION.get(var0);
      }

      if (var1 == null) {
         return null;
      } else {
         try {
            Field var10 = field(var0.getClass(), "dialerButtons");
            Object var3 = var10.get(var0);
            if (var3 instanceof List) {
               for (Object var6 : (List)var3) {
                  if (getInt(var6, "index") == var1) {
                     int var7 = getInt(var6, "displaySymbol");
                     return "Selected: Constellation " + var7 + " | dx=" + ConstellationOffsetStore.dx(var1) + ", dy=" + ConstellationOffsetStore.dy(var1);
                  }
               }
            }
         } catch (Throwable var9) {
         }

         return "Selected: Constellation | dx=" + ConstellationOffsetStore.dx(var1) + ", dy=" + ConstellationOffsetStore.dy(var1);
      }
   }

   private static Field field(Class<?> var0, String var1) throws Exception {
      for (Class var2 = var0; var2 != null; var2 = var2.getSuperclass()) {
         try {
            Field var3 = var2.getDeclaredField(var1);
            var3.setAccessible(true);
            return var3;
         } catch (NoSuchFieldException var4) {
         }
      }

      throw new NoSuchFieldException(var1);
   }

   private static int getInt(Object var0, String var1) throws Exception {
      return field(var0.getClass(), var1).getInt(var0);
   }

   private static boolean getBoolean(Object var0, String var1) {
      try {
         return field(var0.getClass(), var1).getBoolean(var0);
      } catch (Throwable var3) {
         return false;
      }
   }

   private static void setInt(Object var0, String var1, int var2) throws Exception {
      field(var0.getClass(), var1).setInt(var0, var2);
   }
}
