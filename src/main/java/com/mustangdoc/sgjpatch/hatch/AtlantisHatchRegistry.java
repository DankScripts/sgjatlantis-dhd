package com.mustangdoc.sgjpatch.hatch;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/** Added via automatic mod-bus scanning, preserving all existing 2.0.4 registrations. */
@Mod.EventBusSubscriber(modid = "sgjadditions_capacity_patch", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AtlantisHatchRegistry {
    public static Block TABLE;
    public static Block PANEL;
    public static Block WEATHER_SEAL;
    public static Block REDSTONE_PORT;
    public static BlockEntityType<AtlantisHatchTableBlockEntity> TABLE_ENTITY;
    public static BlockEntityType<AtlantisHatchIrisBlockEntity> IRIS_ENTITY;
    public static MenuType<AtlantisHatchMenu> MENU;
    private static final String NS = "sgjadditions_capacity_patch";

    private AtlantisHatchRegistry() {}

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.BLOCKS, helper -> {
            TABLE = new AtlantisHatchTableBlock();
            PANEL = new AtlantisHatchPanelBlock();
            WEATHER_SEAL = new AtlantisHatchWeatherSealBlock();
            REDSTONE_PORT = new AtlantisHatchRedstonePortBlock();
            helper.register(new ResourceLocation(NS, "jumper_bay_door_table"), TABLE);
            helper.register(new ResourceLocation(NS, "jumper_bay_hatch_panel"), PANEL);
            helper.register(new ResourceLocation(NS, "jumper_bay_hatch_weather_seal"), WEATHER_SEAL);
            helper.register(new ResourceLocation(NS, "jumper_bay_hatch_redstone_port"), REDSTONE_PORT);
        });
        event.register(ForgeRegistries.Keys.ITEMS, helper -> {
            helper.register(new ResourceLocation(NS, "jumper_bay_door_table"),
                    new BlockItem(TABLE, new Item.Properties()));
            helper.register(new ResourceLocation(NS, "jumper_bay_hatch_panel"),
                    new BlockItem(PANEL, new Item.Properties()));
            helper.register(new ResourceLocation(NS, "jumper_bay_hatch_redstone_port"),
                    new BlockItem(REDSTONE_PORT, new Item.Properties()));
        });
        event.register(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES, helper -> {
            IRIS_ENTITY = BlockEntityType.Builder.of(AtlantisHatchIrisBlockEntity::new, PANEL).build(null);
            helper.register(new ResourceLocation(NS, "jumper_bay_iris"), IRIS_ENTITY);
            TABLE_ENTITY = BlockEntityType.Builder.of(AtlantisHatchTableBlockEntity::new, TABLE).build(null);
            helper.register(new ResourceLocation(NS, "jumper_bay_door_table"), TABLE_ENTITY);
        });
        event.register(ForgeRegistries.Keys.MENU_TYPES, helper -> {
            MENU = IForgeMenuType.create(AtlantisHatchMenu::new);
            helper.register(new ResourceLocation(NS, "jumper_bay_door_table"), MENU);
        });
    }

    @Mod.EventBusSubscriber(modid = NS, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Client {
        @SubscribeEvent
        public static void onScreens(FMLClientSetupEvent event) {
            event.enqueueWork(() -> MenuScreens.register(MENU, AtlantisHatchScreen::new));
        }
    }
}
