package com.mustangdoc.sgjpatch.zpm;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/** Forge registration for Mike's three-socket ZPM hub port. */
@Mod.EventBusSubscriber(modid = "sgjadditions_capacity_patch", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TripleZpmHubRegistration {
    public static final ResourceLocation ID = new ResourceLocation("sgjadditions_capacity_patch", "triple_zpm_hub");
    public static TripleZpmHubBlock BLOCK;
    public static BlockEntityType<TripleZpmHubBlockEntity> BLOCK_ENTITY;
    public static MenuType<TripleZpmHubMenu> MENU;
    private TripleZpmHubRegistration() {}

    @SubscribeEvent public static void register(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.BLOCKS, helper -> {
            BLOCK = new TripleZpmHubBlock(BlockBehaviour.Properties.of().strength(4F)
                    .requiresCorrectToolForDrops().noOcclusion().pushReaction(PushReaction.BLOCK));
            helper.register(ID, BLOCK);
        });
        event.register(ForgeRegistries.Keys.ITEMS, helper ->
                helper.register(ID, new BlockItem(BLOCK, new Item.Properties())));
        event.register(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES, helper -> {
            BLOCK_ENTITY = BlockEntityType.Builder.of(TripleZpmHubBlockEntity::new, BLOCK).build(null);
            helper.register(ID, BLOCK_ENTITY);
        });
        event.register(ForgeRegistries.Keys.MENU_TYPES, helper -> {
            MENU = IForgeMenuType.create(TripleZpmHubMenu::new);
            helper.register(ID, MENU);
        });
    }
}
