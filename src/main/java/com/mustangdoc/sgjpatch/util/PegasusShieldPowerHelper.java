package com.mustangdoc.sgjpatch.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class PegasusShieldPowerHelper {
   public static final long SHIELD_FE_PER_TICK = 1200L;

   private PegasusShieldPowerHelper() {
   }

   public static boolean canEngage(Object var0) {
      try {
         return !hasOurShield(var0) ? true : availableBufferedPower(var0) >= 1200L;
      } catch (Throwable var2) {
         return false;
      }
   }

   public static void tick(Object var0) {
      if (var0 != null) {
         try {
            Object var1 = irisInfo(var0);
            if (var1 == null || !hasOurShield(var1)) {
               return;
            }

            Object var2 = invokeNoArgs(stargateOr(var1), "isIrisClosed");
            if (!(var2 instanceof Boolean) || !(Boolean)var2) {
               return;
            }

            long var3 = consumeBufferedPower(var0, 1200L);
            if (var3 < 1200L) {
               forceOpen(var0, var1);
            }
         } catch (Throwable var5) {
         }
      }
   }

   public static long getStoredEnergy(Object var0) {
      try {
         Object var1 = energyStorage(var0);
         return var1 == null ? 0L : readLong(var1, "getTrueEnergyStored", "getEnergyStored");
      } catch (Throwable var2) {
         return 0L;
      }
   }

   public static long getEnergyCapacity(Object var0) {
      try {
         Object var1 = energyStorage(var0);
         if (var1 != null) {
            long var2 = readLong(var1, "getTrueMaxEnergyStored", "getMaxEnergyStored");
            if (var2 > 0L) {
               return var2;
            }
         }

         return readLong(var0, "getEnergyCapacity", "getMaxEnergyStored", "getMaxEnergy", "getEnergyBufferCapacity");
      } catch (Throwable var4) {
         return 0L;
      }
   }

   public static long availableBufferedPower(Object var0) {
      long var1 = 0L;

      for (Object var4 : uniquePowerStorages(var0)) {
         try {
            long var5 = deplete(var4, 1200L - Math.min(var1, 1200L), true);
            var1 += Math.max(0L, var5);
            if (var1 >= 1200L) {
               return var1;
            }
         } catch (Throwable var7) {
         }
      }

      return var1;
   }

   private static long consumeBufferedPower(Object var0, long var1) {
      if (var1 <= 0L) {
         return 0L;
      } else {
         List var3 = uniquePowerStorages(var0);
         long var4 = 0L;

         for (Object var7 : var3) {
            try {
               var4 += Math.max(0L, deplete(var7, var1 - Math.min(var4, var1), true));
               if (var4 >= var1) {
                  break;
               }
            } catch (Throwable var12) {
            }
         }

         if (var4 < var1) {
            return 0L;
         } else {
            long var13 = 0L;

            for (Object var9 : var3) {
               if (var13 >= var1) {
                  break;
               }

               try {
                  var13 += Math.max(0L, deplete(var9, var1 - var13, false));
               } catch (Throwable var11) {
               }
            }

            return var13;
         }
      }
   }

   private static List<Object> uniquePowerStorages(Object var0) {
      ArrayList var1 = new ArrayList(2);
      IdentityHashMap var2 = new IdentityHashMap();
      tryAddStorage(var1, var2, var0);

      try {
         Object var3 = connectedDHD(var0);
         tryAddStorage(var1, var2, var3);
      } catch (Throwable var4) {
      }

      return var1;
   }

   private static void tryAddStorage(List<Object> var0, Map<Object, Boolean> var1, Object var2) {
      if (var2 != null) {
         try {
            Object var3 = energyStorage(var2);
            if (var3 != null && !var1.containsKey(var3)) {
               var1.put(var3, Boolean.TRUE);
               var0.add(var3);
            }
         } catch (Throwable var4) {
         }
      }
   }

   private static Object connectedDHD(Object var0) throws Exception {
      Object var1 = null;

      try {
         Field var2 = findField(var0.getClass(), "dhdCache");
         if (var2 != null) {
            var2.setAccessible(true);
            var1 = var2.get(var0);
         }
      } catch (Throwable var5) {
      }

      if (var1 == null) {
         try {
            var1 = invokeNoArgs(var0, "controllerCache");
         } catch (Throwable var4) {
         }
      }

      if (var1 == null) {
         return null;
      } else {
         try {
            return invokeNoArgs(var1, "get");
         } catch (Throwable var3) {
            return invokeNoArgs(var1, "getCached");
         }
      }
   }

   private static Object energyStorage(Object var0) throws Exception {
      if (var0 == null) {
         return null;
      } else {
         try {
            return invokeNoArgs(var0, "getEnergyStorage");
         } catch (Throwable var2) {
            Field var1 = findField(var0.getClass(), "energyStorage");
            if (var1 != null) {
               var1.setAccessible(true);
               return var1.get(var0);
            } else {
               return null;
            }
         }
      }
   }

   public static void notifyInsufficient(Object var0) {
      if (var0 != null) {
         try {
            Class var1 = Class.forName("net.minecraft.network.chat.Component");
            Object var2 = null;

            for (Method var6 : var1.getMethods()) {
               if (Modifier.isStatic(var6.getModifiers())
                  && var6.getParameterCount() == 1
                  && var6.getParameterTypes()[0] == String.class
                  && (var6.getName().equals("literal") || var6.getName().equals("m_237113_"))) {
                  var2 = var6.invoke(null, "Insufficient power: Atlantis shield requires 1200 FE/t");
                  break;
               }
            }

            if (var2 == null) {
               return;
            }

            for (Method var11 : var0.getClass().getMethods()) {
               if ((var11.getName().equals("displayClientMessage") || var11.getName().equals("m_5661_"))
                  && var11.getParameterCount() == 2
                  && var11.getParameterTypes()[1] == boolean.class) {
                  var11.invoke(var0, var2, true);
                  return;
               }
            }
         } catch (Throwable var7) {
         }
      }
   }

   private static Object irisInfo(Object var0) throws Exception {
      return invokeNoArgs(var0, "irisInfo");
   }

   private static Object stargateOr(Object var0) {
      return var0;
   }

   private static boolean hasOurShield(Object var0) throws Exception {
      Object var1 = var0;

      try {
         Object var2 = invokeNoArgs(var0, "irisInfo");
         if (var2 != null) {
            var1 = var2;
         }
      } catch (Throwable var13) {
      }

      if (var1 == null) {
         return false;
      } else {
         Object var14 = invokeNoArgs(var1, "getIris");
         if (var14 == null) {
            return false;
         } else {
            Class var3 = Class.forName("com.mustangdoc.sgjpatch.init.SGJPatchItems");
            Field var4 = var3.getField("PEGASUS_UPGRADED_SHIELD");
            Object var5 = var4.get(null);
            Object var6 = invokeNoArgs(var5, "get");
            if (var6 == null) {
               return false;
            } else {
               for (Method var10 : var14.getClass().getMethods()) {
                  if ((var10.getName().equals("is") || var10.getName().equals("m_150930_"))
                     && var10.getParameterCount() == 1
                     && (var10.getReturnType() == boolean.class || var10.getReturnType() == Boolean.class)) {
                     try {
                        Object var11 = var10.invoke(var14, var6);
                        if (var11 instanceof Boolean) {
                           return (Boolean)var11;
                        }
                     } catch (IllegalArgumentException var12) {
                     }
                  }
               }

               return false;
            }
         }
      }
   }

   private static long deplete(Object var0, long var1, boolean var3) throws Exception {
      if (var0 != null && var1 > 0L) {
         Method var4 = findMethod(var0.getClass(), "depleteEnergy", long.class, boolean.class);
         if (var4 == null) {
            return 0L;
         } else {
            var4.setAccessible(true);
            Object var5 = var4.invoke(var0, var1, var3);
            return var5 instanceof Number ? ((Number)var5).longValue() : 0L;
         }
      } else {
         return 0L;
      }
   }

   private static long readLong(Object var0, String... var1) {
      if (var0 == null) {
         return 0L;
      } else {
         for (String var5 : var1) {
            try {
               Method var6 = findMethod(var0.getClass(), var5);
               if (var6 != null) {
                  var6.setAccessible(true);
                  Object var7 = var6.invoke(var0);
                  if (var7 instanceof Number) {
                     return ((Number)var7).longValue();
                  }
               }
            } catch (Throwable var8) {
            }
         }

         return 0L;
      }
   }

   private static void forceOpen(Object var0, Object var1) {
      try {
         Class var2 = Class.forName("net.povstalec.sgjourney.common.blockstates.ShieldingState");
         Object var3 = var2.getField("OPEN").get(null);
         Method var4 = var2.getMethod("getProgress");
         short var5 = ((Number)var4.invoke(var3)).shortValue();
         Method var6 = var1.getClass().getMethod("setIrisProgress", short.class);
         var6.invoke(var1, var5);
         Method var7 = var0.getClass().getMethod("setStargateState", boolean.class, var2);
         var7.invoke(var0, true, var3);
         playOpeningSound(var0);

         try {
            invokeNoArgs(var0, "updateClient");
         } catch (Throwable var9) {
         }
      } catch (Throwable var10) {
      }
   }

   private static void playOpeningSound(Object var0) {
      try {
         Object var1 = invokeNoArgsEither(var0, "getLevel", "m_58904_");
         Object var2 = invokeNoArgsEither(var0, "getBlockPos", "m_58899_");
         if (var1 == null || var2 == null) {
            return;
         }

         Class var3 = Class.forName("com.mustangdoc.sgjpatch.init.SGJPatchSounds");
         Object var4 = var3.getField("PEGASUS_SHIELD_OPENING").get(null);
         Object var5 = invokeNoArgs(var4, "get");
         Class var6 = Class.forName("net.minecraft.sounds.SoundSource");
         Object var7 = var6.getField("BLOCKS").get(null);

         for (Method var11 : var1.getClass().getMethods()) {
            if ((var11.getName().equals("playSound") || var11.getName().equals("m_5594_")) && var11.getParameterCount() == 6) {
               try {
                  var11.invoke(var1, null, var2, var5, var7, 16.0F, 1.0F);
                  return;
               } catch (IllegalArgumentException var13) {
               }
            }
         }
      } catch (Throwable var14) {
      }
   }

   private static Field findField(Class<?> var0, String var1) {
      for (Class var2 = var0; var2 != null; var2 = var2.getSuperclass()) {
         try {
            return var2.getDeclaredField(var1);
         } catch (NoSuchFieldException var4) {
         }
      }

      return null;
   }

   private static Method findMethod(Class<?> var0, String var1, Class<?>... var2) {
      for (Class var3 = var0; var3 != null; var3 = var3.getSuperclass()) {
         try {
            return var3.getDeclaredMethod(var1, var2);
         } catch (NoSuchMethodException var6) {
         }
      }

      try {
         return var0.getMethod(var1, var2);
      } catch (Throwable var5) {
         return null;
      }
   }

   private static Object invokeNoArgs(Object var0, String var1) throws Exception {
      Method var2 = findMethod(var0.getClass(), var1);
      if (var2 == null) {
         throw new NoSuchMethodException(var1);
      } else {
         var2.setAccessible(true);
         return var2.invoke(var0);
      }
   }

   private static Object invokeNoArgsEither(Object var0, String var1, String var2) throws Exception {
      try {
         return invokeNoArgs(var0, var1);
      } catch (NoSuchMethodException var4) {
         return invokeNoArgs(var0, var2);
      }
   }
}
