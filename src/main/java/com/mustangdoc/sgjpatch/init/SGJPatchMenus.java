package com.mustangdoc.sgjpatch.init;

import com.mustangdoc.sgjpatch.menu.AtlantisDHDCrystalMenu;
import com.mustangdoc.sgjpatch.standalone.AtlantisDHDMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registers the standalone Atlantis DHD menus. */
public final class SGJPatchMenus {
    private static final String MOD_ID = "sgjadditions_capacity_patch";
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);

    public static final RegistryObject<MenuType<AtlantisDHDCrystalMenu>> ATLANTIS_DHD_CRYSTAL =
            MENU_TYPES.register("atlantis_dhd_crystal",
                    () -> IForgeMenuType.create(AtlantisDHDCrystalMenu::fromNetwork));

    public static final RegistryObject<MenuType<AbstractContainerMenu>> ATLANTIS_DHD_COMPAT =
            MENU_TYPES.register("atlantis_dhd_compat",
                    () -> IForgeMenuType.create(SGJPatchMenus::createDialerMenu));

    private SGJPatchMenus() {}

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }

    private static AbstractContainerMenu createDialerMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        return new AtlantisDHDMenu(id, inventory, data);
    }
}
