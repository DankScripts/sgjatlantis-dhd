package com.mustangdoc.sgjpatch.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Items;
import net.povstalec.sgjourney.common.items.StargateIrisItem;

public final class SGJPatchItems {
   public static final Items ITEMS = DeferredRegister.createItems("sgjatlantis_dhd");
   public static final DeferredItem<BlockItem> ATLANTIS_DHD = ITEMS.register(
      "atlantis_dhd", () -> new BlockItem((Block)SGJPatchBlocks.ATLANTIS_DHD.get(), new Properties())
   );
   public static final DeferredItem<StargateIrisItem> PEGASUS_UPGRADED_SHIELD = ITEMS.register(
      "pegasus_upgraded_shield",
      () -> new StargateIrisItem(
            new Properties().stacksTo(1),
            ResourceLocation.fromNamespaceAndPath("sgjatlantis_dhd", "textures/entity/stargate/shield/pegasus_upgraded_shield.jpg"),
            () -> 1500
         )
   );

   private SGJPatchItems() {
   }

   public static void register(IEventBus eventBus) {
      ITEMS.register(eventBus);
   }
}
