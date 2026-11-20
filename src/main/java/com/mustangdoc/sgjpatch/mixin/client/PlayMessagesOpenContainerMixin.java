package com.mustangdoc.sgjpatch.mixin.client;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.PlayMessages.OpenContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {OpenContainer.class},
   remap = false
)
public abstract class PlayMessagesOpenContainerMixin {
   private static final Logger LOGGER = LogManager.getLogger("SGJPATCH");
   private static final ResourceLocation ATLANTIS_DHD_MENU = ResourceLocation.fromNamespaceAndPath("sgjadditions", "atlantis_dhd");

   @Inject(
      method = {"decode"},
      at = {@At("RETURN")}
   )
   private static void sgjpatch$decodeReturn(FriendlyByteBuf buf, CallbackInfoReturnable<OpenContainer> cir) {
      OpenContainer message = (OpenContainer)cir.getReturnValue();
      if (message != null) {
         ResourceLocation key = null;

         try {
            MenuType<?> menuType = message.getType();
            key = BuiltInRegistries.MENU.getKey(menuType);
         } catch (Throwable var8) {
         }

         if (key != null) {
            boolean interesting = ATLANTIS_DHD_MENU.equals(key) || "sgjadditions".equals(key.getNamespace()) && key.getPath().toLowerCase().contains("dhd");
            if (interesting) {
               int readableBytes = -1;

               try {
                  readableBytes = message.getAdditionalData().readableBytes();
               } catch (Throwable var7) {
               }

               LOGGER.info(
                  "[SGJPATCH] ForgeOpenContainer(decode): menuType={} windowId={} name={} additionalDataBytes={}",
                  key,
                  message.getWindowId(),
                  message.getName(),
                  readableBytes
               );
            }
         }
      }
   }

   @Inject(
      method = {"handle"},
      at = {@At("HEAD")}
   )
   private static void sgjpatch$handleHead(OpenContainer message, Supplier<Context> contextSupplier, CallbackInfo ci) {
      ResourceLocation key = null;

      try {
         MenuType<?> menuType = message.getType();
         key = BuiltInRegistries.MENU.getKey(menuType);
      } catch (Throwable var11) {
      }

      boolean interesting = false;
      if (key != null) {
         String namespace = key.getNamespace();
         String path = key.getPath().toLowerCase();
         interesting = ATLANTIS_DHD_MENU.equals(key)
            || "sgjadditions".equals(namespace) && path.contains("dhd")
            || "sgjourney".equals(namespace) && path.contains("dhd");
      }

      if (interesting) {
         Minecraft mc = Minecraft.getInstance();
         String menuClass = mc != null && mc.player != null && mc.player.containerMenu != null ? mc.player.containerMenu.getClass().getName() : "<null>";
         String screenClass = mc != null && mc.screen != null ? mc.screen.getClass().getName() : "<null>";
         int readableBytes = -1;

         try {
            readableBytes = message.getAdditionalData().readableBytes();
         } catch (Throwable var10) {
         }

         LOGGER.info(
            "[SGJPATCH] ForgeOpenContainer(HEAD): menuType={} windowId={} name={} additionalDataBytes={} player.containerMenu={} screen={}",
            key == null ? "<null>" : key,
            message.getWindowId(),
            message.getName(),
            readableBytes,
            menuClass,
            screenClass
         );
      }
   }

   @Inject(
      method = {"handle"},
      at = {@At("RETURN")}
   )
   private static void sgjpatch$handleReturn(OpenContainer message, Supplier<Context> contextSupplier, CallbackInfo ci) {
      ResourceLocation key = null;

      try {
         MenuType<?> menuType = message.getType();
         key = BuiltInRegistries.MENU.getKey(menuType);
      } catch (Throwable var8) {
      }

      if (key != null) {
         boolean interesting = ATLANTIS_DHD_MENU.equals(key) || "sgjadditions".equals(key.getNamespace()) && key.getPath().toLowerCase().contains("dhd");
         if (interesting) {
            Minecraft mc = Minecraft.getInstance();
            String menuClass = mc != null && mc.player != null && mc.player.containerMenu != null ? mc.player.containerMenu.getClass().getName() : "<null>";
            String screenClass = mc != null && mc.screen != null ? mc.screen.getClass().getName() : "<null>";
            LOGGER.info(
               "[SGJPATCH] ForgeOpenContainer(RETURN): menuType={} windowId={} player.containerMenu={} screen={}",
               key,
               message.getWindowId(),
               menuClass,
               screenClass
            );
         }
      }
   }
}

