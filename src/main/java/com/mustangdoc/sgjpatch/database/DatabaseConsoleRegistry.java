package com.mustangdoc.sgjpatch.database;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = "sgjatlantis_dhd", bus = EventBusSubscriber.Bus.MOD)
public final class DatabaseConsoleRegistry {
    static final String NS = "sgjadditions_capacity_patch";
    static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(NS, "atlantis_database_console");
    private static final ResourceLocation DECORATIVE_ID = ResourceLocation.fromNamespaceAndPath(NS, "atlantis_decorative_console");
    private static DecorativeConsoleBlock decorative;
    static DatabaseConsoleBlock BLOCK;
    static Item ITEM;
    static BlockEntityType<DatabaseConsoleEntity> ENTITY;

    private DatabaseConsoleRegistry() {}

    @SubscribeEvent public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, registry -> {
            BLOCK = new DatabaseConsoleBlock();
            registry.register(ID, BLOCK);
            decorative = new DecorativeConsoleBlock();
            registry.register(DECORATIVE_ID, decorative);
        });
        event.register(Registries.ITEM, registry -> {
            ITEM = new DatabaseConsoleItem();
            registry.register(ID, ITEM);
            registry.register(DECORATIVE_ID, new BlockItem(decorative, new Item.Properties()));
        });
        event.register(Registries.BLOCK_ENTITY_TYPE, registry -> {
            ENTITY = BlockEntityType.Builder.of(DatabaseConsoleEntity::new, BLOCK).build(null);
            registry.register(ID, ENTITY);
        });
    }
}
