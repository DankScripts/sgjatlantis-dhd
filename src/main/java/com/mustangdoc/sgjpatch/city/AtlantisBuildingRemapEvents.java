package com.mustangdoc.sgjpatch.city;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.MissingMappingsEvent;

/** Resolves retired add-on names without assigning their old numeric IDs to existing blocks. */
@Mod.EventBusSubscriber(modid = AtlantisBuildingBlocks.NS, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AtlantisBuildingRemapEvents {
    private AtlantisBuildingRemapEvents() {}

    @SubscribeEvent public static void remap(MissingMappingsEvent event) {
        @SuppressWarnings("unchecked")
        ForgeRegistry<Block> blocks = (ForgeRegistry<Block>) ForgeRegistries.BLOCKS;
        @SuppressWarnings("unchecked")
        ForgeRegistry<Item> items = (ForgeRegistry<Item>) ForgeRegistries.ITEMS;
        for (MissingMappingsEvent.Mapping<Block> mapping :
                event.getMappings(ForgeRegistries.Keys.BLOCKS, "sgjadditions")) {
            Block target = AtlantisBuildingBlocks.get(mapping.getKey().getPath());
            if (target == null) continue;
            blocks.addAlias(mapping.getKey(), ForgeRegistries.BLOCKS.getKey(target));
            mapping.ignore();
        }
        for (MissingMappingsEvent.Mapping<Item> mapping :
                event.getMappings(ForgeRegistries.Keys.ITEMS, "sgjadditions")) {
            String path = mapping.getKey().getPath();
            if (AtlantisBuildingBlocks.get(path) == null) continue;
            Item target = ForgeRegistries.ITEMS.getValue(new ResourceLocation(AtlantisBuildingBlocks.NS, path));
            if (target == null) continue;
            items.addAlias(mapping.getKey(), ForgeRegistries.ITEMS.getKey(target));
            mapping.ignore();
        }
    }
}
