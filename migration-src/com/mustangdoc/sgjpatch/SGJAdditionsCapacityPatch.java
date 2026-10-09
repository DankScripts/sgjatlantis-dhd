package com.mustangdoc.sgjpatch;

import com.struxnet.sgjadditions.common.init.MenuInit;
import com.mustangdoc.sgjpatch.client.screens.AtlantisDHDScreenFixed;
import com.mustangdoc.sgjpatch.client.screens.AtlantisDHDCrystalScreen;
import com.mustangdoc.sgjpatch.client.sound.PegasusShieldHumController;
import com.mustangdoc.sgjpatch.config.SGJCompatibilityConfig;
import com.mustangdoc.sgjpatch.init.SGJPatchBlocks;
import com.mustangdoc.sgjpatch.init.SGJPatchBlockEntities;
import com.mustangdoc.sgjpatch.init.SGJPatchItems;
import com.mustangdoc.sgjpatch.init.SGJPatchMenus;
import com.mustangdoc.sgjpatch.init.SGJPatchSounds;
import com.mustangdoc.sgjpatch.menu.AtlantisDHDCompatMenu;
import com.mustangdoc.sgjpatch.network.SGJPatchNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.povstalec.sgjourney.common.menu.dhd.AbstractDHDMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(SGJAdditionsCapacityPatch.MOD_ID)
public class SGJAdditionsCapacityPatch {
	public static final String MOD_ID = "sgjadditions_capacity_patch";
	private static final Logger LOGGER = LogManager.getLogger("SGJPATCH");
	private static final ResourceLocation ATLANTIS_DHD_MENU_ID = ResourceLocation.fromNamespaceAndPath("sgjadditions", "atlantis_dhd");
	private static final ResourceLocation SGJ_ADDITIONS_ATLANTIS_DHD_ID = ResourceLocation.fromNamespaceAndPath("sgjadditions", "atlantis_dhd");

	private static AtlantisDHDScreenFixed createAtlantisDhdScreen(
			AbstractDHDMenu<?> menu, Inventory inventory, Component title) {
		try {
			return AtlantisDHDScreenFixed.class
					.getConstructor(AbstractDHDMenu.class, Inventory.class, Component.class)
					.newInstance(menu, inventory, title);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("Failed to construct the migrated Atlantis DHD screen", exception);
		}
	}

	public SGJAdditionsCapacityPatch() {
		SGJPatchBlocks.register(FMLJavaModLoadingContext.get().getModEventBus());
		SGJPatchBlockEntities.register(FMLJavaModLoadingContext.get().getModEventBus());
		SGJPatchItems.register(FMLJavaModLoadingContext.get().getModEventBus());
		SGJPatchMenus.register(FMLJavaModLoadingContext.get().getModEventBus());
		SGJPatchSounds.register(FMLJavaModLoadingContext.get().getModEventBus());
		FMLJavaModLoadingContext.get().getModEventBus().addListener(SGJAdditionsCapacityPatch::onCommonSetup);
	}

	private static void onCommonSetup(final FMLCommonSetupEvent event) {
		event.enqueueWork(SGJPatchNetwork::register);
	}

	@EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static final class ClientEvents {
		@SubscribeEvent
		public static void onClientSetup(final FMLClientSetupEvent event) {
			if (SGJCompatibilityConfig.useOriginalSGJAdditionsDHD()) {
				return;
			}
			event.enqueueWork(() -> {
				try {
					MenuScreens.register(SGJPatchMenus.ATLANTIS_DHD_CRYSTAL.get(), AtlantisDHDCrystalScreen::new);
					LOGGER.info("[SGJPATCH] ClientSetup: registered Atlantis DHD crystal config screen");
				} catch (Throwable t) {
					LOGGER.error(
							"[SGJPATCH] ClientSetup: failed registering Atlantis DHD crystal config screen ({}: {})",
						t.getClass().getSimpleName(),
						t.getMessage());
				}

			try {
				@SuppressWarnings({"rawtypes", "unchecked"})
				MenuType rawMenuType = (MenuType) SGJPatchMenus.ATLANTIS_DHD_COMPAT.get();
				@SuppressWarnings({"rawtypes", "unchecked"})
				MenuScreens.ScreenConstructor rawFactory = (menu, inv, title) ->
						createAtlantisDhdScreen((AbstractDHDMenu<?>) menu, inv, title);
				MenuScreens.register(rawMenuType, rawFactory);
				LOGGER.info("[SGJPATCH] ClientSetup: registered Atlantis DHD compat dialer screen");
			} catch (Throwable t) {
				LOGGER.error(
						"[SGJPATCH] ClientSetup: failed registering Atlantis DHD compat dialer screen ({}: {})",
						t.getClass().getSimpleName(),
						t.getMessage());
			}

				MenuType<?> menuType = null;
				try {
					menuType = MenuInit.Atlantis_DHD.get();
				} catch (Throwable t) {
						LOGGER.warn("[SGJPATCH] ClientSetup: could not resolve MenuInit.Atlantis_DHD ({}: {})", t.getClass().getSimpleName(), t.getMessage());
				}

				if (menuType == null) {
					menuType = BuiltInRegistries.MENU.getOptional(ATLANTIS_DHD_MENU_ID).orElse(null);
				}

				if (menuType == null) {
						LOGGER.error("[SGJPATCH] ClientSetup: Atlantis DHD MenuType not found in registry for id={} (screen may still open via tick fallback)", ATLANTIS_DHD_MENU_ID);
					return;
				}

				// Prefer registering the proper screen factory so Forge/vanilla can open the UI normally.
				// If this environment has a linkage mismatch (previously observed NoSuchMethodError), fall back to tick-based forcing.
				try {
						// AtlantisDHDScreen is typed against AbstractDHDMenu (the SGJourney base class), while the
						// MenuType is for AtlantisDHDMenu. Java generics are invariant here, so we intentionally
						// register with raw types and cast inside the factory.
						@SuppressWarnings({"rawtypes", "unchecked"})
						MenuType rawMenuType = (MenuType) menuType;
						@SuppressWarnings({"rawtypes", "unchecked"})
						MenuScreens.ScreenConstructor rawFactory = (menu, inv, title) ->
								createAtlantisDhdScreen((AbstractDHDMenu<?>) menu, inv, title);
						MenuScreens.register(rawMenuType, rawFactory);
					LOGGER.info(
							"[SGJPATCH] ClientSetup: registered Atlantis DHD screen factory for {}",
							ATLANTIS_DHD_MENU_ID);
				} catch (Throwable t) {
					LOGGER.error(
							"[SGJPATCH] ClientSetup: MenuScreens.register failed; relying on tick fallback for Atlantis DHD UI ({}: {})",
							t.getClass().getSimpleName(),
							t.getMessage());
				}
			});
		}
	}

	@EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD)
	public static final class CommonModEvents {
		@SubscribeEvent(priority = EventPriority.LOWEST)
		public static void onBuildCreativeTabContents(final BuildCreativeModeTabContentsEvent event) {
			if (event.getTabKey() == CreativeModeTabs.INGREDIENTS || event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
				event.accept(SGJPatchItems.PEGASUS_UPGRADED_SHIELD.get());
				if (!SGJCompatibilityConfig.useOriginalSGJAdditionsDHD()) {
					event.accept(SGJPatchItems.ATLANTIS_DHD.get());
				}
			}

			if (!SGJCompatibilityConfig.useOriginalSGJAdditionsDHD()) {
				Item originalAtlantisDhd = BuiltInRegistries.ITEM.get(SGJ_ADDITIONS_ATLANTIS_DHD_ID);
				if (originalAtlantisDhd != null) {
					event.getEntries().remove(new net.minecraft.world.item.ItemStack(originalAtlantisDhd));
				}
			}
		}
	}

	@EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
	public static final class ClientForgeEvents {
		private static int lastForcedMenuIdentityHash = 0;
		private static int lastObservedMenuIdentityHash = 0;
		private static int lastNoTypeMenuIdentityHash = 0;
		private static int lastAnyMenuIdentityHash = 0;
		private static String lastAnyMenuClassName = null;
		private static String lastAnyScreenClassName = null;
		private static int menuChangeLogBudget = 40;

		@SubscribeEvent
		public static void onClientTick(final TickEvent.ClientTickEvent event) {
			if (event.phase != TickEvent.Phase.END) {
				return;
			}

			PegasusShieldHumController.tickClient();
			if (SGJCompatibilityConfig.useOriginalSGJAdditionsDHD()) {
				return;
			}

			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft == null || minecraft.player == null) {
				return;
			}

			AbstractContainerMenu menu = minecraft.player.containerMenu;
			if (menu == null) {
				return;
			}

			// Global diagnostics: if the client never switches away from InventoryMenu when you right-click the DHD,
			// it strongly suggests the client never handled an OpenScreen packet (or the server never sent one).
			if (menuChangeLogBudget > 0) {
				int identityHash = System.identityHashCode(menu);
				String menuClassName = menu.getClass().getName();
				String screenClassName = (minecraft.screen == null ? "<null>" : minecraft.screen.getClass().getName());
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
					} catch (Throwable ignored) {
					}

					LOGGER.info(
							"[SGJPATCH] ClientTick: containerMenu/screen changed: menuClass={} menuKey={} screen={} (remainingBudget={})",
							menuClassName,
							(anyMenuKey == null ? "<null>" : anyMenuKey),
							screenClassName,
							menuChangeLogBudget);
				}
			}

			// Prefer identifying by concrete menu class first. Some environments/mods may cause MenuType lookups
			// (or registry keys) to be unavailable or different than expected.
			boolean isAtlantisDhdMenu = menu instanceof AtlantisDHDCompatMenu;

			MenuType<?> menuType;
			try {
				menuType = menu.getType();
			} catch (UnsupportedOperationException ex) {
				// Some container menus are constructed without a MenuType and will throw here.
				// This can happen during login/world transitions; never crash the client because of our fallback.
				if (!isAtlantisDhdMenu) {
				int identityHash = System.identityHashCode(menu);
				if (identityHash != lastNoTypeMenuIdentityHash) {
					lastNoTypeMenuIdentityHash = identityHash;
					LOGGER.warn(
							"[SGJPATCH] ClientTick: containerMenu has no type (menuClass={}); skipping Atlantis DHD detection",
							menu.getClass().getName());
				}
					return;
				}
				menuType = null;
			} catch (Throwable t) {
				if (!isAtlantisDhdMenu) {
				LOGGER.warn(
						"[SGJPATCH] ClientTick: failed reading containerMenu type ({}: {}); skipping Atlantis DHD detection",
						t.getClass().getSimpleName(),
						t.getMessage());
					return;
				}
				menuType = null;
			}

			ResourceLocation menuKey = (menuType == null ? null : BuiltInRegistries.MENU.getKey(menuType));
			boolean isAtlantisDhdByKey = ATLANTIS_DHD_MENU_ID.equals(menuKey);
			if (!isAtlantisDhdMenu && !isAtlantisDhdByKey) {
				return;
			}

			int identityHash = System.identityHashCode(menu);
			if (identityHash != lastObservedMenuIdentityHash) {
				lastObservedMenuIdentityHash = identityHash;
				LOGGER.warn(
						"[SGJPATCH] ClientTick: detected Atlantis DHD containerMenu (key={} isAtlantisDhdMenu={}) menuClass={} screen={} ",
						(menuKey == null ? "<null>" : menuKey),
						isAtlantisDhdMenu,
						menu.getClass().getName(),
						(minecraft.screen == null ? "<null>" : minecraft.screen.getClass().getName()));
			}

			if (minecraft.screen instanceof AtlantisDHDScreenFixed) {
				return;
			}

			// If the server opened the menu but no screen appeared (missing/blocked MenuScreens.create),
			// force-open the screen once per menu instance.
			if (identityHash == lastForcedMenuIdentityHash) {
				return;
			}

			if (minecraft.screen != null && !(minecraft.screen instanceof AbstractContainerScreen<?>)) {
				LOGGER.warn(
						"[SGJPATCH] ClientTick: AtlantisDHDMenu is open but another non-container screen is active ({}); not forcing AtlantisDHDScreen",
						minecraft.screen.getClass().getName());
				return;
			}

			lastForcedMenuIdentityHash = identityHash;
			LOGGER.warn(
					"[SGJPATCH] ClientTick: AtlantisDHDMenu detected with no AtlantisDHDScreen; forcing screen open (menuIdentityHash={})",
					identityHash);
			if (!(menu instanceof AbstractDHDMenu)) {
				LOGGER.error(
						"[SGJPATCH] ClientTick: atlantis_dhd menu opened but menu is not an AbstractDHDMenu (menuClass={}); cannot open AtlantisDHDScreen",
						menu.getClass().getName());
				return;
			}
			minecraft.setScreen(createAtlantisDhdScreen(
					(AbstractDHDMenu<?>) menu,
					minecraft.player.getInventory(),
					Component.translatable("screen.sgjourney.dhd")));
		}
	}
}
