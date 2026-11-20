package com.mustangdoc.sgjpatch.city;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Block items for the Atlantis building pieces. */
public final class AtlantisUnreleasedItems {
    private AtlantisUnreleasedItems() {}
    public static Item item(String name, Block block) {
        return new BlockItem(block, new Item.Properties());
    }
}
