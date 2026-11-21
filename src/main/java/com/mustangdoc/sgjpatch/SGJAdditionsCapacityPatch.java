package com.mustangdoc.sgjpatch;

import com.mustangdoc.sgjpatch.client.screens.AtlantisDHDCrystalScreen;
import com.mustangdoc.sgjpatch.client.screens.AtlantisDHDScreenFixed;
import com.mustangdoc.sgjpatch.client.sound.PegasusShieldHumController;
import com.mustangdoc.sgjpatch.config.SGJCompatibilityConfig;
import com.mustangdoc.sgjpatch.init.SGJPatchBlockEntities;
import com.mustangdoc.sgjpatch.init.SGJPatchBlocks;
import com.mustangdoc.sgjpatch.init.SGJPatchItems;
import com.mustangdoc.sgjpatch.init.SGJPatchMenus;
import com.mustangdoc.sgjpatch.init.SGJPatchSounds;
import com.mustangdoc.sgjpatch.menu.AtlantisDHDCompatMenu;
import com.mustangdoc.sgjpatch.network.SGJPatchNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.povstalec.sgjourney.common.menu.dhd.AbstractDHDMenu;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("sgjadditions_capacity_patch")
public class SGJAdditionsCapacityPatch {
   public static final String MOD_ID = "sgjadditions_capacity_patch";
   private static final Logger LOGGER = LogManager.getLogger("SGJPATCH");
   private static final ResourceLocation ATLANTIS_DHD_MENU_ID = ResourceLocation.fromNamespaceAndPath("sgjadditions", "atlantis_dhd");
   private static final ResourceLocation SGJ_ADDITIONS_ATLANTIS_DHD_ID = ResourceLocation.fromNamespaceAndPath("sgjadditions", "atlantis_dhd");

   private static AtlantisDHDScreenFixed createAtlantisDhdScreen(AbstractDHDMenu<?> menu, Inventory inventory, Component title) {
      return new AtlantisDHDScreenFixed(menu, inventory, title);
   }

   public SGJAdditionsCapacityPatch() {
      SGJPatchBlocks.register(FMLJavaModLoadingContext.get().getModEventBus());
      SGJPatchBlockEntities.register(FMLJavaModLoadingContext.get().getModEventBus());
      SGJPatchItems.register(FMLJavaModLoadingContext.get().getModEventBus());
      SGJPatchMenus.register(FMLJavaModLoadingContext.get().getModEventBus());
      SGJPatchSounds.register(FMLJavaModLoadingContext.get().getModEventBus());
      FMLJavaModLoadingContext.get().getModEventBus().addListener(SGJAdditionsCapacityPatch::onCommonSetup);
   }

   private static void onCommonSetup(FMLCommonSetupEvent event) {
      event.enqueueWork(SGJPatchNetwork::register);
   }

   @EventBusSubscriber(
      modid = "sgjadditions_capacity_patch",
      bus = Bus.MOD,
      value = {Dist.CLIENT}
   )
   public static final class ClientEvents {
      @SubscribeEvent
      public static void onClientSetup(FMLClientSetupEvent event) {
         if (!SGJCompatibilityConfig.useOriginalSGJAdditionsDHD()) {
            event.enqueueWork(
               () -> {
                  try {
                     MenuScreens.register(SGJPatchMenus.ATLANTIS_DHD_CRYSTAL.get(), AtlantisDHDCrystalScreen::new);
                     SGJAdditionsCapacityPatch.LOGGER.info("[SGJPATCH] ClientSetup: registered Atlantis DHD crystal config screen");
                  } catch (Throwable var6) {
                     SGJAdditionsCapacityPatch.LOGGER
                        .error(
                           "[SGJPATCH] ClientSetup: failed registering Atlantis DHD crystal config screen ({}: {})",
                           var6.getClass().getSimpleName(),
                           var6.getMessage()
                        );
                  }

                  try {
                     MenuType rawMenuType = SGJPatchMenus.ATLANTIS_DHD_COMPAT.get();
                     ScreenConstructor rawFactory = (menu, inv, title) -> SGJAdditionsCapacityPatch.createAtlantisDhdScreen(
                           (AbstractDHDMenu<?>)menu, inv, title
                        );
                     MenuScreens.register(rawMenuType, rawFactory);
                     SGJAdditionsCapacityPatch.LOGGER.info("[SGJPATCH] ClientSetup: registered Atlantis DHD compat dialer screen");
                  } catch (Throwable var5) {
                     SGJAdditionsCapacityPatch.LOGGER
                        .error(
                           "[SGJPATCH] ClientSetup: failed registering Atlantis DHD compat dialer screen ({}: {})",
                           var5.getClass().getSimpleName(),
                           var5.getMessage()
                        );
                  }

                  MenuType<?> menuType = BuiltInRegistries.MENU
                     .getOptional(SGJAdditionsCapacityPatch.ATLANTIS_DHD_MENU_ID).orElse(null);

                  if (menuType == null) {
                     SGJAdditionsCapacityPatch.LOGGER
                        .debug(
                           "[SGJPATCH] Optional legacy Atlantis DHD menu is not installed: {}",
                           SGJAdditionsCapacityPatch.ATLANTIS_DHD_MENU_ID
                        );
                  } else {
                     try {
                        ScreenConstructor rawFactory = (menu, inv, title) -> SGJAdditionsCapacityPatch.createAtlantisDhdScreen(
                              (AbstractDHDMenu<?>)menu, inv, title
                           );
                        MenuScreens.register(menuType, rawFactory);
                        SGJAdditionsCapacityPatch.LOGGER
                           .info("[SGJPATCH] ClientSetup: registered Atlantis DHD screen factory for {}", SGJAdditionsCapacityPatch.ATLANTIS_DHD_MENU_ID);
                     } catch (Throwable var3) {
                        SGJAdditionsCapacityPatch.LOGGER
                           .error(
                              "[SGJPATCH] ClientSetup: MenuScreens.register failed; relying on tick fallback for Atlantis DHD UI ({}: {})",
                              var3.getClass().getSimpleName(),
                              var3.getMessage()
                           );
                     }
                  }
               }
            );
         }
      }
   }

   @EventBusSubscriber(
      modid = "sgjadditions_capacity_patch",
      bus = Bus.FORGE,
      value = {Dist.CLIENT}
   )
   public static final class ClientForgeEvents {
      private static int lastForcedMenuIdentityHash = 0;
      private static int lastObservedMenuIdentityHash = 0;
      private static int lastNoTypeMenuIdentityHash = 0;
      private static int lastAnyMenuIdentityHash = 0;
      private static String lastAnyMenuClassName = null;
      private static String lastAnyScreenClassName = null;
      private static int menuChangeLogBudget = 40;

      @SubscribeEvent
      public static void onClientTick(ClientTickEvent event) {
         if (event.phase == Phase.END) {
            PegasusShieldHumController.tickClient();
            if (!SGJCompatibilityConfig.useOriginalSGJAdditionsDHD()) {
               Minecraft minecraft = Minecraft.getInstance();
               if (minecraft != null && minecraft.player != null) {
                  AbstractContainerMenu menu = minecraft.player.containerMenu;
                  if (menu != null) {
                     if (menuChangeLogBudget > 0) {
                        int identityHash = System.identityHashCode(menu);
                        String menuClassName = menu.getClass().getName();
                        String screenClassName = minecraft.screen == null ? "<null>" : minecraft.screen.getClass().getName();
                        if (identityHash != lastAnyMenuIdentityHash
                           || lastAnyMenuClassName == null
                           || !menuClassName.equals(lastAnyMenuClassName)
                           || lastAnyScreenClassName == null
                           || !screenClassName.equals(lastAnyScreenClassName)) {
                           lastAnyMenuIdentityHash = identityHash;
                           lastAnyMenuClassName = menuClassName;
                           lastAnyScreenClassName = screenClassName;
                           menuChangeLogBudget--;
                           ResourceLocation anyMenuKey = null;

                           try {
                              MenuType<?> anyType = menu.getType();
                              anyMenuKey = BuiltInRegistries.MENU.getKey(anyType);
                           } catch (Throwable var8) {
                           }

                           SGJAdditionsCapacityPatch.LOGGER
                              .info(
                                 "[SGJPATCH] ClientTick: containerMenu/screen changed: menuClass={} menuKey={} screen={} (remainingBudget={})",
                                 menuClassName,
                                 anyMenuKey == null ? "<null>" : anyMenuKey,
                                 screenClassName,
                                 menuChangeLogBudget
                              );
                        }
                     }

                     boolean isAtlantisDhdMenu = menu instanceof AtlantisDHDCompatMenu;

                     MenuType<?> menuType;
                     try {
                        menuType = menu.getType();
                     } catch (UnsupportedOperationException var9) {
                        if (!isAtlantisDhdMenu) {
                           int identityHash = System.identityHashCode(menu);
                           if (identityHash != lastNoTypeMenuIdentityHash) {
                              lastNoTypeMenuIdentityHash = identityHash;
                              SGJAdditionsCapacityPatch.LOGGER
                                 .warn(
                                    "[SGJPATCH] ClientTick: containerMenu has no type (menuClass={}); skipping Atlantis DHD detection",
                                    menu.getClass().getName()
                                 );
                           }

                           return;
                        }

                        menuType = null;
                     } catch (Throwable var10) {
                        if (!isAtlantisDhdMenu) {
                           SGJAdditionsCapacityPatch.LOGGER
                              .warn(
                                 "[SGJPATCH] ClientTick: failed reading containerMenu type ({}: {}); skipping Atlantis DHD detection",
                                 var10.getClass().getSimpleName(),
                                 var10.getMessage()
                              );
                           return;
                        }

                        menuType = null;
                     }

                     ResourceLocation menuKey = menuType == null ? null : BuiltInRegistries.MENU.getKey(menuType);
                     boolean isAtlantisDhdByKey = SGJAdditionsCapacityPatch.ATLANTIS_DHD_MENU_ID.equals(menuKey);
                     if (isAtlantisDhdMenu || isAtlantisDhdByKey) {
                        int identityHash = System.identityHashCode(menu);
                        if (identityHash != lastObservedMenuIdentityHash) {
                           lastObservedMenuIdentityHash = identityHash;
                           SGJAdditionsCapacityPatch.LOGGER
                              .warn(
                                 "[SGJPATCH] ClientTick: detected Atlantis DHD containerMenu (key={} isAtlantisDhdMenu={}) menuClass={} screen={} ",
                                 menuKey == null ? "<null>" : menuKey,
                                 isAtlantisDhdMenu,
                                 menu.getClass().getName(),
                                 minecraft.screen == null ? "<null>" : minecraft.screen.getClass().getName()
                              );
                        }

                        if (!(minecraft.screen instanceof AtlantisDHDScreenFixed)) {
                           if (identityHash != lastForcedMenuIdentityHash) {
                              if (minecraft.screen != null && !(minecraft.screen instanceof AbstractContainerScreen)) {
                                 SGJAdditionsCapacityPatch.LOGGER
                                    .warn(
                                       "[SGJPATCH] ClientTick: AtlantisDHDMenu is open but another non-container screen is active ({}); not forcing AtlantisDHDScreen",
                                       minecraft.screen.getClass().getName()
                                    );
                              } else {
                                 lastForcedMenuIdentityHash = identityHash;
                                 SGJAdditionsCapacityPatch.LOGGER
                                    .warn(
                                       "[SGJPATCH] ClientTick: AtlantisDHDMenu detected with no AtlantisDHDScreen; forcing screen open (menuIdentityHash={})",
                                       identityHash
                                    );
                                 if (!(menu instanceof AbstractDHDMenu)) {
                                    SGJAdditionsCapacityPatch.LOGGER
                                       .error(
                                          "[SGJPATCH] ClientTick: atlantis_dhd menu opened but menu is not an AbstractDHDMenu (menuClass={}); cannot open AtlantisDHDScreen",
                                          menu.getClass().getName()
                                       );
                                 } else {
                                    minecraft.setScreen(
                                       SGJAdditionsCapacityPatch.createAtlantisDhdScreen(
                                          (AbstractDHDMenu<?>)menu, minecraft.player.getInventory(), Component.translatable("screen.sgjourney.dhd")
                                       )
                                    );
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @EventBusSubscriber(
      modid = "sgjadditions_capacity_patch",
      bus = Bus.MOD
   )
   public static final class CommonModEvents {
      @SubscribeEvent(
         priority = EventPriority.LOWEST
      )
      public static void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
         if (event.getTabKey() == CreativeModeTabs.INGREDIENTS || event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(SGJPatchItems.PEGASUS_UPGRADED_SHIELD.get());
            if (!SGJCompatibilityConfig.useOriginalSGJAdditionsDHD()) {
               event.accept(SGJPatchItems.ATLANTIS_DHD.get());
            }
         }

         if (!SGJCompatibilityConfig.useOriginalSGJAdditionsDHD()) {
            Item originalAtlantisDhd = BuiltInRegistries.ITEM.get(SGJAdditionsCapacityPatch.SGJ_ADDITIONS_ATLANTIS_DHD_ID);
            if (originalAtlantisDhd != null) {
               event.getEntries().remove(new ItemStack(originalAtlantisDhd));
            }
         }
      }
   }
}
