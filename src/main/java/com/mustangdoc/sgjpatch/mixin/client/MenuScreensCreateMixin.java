package com.mustangdoc.sgjpatch.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MenuScreens.class})
public abstract class MenuScreensCreateMixin {
   private static final Logger LOGGER = LogManager.getLogger("SGJPATCH");
   private static final ResourceLocation ATLANTIS_DHD_MENU = ResourceLocation.fromNamespaceAndPath("sgjadditions", "atlantis_dhd");

   @Inject(
      method = {"create"},
      at = {@At("HEAD")}
   )
   private static void sgjpatch$logCreateHead(MenuType<?> menuType, Minecraft minecraft, int containerId, Component title, CallbackInfo ci) {
      ResourceLocation key = BuiltInRegistries.MENU.getKey(menuType);
      if (key != null) {
         boolean looksLikeAtlantisDhd = ATLANTIS_DHD_MENU.equals(key)
            || "sgjadditions".equals(key.getNamespace()) && key.getPath().toLowerCase().contains("dhd");
         if (looksLikeAtlantisDhd) {
            LOGGER.info("[SGJPATCH] MenuScreens.create HEAD: menuType={} containerId={} title={}", key, containerId, title);
         }
      }
   }

   @Inject(
      method = {"create"},
      at = {@At("TAIL")}
   )
   private static void sgjpatch$logCreateTail(MenuType<?> menuType, Minecraft minecraft, int containerId, Component title, CallbackInfo ci) {
      ResourceLocation key = BuiltInRegistries.MENU.getKey(menuType);
      if (key != null) {
         boolean looksLikeAtlantisDhd = ATLANTIS_DHD_MENU.equals(key)
            || "sgjadditions".equals(key.getNamespace()) && key.getPath().toLowerCase().contains("dhd");
         if (looksLikeAtlantisDhd) {
            AbstractContainerScreen<?> screen = null;

            try {
               if (minecraft != null && minecraft.screen instanceof AbstractContainerScreen<?> containerScreen) {
                  screen = containerScreen;
               }
            } catch (Throwable var10) {
            }

            LOGGER.info(
               "[SGJPATCH] MenuScreens.create TAIL: menuType={} minecraft.screen={}",
               key,
               screen == null ? "<non-container-or-null>" : screen.getClass().getName()
            );
         }
      }
   }
}

