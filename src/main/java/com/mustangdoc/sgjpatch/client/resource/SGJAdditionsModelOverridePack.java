package com.mustangdoc.sgjpatch.client.resource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
   modid = "sgjadditions_capacity_patch",
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public final class SGJAdditionsModelOverridePack {
   private static final String MOD_ID = "sgjadditions_capacity_patch";
   private static final String PACK_ROOT = "resourcepacks/sgjatlantis_sgjadditions_override";
   private static final String PACK_ID = "builtin/sgjatlantis_sgjadditions_override";

   private SGJAdditionsModelOverridePack() {
   }

   @SubscribeEvent
   public static void addPackFinders(AddPackFindersEvent var0) {
      PackType var1 = var0.getPackType();
      if (var1 instanceof Enum && var1.ordinal() == 0) {
         try {
            Path var2 = findNestedPackRoot();
            if (var2 == null || !Files.exists(var2.resolve("pack.mcmeta"))) {
               System.err.println("[SGJPATCH] Atlantis model override pack root was not found: " + var2);
               return;
            }

            ClassLoader var3 = SGJAdditionsModelOverridePack.class.getClassLoader();
            Class var4 = Class.forName("net.minecraft.server.packs.repository.Pack", false, var3);
            Class var5 = Class.forName("net.minecraft.server.packs.repository.Pack$ResourcesSupplier", false, var3);
            Class var6 = Class.forName("net.minecraft.server.packs.PackResources", false, var3);
            Class var7 = Class.forName("net.minecraft.server.packs.PathPackResources", false, var3);
            Class var8 = Class.forName("net.minecraft.network.chat.Component", false, var3);
            Class var9 = Class.forName("net.minecraft.server.packs.repository.Pack$Position", false, var3);
            Class var10 = Class.forName("net.minecraft.server.packs.repository.PackSource", false, var3);
            Class var11 = Class.forName("net.minecraft.server.packs.repository.RepositorySource", false, var3);
            Constructor var12 = var7.getConstructor(String.class, Path.class, boolean.class);
            Object var13 = Proxy.newProxyInstance(var3, new Class[]{var5}, (var3x, var4x, var5x) -> {
               if (var4x.getDeclaringClass() == Object.class) {
                  return objectMethod(var3x, var4x, var5x);
               } else if (!var6.isAssignableFrom(var4x.getReturnType())) {
                  return null;
               } else {
                  String var6x = var5x != null && var5x.length > 0 && var5x[0] != null ? String.valueOf(var5x[0]) : "builtin/sgjatlantis_sgjadditions_override";
                  return var12.newInstance(var6x, var2, true);
               }
            });
            Object var14 = createEmptyComponent(var8);
            Object var15 = firstEnumConstant(var9);
            Object var16 = Proxy.newProxyInstance(var3, new Class[]{var10}, (var1x, var2x, var3x) -> {
               if (var2x.getDeclaringClass() == Object.class) {
                  return objectMethod(var1x, var2x, var3x);
               } else if (var2x.getReturnType() == boolean.class) {
                  return Boolean.TRUE;
               } else {
                  return var8.isAssignableFrom(var2x.getReturnType()) && var3x != null && var3x.length > 0 ? var3x[0] : null;
               }
            });
            Method var17 = findReadMetaAndCreate(var4, var5, var9, var10);
            Object var18 = var17.invoke(null, "builtin/sgjatlantis_sgjadditions_override", var14, Boolean.TRUE, var13, var1, var15, var16);
            if (var18 == null) {
               System.err.println("[SGJPATCH] Failed to create Atlantis SGJ Additions model override pack.");
               return;
            }

            Object var19 = Proxy.newProxyInstance(var3, new Class[]{var11}, (var1x, var2x, var3x) -> {
               if (var2x.getDeclaringClass() == Object.class) {
                  return objectMethod(var1x, var2x, var3x);
               } else {
                  if (var3x != null && var3x.length == 1 && var3x[0] instanceof Consumer var4x) {
                     var4x.accept(var18);
                  }

                  return null;
               }
            });
            var0.addRepositorySource((RepositorySource)var19);
            System.out.println("[SGJPATCH] Registered high-priority SGJ Additions Atlantis DHD model override pack.");
         } catch (Throwable var20) {
            System.err.println("[SGJPATCH] Failed registering Atlantis model override pack: " + var20.getClass().getSimpleName() + ": " + var20.getMessage());
            var20.printStackTrace();
         }
      }
   }

   private static Path findNestedPackRoot() throws Exception {
      Class var0 = Class.forName("net.minecraftforge.fml.ModList");
      Object var1 = var0.getMethod("get").invoke(null);
      Object var2 = var0.getMethod("getModFileById", String.class).invoke(var1, "sgjadditions_capacity_patch");
      if (var2 == null) {
         return null;
      } else {
         Object var3 = var2.getClass().getMethod("getFile").invoke(var2);
         Method var4 = var3.getClass().getMethod("findResource", String[].class);
         return (Path)var4.invoke(var3, new String[]{"resourcepacks/sgjatlantis_sgjadditions_override"});
      }
   }

   private static Method findReadMetaAndCreate(Class<?> var0, Class<?> var1, Class<?> var2, Class<?> var3) {
      for (Method var7 : var0.getDeclaredMethods()) {
         if (Modifier.isStatic(var7.getModifiers()) && var7.getReturnType() == var0) {
            Class[] var8 = var7.getParameterTypes();
            if (var8.length == 7 && var8[0] == String.class && var8[2] == boolean.class && var8[3] == var1 && var8[5] == var2 && var8[6] == var3) {
               var7.setAccessible(true);
               return var7;
            }
         }
      }

      throw new IllegalStateException("Could not locate Pack.readMetaAndCreate runtime method");
   }

   private static Object createEmptyComponent(Class<?> var0) throws Exception {
      for (Method var4 : var0.getDeclaredMethods()) {
         if (Modifier.isStatic(var4.getModifiers()) && var4.getParameterCount() == 0 && var0.isAssignableFrom(var4.getReturnType())) {
            var4.setAccessible(true);
            return var4.invoke(null);
         }
      }

      throw new IllegalStateException("Could not create empty Component");
   }

   private static Object firstEnumConstant(Class<?> var0) {
      Object[] var1 = var0.getEnumConstants();
      if (var1 != null && var1.length != 0) {
         return var1[0];
      } else {
         throw new IllegalStateException("Expected enum constants for " + var0.getName());
      }
   }

   private static Object objectMethod(Object var0, Method var1, Object[] var2) {
      String var3 = var1.getName();
      switch (var3) {
         case "toString":
            return "SGJ Atlantis model override proxy";
         case "hashCode":
            return System.identityHashCode(var0);
         case "equals":
            return var2 != null && var2.length == 1 && var0 == var2[0];
         default:
            return null;
      }
   }
}
