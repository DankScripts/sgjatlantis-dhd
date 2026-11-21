package com.mustangdoc.sgjpatch.init;

import com.mustangdoc.sgjpatch.item.PegasusUpgradedShieldItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class SGJPatchItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "stargate_atlantis");
    private static final DeferredRegister<Item> ATLANTIS_ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "stargate_atlantis");
    public static final RegistryObject<Item> PEGASUS_UPGRADED_SHIELD =
            ITEMS.register("pegasus_upgraded_shield", () -> new PegasusUpgradedShieldItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<BlockItem> ATLANTIS_DHD =
            ATLANTIS_ITEMS.register("atlantis_dhd", () -> new BlockItem(SGJPatchBlocks.ATLANTIS_DHD.get(), new Item.Properties()));

    private SGJPatchItems() {}

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        ATLANTIS_ITEMS.register(eventBus);
    }
}
