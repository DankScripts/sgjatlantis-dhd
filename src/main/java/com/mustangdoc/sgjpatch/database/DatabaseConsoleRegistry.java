package com.mustangdoc.sgjpatch.database;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

@Mod.EventBusSubscriber(modid = DatabaseConsoleRegistry.NS, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DatabaseConsoleRegistry {
    static final String NS = "sgjadditions_capacity_patch";
    static final ResourceLocation ID = new ResourceLocation(NS, "atlantis_database_console");
    private static final ResourceLocation DECORATIVE_ID = new ResourceLocation(NS, "atlantis_decorative_console");
    private static DecorativeConsoleBlock decorative;
    static DatabaseConsoleBlock BLOCK;
    static Item ITEM;
    static BlockEntityType<DatabaseConsoleEntity> ENTITY;

    private DatabaseConsoleRegistry() {}

    @SubscribeEvent public static void register(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.BLOCKS, registry -> {
            BLOCK = new DatabaseConsoleBlock();
            registry.register(ID, BLOCK);
            decorative = new DecorativeConsoleBlock();
            registry.register(DECORATIVE_ID, decorative);
        });
        event.register(ForgeRegistries.Keys.ITEMS, registry -> {
            ITEM = new DatabaseConsoleItem();
            registry.register(ID, ITEM);
            registry.register(DECORATIVE_ID, new BlockItem(decorative, new Item.Properties()));
        });
        event.register(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES, registry -> {
            ENTITY = BlockEntityType.Builder.of(DatabaseConsoleEntity::new, BLOCK).build(null);
            registry.register(ID, ENTITY);
        });
    }
}
