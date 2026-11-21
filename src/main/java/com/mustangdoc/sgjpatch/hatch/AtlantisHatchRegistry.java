package com.mustangdoc.sgjpatch.hatch;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Added via automatic mod-bus scanning, preserving all existing 2.0.4 registrations. */
@EventBusSubscriber(modid = "sgjatlantis_dhd", bus = EventBusSubscriber.Bus.MOD)
public final class AtlantisHatchRegistry {
    public static Block TABLE;
    public static Block PANEL;
    public static Block WEATHER_SEAL;
    public static Block REDSTONE_PORT;
    public static BlockEntityType<AtlantisHatchTableBlockEntity> TABLE_ENTITY;
    public static BlockEntityType<AtlantisHatchIrisBlockEntity> IRIS_ENTITY;
    public static MenuType<AtlantisHatchMenu> MENU;
    private static final String NS = "stargate_atlantis";

    private AtlantisHatchRegistry() {}

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            TABLE = new AtlantisHatchTableBlock();
            PANEL = new AtlantisHatchPanelBlock();
            WEATHER_SEAL = new AtlantisHatchWeatherSealBlock();
            REDSTONE_PORT = new AtlantisHatchRedstonePortBlock();
            helper.register(ResourceLocation.fromNamespaceAndPath(NS, "jumper_bay_door_table"), TABLE);
            helper.register(ResourceLocation.fromNamespaceAndPath(NS, "jumper_bay_hatch_panel"), PANEL);
            helper.register(ResourceLocation.fromNamespaceAndPath(NS, "jumper_bay_hatch_weather_seal"), WEATHER_SEAL);
            helper.register(ResourceLocation.fromNamespaceAndPath(NS, "jumper_bay_hatch_redstone_port"), REDSTONE_PORT);
        });
        event.register(Registries.ITEM, helper -> {
            helper.register(ResourceLocation.fromNamespaceAndPath(NS, "jumper_bay_door_table"),
                    new BlockItem(TABLE, new Item.Properties()));
            helper.register(ResourceLocation.fromNamespaceAndPath(NS, "jumper_bay_hatch_panel"),
                    new BlockItem(PANEL, new Item.Properties()));
            helper.register(ResourceLocation.fromNamespaceAndPath(NS, "jumper_bay_hatch_redstone_port"),
                    new BlockItem(REDSTONE_PORT, new Item.Properties()));
        });
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> {
            IRIS_ENTITY = BlockEntityType.Builder.of(AtlantisHatchIrisBlockEntity::new, PANEL).build(null);
            helper.register(ResourceLocation.fromNamespaceAndPath(NS, "jumper_bay_iris"), IRIS_ENTITY);
            TABLE_ENTITY = BlockEntityType.Builder.of(AtlantisHatchTableBlockEntity::new, TABLE).build(null);
            helper.register(ResourceLocation.fromNamespaceAndPath(NS, "jumper_bay_door_table"), TABLE_ENTITY);
        });
        event.register(Registries.MENU, helper -> {
            MENU = IMenuTypeExtension.create(AtlantisHatchMenu::new);
            helper.register(ResourceLocation.fromNamespaceAndPath(NS, "jumper_bay_door_table"), MENU);
        });
    }

    @EventBusSubscriber(modid = "sgjatlantis_dhd", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Client {
        @SubscribeEvent
        public static void onScreens(RegisterMenuScreensEvent event) {
            event.register(MENU, AtlantisHatchScreen::new);
        }
    }
}
